package com.aaditya.mediashrinker

import android.net.Uri
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
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<PhotoPickerAdapter.ViewHolder>() {

    // This list holds only the URIs the user has tapped/selected so far.
    // allPhotos never changes; selectedUris is the one that grows/shrinks.
    private val selectedUris = mutableListOf<Uri>()

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
        holder.imageView.setImageURI(uri)

        val isSelected = selectedUris.contains(uri)

        // Show the blue checkmark badge only if this exact photo is selected
        holder.checkOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

        // If limit is already reached AND this particular photo is NOT selected,
        // dim it out so the user visually understands it can't be tapped right now.
        val limitReached = selectedUris.size >= maxSelection
        holder.dimOverlay.visibility = if (limitReached && !isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
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

            // Refresh the whole grid so checkmarks AND dim overlays update everywhere.
            // (Simple and reliable — fine for a photo picker screen size.)
            notifyDataSetChanged()
        }
    }

    fun getSelectedUris(): List<Uri> = selectedUris

    override fun getItemCount(): Int = allPhotos.size
}
