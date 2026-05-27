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

class ResizeActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView

    private lateinit var selectButton: Button
    private lateinit var resize1080: Button
    private lateinit var resize720: Button
    private lateinit var resize512: Button

    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_resize)

        imagePreview =
            findViewById(R.id.resizePreview)

        selectButton =
            findViewById(R.id.selectResizeImage)

        resize1080 =
            findViewById(R.id.resize1080)

        resize720 =
            findViewById(R.id.resize720)

        resize512 =
            findViewById(R.id.resize512)

        selectButton.setOnClickListener {

            val intent =
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )

            intent.type = "image/*"

            startActivityForResult(intent, 200)
        }

        resize1080.setOnClickListener {

            resizeImage(1080, 1080)
        }

        resize720.setOnClickListener {

            resizeImage(720, 720)
        }

        resize512.setOnClickListener {

            resizeImage(512, 512)
        }
    }

    private fun resizeImage(
        width: Int,
        height: Int
    ) {

        if (selectedUri == null) {

            Toast.makeText(
                this,
                "Select image first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val inputStream =
                contentResolver.openInputStream(selectedUri!!)

            val bitmap =
                BitmapFactory.decodeStream(inputStream)

            val resizedBitmap =
                Bitmap.createScaledBitmap(
                    bitmap,
                    width,
                    height,
                    true
                )

            val filename =
                "resized_${System.currentTimeMillis()}.jpg"

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        filename
                    )

                    put(
                        MediaStore.Images.Media.MIME_TYPE,
                        "image/jpeg"
                    )

                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "DCIM/MediaShrinker"
                    )
                }

            val uri =
                contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )

            if (uri != null) {

                val stream =
                    contentResolver.openOutputStream(uri)

                resizedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    100,
                    stream
                )

                stream?.flush()

                stream?.close()

                Toast.makeText(
                    this,
                    "Image Saved",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Resize Failed",
                Toast.LENGTH_SHORT
            ).show()
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
            requestCode == 200 &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {

            selectedUri =
                data.data

            imagePreview.setImageURI(
                selectedUri
            )
        }
    }
}