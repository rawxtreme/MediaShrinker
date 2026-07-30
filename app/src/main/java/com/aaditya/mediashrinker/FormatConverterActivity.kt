package com.aaditya.mediashrinker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class FormatConverterActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var selectButton: Button
    private lateinit var convertButton: Button
    private lateinit var formatSelectorRow: LinearLayout
    private lateinit var selectedFormatText: TextView
    private lateinit var resultText: TextView

    private var selectedImageUri: Uri? = null

    private val formats = arrayOf(
        "PNG → JPG",
        "JPG → PNG",
        "WEBP → JPG",
        "WEBP → PNG"
    )
    private var selectedFormatIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_format_converter)

        imagePreview = findViewById(R.id.imagePreview)
        selectButton = findViewById(R.id.selectButton)
        convertButton = findViewById(R.id.convertButton)
        formatSelectorRow = findViewById(R.id.formatSelectorRow)
        selectedFormatText = findViewById(R.id.selectedFormatText)
        resultText = findViewById(R.id.resultText)

        selectedFormatText.text = formats[selectedFormatIndex]

        formatSelectorRow.setOnClickListener {
            showFormatSelectorDialog()
        }

        selectButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            intent.type = "image/*"
            startActivityForResult(intent, 101)
        }

        convertButton.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Select image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            convertImage()
        }
    }

    private fun showFormatSelectorDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_format_selector, null)

        val optionRows = listOf(
            dialogView.findViewById<LinearLayout>(R.id.formatOption0),
            dialogView.findViewById<LinearLayout>(R.id.formatOption1),
            dialogView.findViewById<LinearLayout>(R.id.formatOption2),
            dialogView.findViewById<LinearLayout>(R.id.formatOption3)
        )
        val checkMarks = listOf(
            dialogView.findViewById<TextView>(R.id.formatCheck0),
            dialogView.findViewById<TextView>(R.id.formatCheck1),
            dialogView.findViewById<TextView>(R.id.formatCheck2),
            dialogView.findViewById<TextView>(R.id.formatCheck3)
        )

        var tempSelectedIndex = selectedFormatIndex

        fun refreshSelection() {
            for (i in optionRows.indices) {
                if (i == tempSelectedIndex) {
                    optionRows[i].setBackgroundResource(R.drawable.save_option_bg_selected)
                    checkMarks[i].text = "✓"
                    checkMarks[i].setBackgroundResource(R.drawable.check_badge_bg)
                } else {
                    optionRows[i].setBackgroundResource(R.drawable.save_option_bg)
                    checkMarks[i].text = ""
                    checkMarks[i].setBackgroundResource(R.drawable.radio_unselected_bg)
                }
            }
        }
        refreshSelection()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        for (i in optionRows.indices) {
            optionRows[i].setOnClickListener {
                tempSelectedIndex = i
                refreshSelection()
            }
        }

        dialogView.findViewById<Button>(R.id.formatSelectorDoneBtn).setOnClickListener {
            selectedFormatIndex = tempSelectedIndex
            selectedFormatText.text = formats[selectedFormatIndex]
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun convertImage() {
        try {
            val bitmap = BitmapFactory.decodeStream(
                contentResolver.openInputStream(selectedImageUri!!)
            )

            val output = ByteArrayOutputStream()
            val selectedFormat = formats[selectedFormatIndex]

            var extension = ".jpg"
            var mime = "image/jpeg"

            when (selectedFormat) {
                "PNG → JPG",
                "WEBP → JPG" -> {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    extension = ".jpg"
                    mime = "image/jpeg"
                }
                "JPG → PNG",
                "WEBP → PNG" -> {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    extension = ".png"
                    mime = "image/png"
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "converted_${System.currentTimeMillis()}$extension")
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/MediaShrinker/Converted")
            }

            val savedUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (savedUri != null) {
                val stream = contentResolver.openOutputStream(savedUri)
                stream?.write(output.toByteArray())
                stream?.flush()
                stream?.close()

                resultText.text = "Saved in DCIM/MediaShrinker/Converted"
                Toast.makeText(this, "Conversion Complete", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            resultText.text = "Conversion Failed"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imagePreview.setImageURI(selectedImageUri)
            resultText.text = "Image Selected"
        }
    }
}
