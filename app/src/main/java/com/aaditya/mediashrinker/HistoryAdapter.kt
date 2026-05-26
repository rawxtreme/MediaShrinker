package com.aaditya.mediashrinker

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(

    private val historyList:
    MutableList<CompressionHistory>

) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imageView:
        ImageView =
            itemView.findViewById(R.id.historyImage)

        val originalSize:
        TextView =
            itemView.findViewById(R.id.originalSize)

        val compressedSize:
        TextView =
            itemView.findViewById(R.id.compressedSize)

        val reducedPercent:
        TextView =
            itemView.findViewById(R.id.reducedPercent)

        val shareButton:
        Button =
            itemView.findViewById(R.id.shareButton)

        val openButton:
        Button =
            itemView.findViewById(R.id.openButton)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.item_history,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item =
            historyList[position]

        holder.imageView.setImageURI(
            Uri.parse(item.imageUri)
        )

        holder.originalSize.text =
            "Original: ${item.originalSize}"

        holder.compressedSize.text =
            "Compressed: ${item.compressedSize}"

        holder.reducedPercent.text =
            "Reduced: ${item.reducedPercent}"

        holder.shareButton.setOnClickListener {

            val intent =
                Intent(Intent.ACTION_SEND)

            intent.type =
                "image/*"

            intent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(item.imageUri)
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            holder.itemView.context.startActivity(

                Intent.createChooser(
                    intent,
                    "Share Image"
                )
            )
        }

        holder.openButton.setOnClickListener {

            val intent =
                Intent(Intent.ACTION_VIEW)

            intent.setDataAndType(
                Uri.parse(item.imageUri),
                "image/*"
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            holder.itemView.context.startActivity(
                intent
            )
        }
    }

    override fun getItemCount():
    Int {

        return historyList.size
    }
}