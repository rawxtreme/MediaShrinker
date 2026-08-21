package com.aaditya.mediashrinker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class PhotoPickerAdapter(
    private val allPhotos: MutableList<Uri>,
    private val maxSelection: Int,
    initiallySelected: List<Uri> = emptyList(),
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<PhotoPickerAdapter.ViewHolder>() {

    // This list holds only the URIs the user has tapped/selected so far.
    // allPhotos never changes; selectedUris is the one that grows/shrinks.
    // It starts pre-filled with whatever was already selected before this
    // screen opened, so reopening the picker does not wipe earlier picks.
    private val selectedUris = mutableListOf<Uri>().apply { addAll(initiallySelected) }

    // Small in-memory cache of already-decoded thumbnails. Without this, every
    // notifyItemChanged/notifyDataSetChanged call would re-decode the same photo
    // from disk again — this repeated full-res decoding was the actual cause of
    // the multi-second lag when tapping photos.
    private val thumbnailCache = LruCache<Uri, Bitmap>(150)

    // Small fixed thread pool for decoding thumbnails off the main thread —
    // this is what actually fixes scroll lag, since decoding (even a small
    // downsized one) still takes a few milliseconds each, and doing that
    // synchronously inside onBindViewHolder was blocking the UI thread during
    // fast scrolling.
    private val decodeExecutor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.pickerItemImage)
        val checkOverlay: TextView = itemView.findViewById(R.id.pickerItemCheck)
        val dimOverlay: View = itemView.findViewById(R.id.pickerItemDim)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = allPhotos[position]

        // Load a small downsized thumbnail instead of the full-resolution photo.
        // Cache hit = instant. Cache miss = decode happens on a background thread
        // so fast scrolling never blocks waiting for disk/decode work.
        holder.imageView.tag = uri
        val cached = thumbnailCache.get(uri)
        if (cached != null) {
            holder.imageView.setImageBitmap(cached)
        } else {
            holder.imageView.setImageBitmap(null)
            decodeExecutor.execute {
                val bitmap = decodeThumbnail(holder.itemView.context, uri, 200)
                if (bitmap != null) {
                    thumbnailCache.put(uri, bitmap)
                    mainHandler.post {
                        // This ViewHolder may have been recycled for a different
                        // photo by the time decoding finishes (fast scroll) — only
                        // apply the result if it's still showing the same uri.
                        if (holder.imageView.tag == uri) {
                            holder.imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }

        val isSelected = selectedUris.contains(uri)

        // Show the selection number instead of just a checkmark
        if (isSelected) {
            holder.checkOverlay.visibility = View.VISIBLE
            val index = selectedUris.indexOf(uri)
            holder.checkOverlay.text = String.format(java.util.Locale.getDefault(), "%d", index + 1)
        } else {
            holder.checkOverlay.visibility = View.GONE
        }

        // If limit is already reached AND this particular photo is NOT selected,
        // dim it out so the user visually understands it can't be tapped right now.
        val limitReached = selectedUris.size >= maxSelection
        holder.dimOverlay.visibility = if (limitReached && !isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnLongClickListener {
            showEnlargedPreview(holder.itemView.context, uri)
            true
        }

        holder.itemView.setOnClickListener {
            val wasLimitReached = selectedUris.size >= maxSelection

            if (isSelected) {
                // Tapping an already-selected photo removes it (deselect)
                selectedUris.remove(uri)
                // Refresh everything because other selected numbers might change
                notifyDataSetChanged()
            } else {
                // Trying to select a NEW photo — check limit BEFORE adding
                if (selectedUris.size >= maxSelection) {
                    Toast.makeText(
                        holder.itemView.context,
                        "Max $maxSelection photos allowed. Remove one first.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                selectedUris.add(uri)
                notifyItemChanged(position)
            }

            // Tell the Activity how many are selected now (updates the "x / 100" text)
            onSelectionChanged(selectedUris.size)

            val nowLimitReached = selectedUris.size >= maxSelection

            if (wasLimitReached != nowLimitReached && !isSelected) {
                // If limit state changed when ADDING (not removing), refresh all to show/hide dims.
                // removing already calls notifyDataSetChanged above.
                notifyDataSetChanged()
            }
        }
    }

    // Decodes the photo at a small target size instead of full resolution.
    // A full 4000x3000 photo can take a noticeable amount of time to decode;
    // decoding at ~200px is close to instant and is more than enough for a grid thumbnail.
    private fun decodeThumbnail(context: Context, uri: Uri, targetSize: Int): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOptions)
            }

            var sampleSize = 1
            while ((boundsOptions.outWidth / sampleSize) > targetSize ||
                (boundsOptions.outHeight / sampleSize) > targetSize
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    // Long-press preview — shows the tapped photo at full size so the user can
    // confirm it's the right one. Each long-press refreshes it to whichever
    // photo was just pressed (it's not tied to one fixed image).
    private fun showEnlargedPreview(context: Context, uri: Uri) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enlarge_photo, null)
        dialogView.findViewById<ImageView>(R.id.enlargedImageView).setImageURI(uri)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.enlargeCloseBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun getSelectedUris(): List<Uri> = selectedUris

    override fun getItemCount(): Int = allPhotos.size
}
