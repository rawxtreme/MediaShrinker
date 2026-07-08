package com.aaditya.mediashrinker

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.*

class ImageInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_info)
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        val uriString = intent.getStringExtra("imageUri")
        if (uriString != null) {
            loadImageInfo(Uri.parse(uriString))
        }
    }

    private fun loadImageInfo(uri: Uri) {
        try {
            val preview = findViewById<ImageView>(R.id.infoImagePreview)
            preview.setImageURI(uri)

            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            val sizeKB = (bytes?.size ?: 0) / 1024
            val sizeMB = sizeKB / 1024f
            val sizeText = if (sizeKB > 1024) String.format("%.2f MB", sizeMB) else "$sizeKB KB"

            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }

            val width = options.outWidth
            val height = options.outHeight
            val mimeType = contentResolver.getType(uri) ?: "Unknown"
            val format = mimeType.substringAfter("/").uppercase()

            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val cursor = contentResolver.query(uri, projection, null, null, null)
            var fileName = "Unknown"
            var dateAdded = "Unknown"

            cursor?.use {
                if (it.moveToFirst()) {
                    fileName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: "Unknown"
                    val dateMillis = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) * 1000L
                    dateAdded = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(dateMillis))
                }
            }

            // EXIF - seedha normal URI se padhte hain, koi permission nahi chahiye
            // (sirf GPS ke liye special permission chahiye hoti hai, baaki sab yahi se mil jata hai)
            var cameraMake = "N/A"
            var cameraModel = "N/A"
            var flashUsed = "N/A"
            var focalLength = "N/A"

            try {
                val exifStream = contentResolver.openInputStream(uri)
                if (exifStream != null) {
                    val exif = ExifInterface(exifStream)
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/A"
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "N/A"
                    val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                    flashUsed = if (flash != -1) (if (flash and 1 != 0) "Yes" else "No") else "N/A"
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "N/A"
                    exifStream.close()
                }
            } catch (e: Exception) { /* exif not available */ }

            // Set all values
            setVal(R.id.infoFileName, fileName)
            setVal(R.id.infoFileSize, sizeText)
            setVal(R.id.infoResolution, "${width} x ${height} px")
            setVal(R.id.infoFormat, format)
            setVal(R.id.infoDateAdded, dateAdded)
            setVal(R.id.infoCameraMake, cameraMake)
            setVal(R.id.infoCameraModel, cameraModel)
            setVal(R.id.infoFlash, flashUsed)
            setVal(R.id.infoFocalLength, focalLength)

        } catch (e: Exception) {
            setVal(R.id.infoFileName, "Could not read image info")
        }
    }

    private fun setVal(id: Int, value: String) {
        findViewById<TextView>(id).text = value
    }
}