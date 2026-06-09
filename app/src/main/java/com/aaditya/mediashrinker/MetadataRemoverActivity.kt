package com.aaditya.mediashrinker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class MetadataRemoverActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var selectButton: Button
    private lateinit var removeButton: Button
    private lateinit var resultText: TextView

    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_metadata_remover)

        imagePreview = findViewById(R.id.imagePreview)
        selectButton = findViewById(R.id.selectButton)
        removeButton = findViewById(R.id.removeButton)
        resultText = findViewById(R.id.resultText)

        selectButton.setOnClickListener {

            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            intent.type = "image/*"

            startActivityForResult(intent, 102)
        }

        removeButton.setOnClickListener {

            if (selectedUri == null) {

                Toast.makeText(
                    this,
                    "Select image first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            removeMetadata()
        }
    }

    private fun removeMetadata() {

        try {

            val bitmap = BitmapFactory.decodeStream(
                contentResolver.openInputStream(selectedUri!!)
            )

            val output = ByteArrayOutputStream()

            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                output
            )

            val values = ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "clean_${System.currentTimeMillis()}.jpg"
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
                )

                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "DCIM/MediaShrinker/CleanImages"
                )
            }

            val savedUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            savedUri?.let {

                contentResolver.openOutputStream(it)?.use { stream ->

                    stream.write(output.toByteArray())
                }

                resultText.text =
                    "Saved in DCIM/MediaShrinker/CleanImages"

                Toast.makeText(
                    this,
                    "Metadata Removed",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            resultText.text = "Failed to remove metadata"
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == 102 &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {

            selectedUri = data.data

            imagePreview.setImageURI(selectedUri)

            resultText.text = "Image Selected"
        }
    }
}