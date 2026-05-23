package com.aaditya.mediashrinker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: TextView

    private lateinit var contactDeveloperOption: TextView
    private lateinit var instagramOption: TextView
    private lateinit var aboutAppOption: TextView
    private lateinit var suggestionOption: TextView
    private lateinit var buyCoffeeOption: TextView

    private lateinit var imagePreview: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var compressButton: Button
    private lateinit var targetSizeInput: EditText

    private lateinit var resultText: TextView
    private lateinit var originalSizeText: TextView
    private lateinit var compressedSizeText: TextView

    private lateinit var openFolderButton: Button
    private lateinit var shareButton: Button

    private var selectedImageUri: Uri? = null
    private var compressedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        drawerLayout =
            findViewById(R.id.drawerLayout)

        menuButton =
            findViewById(R.id.menuButton)

        contactDeveloperOption =
            findViewById(R.id.contactDeveloperOption)

        instagramOption =
            findViewById(R.id.instagramOption)

        aboutAppOption =
            findViewById(R.id.aboutAppOption)

        suggestionOption =
            findViewById(R.id.suggestionOption)

        buyCoffeeOption =
            findViewById(R.id.buyCoffeeOption)

        imagePreview =
            findViewById(R.id.imagePreview)

        selectImageButton =
            findViewById(R.id.selectImageButton)

        compressButton =
            findViewById(R.id.compressButton)

        targetSizeInput =
            findViewById(R.id.targetSizeInput)

        resultText =
            findViewById(R.id.resultText)

        originalSizeText =
            findViewById(R.id.originalSizeText)

        compressedSizeText =
            findViewById(R.id.compressedSizeText)

        openFolderButton =
            findViewById(R.id.openFolderButton)

        shareButton =
            findViewById(R.id.shareButton)

        menuButton.setOnClickListener {

            drawerLayout.openDrawer(
                GravityCompat.START
            )
        }

        contactDeveloperOption.setOnClickListener {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.instagram.com/carryon.aditya"
                    )
                )

            startActivity(intent)
        }

        instagramOption.setOnClickListener {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.instagram.com/carryon.aditya"
                    )
                )

            startActivity(intent)
        }

        aboutAppOption.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AboutActivity::class.java
                )
            )
        }

        suggestionOption.setOnClickListener {

            val intent =
                Intent(Intent.ACTION_SENDTO)

            intent.data =
                Uri.parse(
                    "mailto:blastergaming98611info@gmail.com"
                )

            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "MediaShrinker Suggestion"
            )

            startActivity(intent)
        }

        buyCoffeeOption.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    DonateActivity::class.java
                )
            )
        }

        selectImageButton.setOnClickListener {

            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            startActivityForResult(intent, 100)
        }

        compressButton.setOnClickListener {

            if (selectedImageUri == null) {

                Toast.makeText(
                    this,
                    "Select an image first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val targetKB =
                targetSizeInput.text.toString()

            if (targetKB.isEmpty()) {

                Toast.makeText(
                    this,
                    "Enter target KB size",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            compressImage(
                selectedImageUri!!,
                targetKB.toInt()
            )
        }

        openFolderButton.setOnClickListener {

            Toast.makeText(
                this,
                "Check Gallery → DCIM → MediaShrinker",
                Toast.LENGTH_LONG
            ).show()
        }

        shareButton.setOnClickListener {

            if (compressedImageUri == null) {

                Toast.makeText(
                    this,
                    "No compressed image",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val shareIntent =
                Intent(Intent.ACTION_SEND)

            shareIntent.type =
                "image/jpeg"

            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                compressedImageUri
            )

            shareIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share Image"
                )
            )
        }
    }

    private fun compressImage(
        imageUri: Uri,
        targetKB: Int
    ) {

        try {

            resultText.text =
                "Compressing..."

            val inputStream =
                contentResolver.openInputStream(imageUri)

            val bitmap =
                BitmapFactory.decodeStream(inputStream)

            val originalBytes =
                contentResolver
                    .openInputStream(imageUri)
                    ?.readBytes()

            val originalKB =
                (originalBytes?.size ?: 0) / 1024

            var quality = 100

            val outputStream =
                ByteArrayOutputStream()

            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                outputStream
            )

            while (
                outputStream.toByteArray().size / 1024 > targetKB &&
                quality > 5
            ) {

                outputStream.reset()

                quality -= 5

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    outputStream
                )
            }

            val compressedBytes =
                outputStream.toByteArray()

            val finalSize =
                compressedBytes.size / 1024

            val filename =
                "compressed_${System.currentTimeMillis()}.jpg"

            val values =
                android.content.ContentValues().apply {

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

                compressedImageUri = uri

                val stream =
                    contentResolver.openOutputStream(uri)

                stream?.write(compressedBytes)

                stream?.flush()

                stream?.close()
            }

            originalSizeText.text =
                "Original Size: ${originalKB} KB"

            compressedSizeText.text =
                "Compressed Size: ${finalSize} KB"

            resultText.text =
                "Compression Completed Successfully"

            Toast.makeText(
                this,
                "Saved in Gallery",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            resultText.text =
                "Compression Failed"
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
            requestCode == 100 &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {

            selectedImageUri =
                data.data

            imagePreview.setImageURI(
                selectedImageUri
            )
        }
    }
}