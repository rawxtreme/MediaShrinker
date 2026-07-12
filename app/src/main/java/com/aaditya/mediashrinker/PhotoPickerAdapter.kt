package com.aaditya.mediashrinker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

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
        // Cache hit = instant. Cache miss = fast small decode (not full 12MP decode).
        val cached = thumbnailCache.get(uri)
        if (cached != null) {
            holder.imageView.setImageBitmap(cached)
        } else {
            holder.imageView.setImageBitmap(null)
            val bitmap = decodeThumbnail(holder.itemView.context, uri, 200)
            if (bitmap != null) {
                thumbnailCache.put(uri, bitmap)
                holder.imageView.setImageBitmap(bitmap)
            }
        }

        val isSelected = selectedUris.contains(uri)

        // Show the blue checkmark badge only if this exact photo is selected
        holder.checkOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

        // If limit is already reached AND this particular photo is NOT selected,
        // dim it out so the user visually understands it can't be tapped right now.
        val limitReached = selectedUris.size >= maxSelection
        holder.dimOverlay.visibility = if (limitReached && !isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val wasLimitReached = selectedUris.size >= maxSelection

            if (isSelected) {
                // Tapping an already-selected photo removes it (deselect)
                selectedUris.remove(uri)
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
            }

            // Tell the Activity how many are selected now (updates the "x / 100" text)
            onSelectionChanged(selectedUris.size)

            val nowLimitReached = selectedUris.size >= maxSelection

            if (wasLimitReached != nowLimitReached) {
                // The 100-limit boundary was just crossed (hit it or freed up from it) —
                // every visible photo's dim state may need to change, so refresh all.
                notifyDataSetChanged()
            } else {
                // Normal case (well under the limit): only this one photo changed.
                // Refreshing just this item instead of the whole grid is what
                // actually fixes the lag.
                notifyItemChanged(position)
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
        } catch (e: Exception) {
            null
        }
    }

    fun getSelectedUris(): List<Uri> = selectedUris

    override fun getItemCount(): Int = allPhotos.size
}
