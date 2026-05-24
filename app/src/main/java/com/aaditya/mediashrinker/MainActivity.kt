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
import androidx.appcompat.app.AlertDialog
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

    private var selectedImageUris =
        mutableListOf<Uri>()

    private var compressedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        showWhatsNewPopup()

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

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.instagram.com/carryon.aditya"
                    )
                )
            )
        }

        instagramOption.setOnClickListener {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.instagram.com/carryon.aditya"
                    )
                )
            )
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

            intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                true
            )

            intent.type = "image/*"

            startActivityForResult(intent, 100)
        }

        compressButton.setOnClickListener {

            if (selectedImageUris.isEmpty()) {

                Toast.makeText(
                    this,
                    "Select images first",
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

            compressAllImages(
                targetKB.toInt()
            )
        }

        openFolderButton.setOnClickListener {

            Toast.makeText(
                this,
                "Saved in Gallery → DCIM → MediaShrinker",
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

    private fun compressAllImages(
        targetKB: Int
    ) {

        resultText.text =
            "Processing Images..."

        var completed = 0

        for (uri in selectedImageUris) {

            val mimeType =
                contentResolver.getType(uri)

            if (mimeType == "image/png") {

                AlertDialog.Builder(this)

                    .setTitle(
                        "PNG Image Detected"
                    )

                    .setMessage(
                        "Do you want to convert this PNG image to JPEG for better compression?"
                    )

                    .setPositiveButton(
                        "YES"
                    ) { _, _ ->

                        compressImage(
                            uri,
                            targetKB,
                            true
                        )
                    }

                    .setNegativeButton(
                        "NO"
                    ) { _, _ ->

                        compressImage(
                            uri,
                            targetKB,
                            false
                        )
                    }

                    .show()

            } else {

                compressImage(
                    uri,
                    targetKB,
                    true
                )
            }

            completed++
        }

        resultText.text =
            "$completed Images Processed"
    }

    private fun compressImage(
        imageUri: Uri,
        targetKB: Int,
        convertToJpg: Boolean
    ) {

        try {

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

            val mimeType =
                contentResolver.getType(imageUri)

            var minQuality = 5
            var maxQuality = 100
            var bestBytes: ByteArray? = null

            while (minQuality <= maxQuality) {

                val quality =
                    (minQuality + maxQuality) / 2

                val outputStream =
                    ByteArrayOutputStream()

                if (convertToJpg) {

                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        quality,
                        outputStream
                    )

                } else {

                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        quality,
                        outputStream
                    )
                }

                val bytes =
                    outputStream.toByteArray()

                val sizeKB =
                    bytes.size / 1024

                if (sizeKB <= targetKB) {

                    bestBytes = bytes

                    minQuality =
                        quality + 1

                } else {

                    maxQuality =
                        quality - 1
                }
            }

            if (bestBytes == null) {

                return
            }

            val finalSize =
                bestBytes.size / 1024

            val reducedPercent =
                100 - ((finalSize * 100) / originalKB)

            val extension =
                if (convertToJpg)
                    ".jpg"
                else
                    ".png"

            val mime =
                if (convertToJpg)
                    "image/jpeg"
                else
                    "image/png"

            val filename =
                "compressed_${System.currentTimeMillis()}$extension"

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        filename
                    )

                    put(
                        MediaStore.Images.Media.MIME_TYPE,
                        mime
                    )

                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "DCIM/MediaShrinker"
                    )
                }

            val savedUri =
                contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )

            if (savedUri != null) {

                compressedImageUri =
                    savedUri

                val stream =
                    contentResolver.openOutputStream(savedUri)

                stream?.write(bestBytes)

                stream?.flush()

                stream?.close()
            }

            originalSizeText.text =
                "Original Size: ${originalKB} KB"

            compressedSizeText.text =
                "Compressed Size: ${finalSize} KB"

            if (
                mimeType == "image/png" &&
                convertToJpg
            ) {

                resultText.text =
                    "PNG Converted to JPEG • Reduced by ${reducedPercent}%"

            } else {

                resultText.text =
                    "Reduced by ${reducedPercent}%"
            }

        } catch (e: Exception) {

            resultText.text =
                "Compression Failed"
        }
    }

    private fun showWhatsNewPopup() {

        val prefs =
            getSharedPreferences(
                "MediaShrinkerPrefs",
                MODE_PRIVATE
            )

        val currentVersion =
            "3.0"

        val lastSeenVersion =
            prefs.getString(
                "last_seen_version",
                ""
            )

        if (lastSeenVersion != currentVersion) {

            val message = """

✨ What's New in v3.0

• Multiple Photo Selection
• Smart PNG Detection
• PNG to JPEG Converter
• PNG Compression Support
• Better Compression Accuracy
• Faster Batch Processing
• Premium Modern UI
• Smart Compression System

Supported Formats:

• JPG
• JPEG
• PNG
• WEBP

Coming Soon:

• AI Compression
• Auto Update Checker
• Live Compression Slider
• Compression History

Developer Aaditya Shukla 🥂

            """.trimIndent()

            AlertDialog.Builder(this)

                .setTitle("MediaShrinker Updated")

                .setMessage(message)

                .setPositiveButton(
                    "Continue"
                ) { dialog, _ ->

                    dialog.dismiss()
                }

                .show()

            prefs.edit()

                .putString(
                    "last_seen_version",
                    currentVersion
                )

                .apply()
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

            selectedImageUris.clear()

            if (data.clipData != null) {

                val clipData =
                    data.clipData!!

                for (i in 0 until clipData.itemCount) {

                    val uri =
                        clipData.getItemAt(i).uri

                    selectedImageUris.add(uri)
                }

                imagePreview.setImageURI(
                    selectedImageUris[0]
                )

                resultText.text =
                    "${selectedImageUris.size} Images Selected"

            } else if (data.data != null) {

                val uri =
                    data.data!!

                selectedImageUris.add(uri)

                imagePreview.setImageURI(uri)

                resultText.text =
                    "1 Image Selected"
            }
        }
    }
}