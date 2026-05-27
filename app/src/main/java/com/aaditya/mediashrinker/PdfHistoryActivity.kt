package com.aaditya.mediashrinker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PdfHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pdf_history)

        val container =
            findViewById<LinearLayout>(
                R.id.pdfContainer
            )

        val prefs =
            getSharedPreferences(
                "MediaShrinkerPdfHistory",
                MODE_PRIVATE
            )

        val pdfSet =
            prefs.getStringSet(
                "pdf_history",
                mutableSetOf()
            )

        if (
            pdfSet == null ||
            pdfSet.isEmpty()
        ) {

            val emptyText =
                TextView(this)

            emptyText.text =
                "No PDF History"

            emptyText.textSize = 20f

            emptyText.setTextColor(
                android.graphics.Color.WHITE
            )

            container.addView(emptyText)

            return
        }

        for (pdf in pdfSet.reversed()) {

            val card =
                layoutInflater.inflate(
                    R.layout.item_pdf,
                    null
                )

            val pdfName =
                card.findViewById<TextView>(
                    R.id.pdfName
                )

            val openButton =
                card.findViewById<TextView>(
                    R.id.openPdfButton
                )

            val shareButton =
                card.findViewById<TextView>(
                    R.id.sharePdfButton
                )

            val uri =
                Uri.parse(pdf)

            pdfName.text =
                uri.lastPathSegment

            openButton.setOnClickListener {

                val intent =
                    Intent(Intent.ACTION_VIEW)

                intent.setDataAndType(
                    uri,
                    "application/pdf"
                )

                intent.flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION

                startActivity(intent)
            }

            shareButton.setOnClickListener {

                val shareIntent =
                    Intent(Intent.ACTION_SEND)

                shareIntent.type =
                    "application/pdf"

                shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share PDF"
                    )
                )
            }

            container.addView(card)
        }
    }
}