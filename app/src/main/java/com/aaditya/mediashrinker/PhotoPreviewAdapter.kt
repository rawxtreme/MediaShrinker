package com.aaditya.mediashrinker

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class PhotoPreviewAdapter(
    private val photoList: MutableList<Uri>,
    private val onListChanged: () -> Unit,
    private val onMarksChanged: (Int) -> Unit
) : RecyclerView.Adapter<PhotoPreviewAdapter.ViewHolder>() {

    // When true: cross buttons are hidden, tapping a photo marks/unmarks it for
    // deletion instead of removing it immediately. Enlarge button still works
    // in both modes.
    var isIndividualDeleteMode = false
        set(value) {
            field = value
            if (!value) markedForDeletion.clear()
            notifyDataSetChanged()
        }

    private val markedForDeletion = mutableSetOf<Uri>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.previewItemImage)
        val removeBtn: TextView = itemView.findViewById(R.id.previewItemRemoveBtn)
        val enlargeBtn: TextView = itemView.findViewById(R.id.previewItemEnlargeBtn)
        val markOverlay: View = itemView.findViewById(R.id.previewItemMarkOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = photoList[position]
        holder.imageView.setImageURI(uri)

        // Enlarge works the same in both modes — long-press not needed here,
        // it's always a visible dedicated button on this screen.
        holder.enlargeBtn.setOnClickListener {
            showEnlargedPreview(holder.itemView.context, uri)
        }

        if (isIndividualDeleteMode) {
            holder.removeBtn.visibility = View.GONE
            holder.markOverlay.visibility = if (markedForDeletion.contains(uri)) View.VISIBLE else View.GONE
            holder.removeBtn.setOnClickListener(null)

            holder.itemView.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val currentUri = photoList[currentPosition]
                if (markedForDeletion.contains(currentUri)) {
                    markedForDeletion.remove(currentUri)
                } else {
                    markedForDeletion.add(currentUri)
                }
                notifyItemChanged(currentPosition)
                onMarksChanged(markedForDeletion.size)
            }
        } else {
            holder.removeBtn.visibility = View.VISIBLE
            holder.markOverlay.visibility = View.GONE
            holder.itemView.setOnClickListener(null)

            holder.removeBtn.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    photoList.removeAt(currentPosition)
                    notifyItemRemoved(currentPosition)
                    notifyItemRangeChanged(currentPosition, photoList.size)
                    onListChanged()
                }
            }
        }
    }

    // Long-press preview — enlarge is also always available via its button,
    // this stays as a full-size confirmation view.
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

    fun getMarkedCount(): Int = markedForDeletion.size

    fun deleteMarkedPhotos() {
        photoList.removeAll { markedForDeletion.contains(it) }
        markedForDeletion.clear()
        notifyDataSetChanged()
        onListChanged()
    }

    fun clearMarks() {
        markedForDeletion.clear()
        notifyDataSetChanged()
    }

    fun deleteAllPhotos() {
        photoList.clear()
        notifyDataSetChanged()
        onListChanged()
    }

    override fun getItemCount(): Int = photoList.size
}
