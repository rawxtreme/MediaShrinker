package com.aaditya.mediashrinker

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhotoPreviewAdapter(
    private val photoList: MutableList<Uri>,
    private val onListChanged: () -> Unit
) : RecyclerView.Adapter<PhotoPreviewAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.previewItemImage)
        val removeBtn: TextView = itemView.findViewById(R.id.previewItemRemoveBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = photoList[position]
        holder.imageView.setImageURI(uri)

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

    override fun getItemCount(): Int = photoList.size
}
