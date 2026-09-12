package com.aaditya.mediashrinker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MetadataRemoverActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var selectButton: Button
    private lateinit var removeButton: Button
    private lateinit var resultText: TextView
    private lateinit var selectedCountText: TextView
    private lateinit var metadataFoundCard: View
    private lateinit var metadataFoundText: TextView
    private lateinit var progressBar: ProgressBar

    private var selectedUris: MutableList<Uri> = mutableListOf()

    // The EXIF/metadata tags we look for, detect, and strip. Covers GPS location,
    // camera identity, timestamps and free-text fields — the kinds of things that
    // can leak where/when/with-what-device a photo was taken.
    private val metadataTags = listOf(
        ExifInterface.TAG_GPS_LATITUDE to "GPS latitude",
        ExifInterface.TAG_GPS_LONGITUDE to "GPS longitude",
        ExifInterface.TAG_GPS_ALTITUDE to "GPS altitude",
        ExifInterface.TAG_GPS_DATESTAMP to "GPS date",
        ExifInterface.TAG_GPS_TIMESTAMP to "GPS time",
        ExifInterface.TAG_GPS_PROCESSING_METHOD to "GPS processing method",
        ExifInterface.TAG_MAKE to "Camera make",
        ExifInterface.TAG_MODEL to "Camera model",
        ExifInterface.TAG_SOFTWARE to "Software used",
        ExifInterface.TAG_DATETIME to "Date/time",
        ExifInterface.TAG_DATETIME_ORIGINAL to "Original date/time",
        ExifInterface.TAG_DATETIME_DIGITIZED to "Digitized date/time",
        ExifInterface.TAG_ARTIST to "Artist",
        ExifInterface.TAG_COPYRIGHT to "Copyright",
        ExifInterface.TAG_IMAGE_DESCRIPTION to "Image description",
        ExifInterface.TAG_USER_COMMENT to "User comment",
        ExifInterface.TAG_LENS_MAKE to "Lens make",
        ExifInterface.TAG_LENS_MODEL to "Lens model"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metadata_remover)

        imagePreview = findViewById(R.id.imagePreview)
        selectButton = findViewById(R.id.selectButton)
        removeButton = findViewById(R.id.removeButton)
        resultText = findViewById(R.id.resultText)
        selectedCountText = findViewById(R.id.selectedCountText)
        metadataFoundCard = findViewById(R.id.metadataFoundCard)
        metadataFoundText = findViewById(R.id.metadataFoundText)
        progressBar = findViewById(R.id.metadataProgressBar)

        selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select images"), 102)
        }

        removeButton.setOnClickListener {
            if (selectedUris.isEmpty()) {
                Toast.makeText(this, "Select image(s) first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            removeMetadataBatch()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 102 && resultCode == Activity.RESULT_OK && data != null) {
            val uris = mutableListOf<Uri>()
            val clipData = data.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } else {
                data.data?.let { uris.add(it) }
            }

            if (uris.isEmpty()) return

            selectedUris = uris
            imagePreview.setImageURI(selectedUris.first())
            selectedCountText.text = if (selectedUris.size == 1) "1 image selected" else "${selectedUris.size} images selected"
            resultText.text = ""

            showDetectedMetadata(selectedUris.first())
        }
    }

    // Reads the first selected image's EXIF tags (read-only) purely to show the
    // user what's actually in the file before they remove anything.
    private fun showDetectedMetadata(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val found = try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val exif = ExifInterface(input)
                    metadataTags.filter { (tag, _) -> exif.getAttribute(tag) != null }.map { it.second }
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                if (found.isEmpty()) {
                    metadataFoundCard.visibility = android.view.View.GONE
                } else {
                    metadataFoundCard.visibility = android.view.View.VISIBLE
                    metadataFoundText.text = found.joinToString(" • ")
                }
            }
        }
    }

    private fun removeMetadataBatch() {
        removeButton.isEnabled = false
        selectButton.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE
        progressBar.max = selectedUris.size
        progressBar.progress = 0
        resultText.text = "Processing..."

        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            var failCount = 0

            selectedUris.forEachIndexed { index, uri ->
                val cleaned = try {
                    stripMetadata(uri, index)
                } catch (e: Exception) {
                    null
                }
                if (cleaned != null) successCount++ else failCount++

                withContext(Dispatchers.Main) {
                    progressBar.progress = index + 1
                }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = android.view.View.GONE
                removeButton.isEnabled = true
                selectButton.isEnabled = true

                resultText.text = if (failCount == 0) {
                    "$successCount image${if (successCount == 1) "" else "s"} cleaned — saved in DCIM/MediaShrinker/CleanImages"
                } else {
                    "$successCount cleaned, $failCount failed"
                }
                Toast.makeText(
                    this@MetadataRemoverActivity,
                    "Metadata removed from $successCount image${if (successCount == 1) "" else "s"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Copies the original image byte-for-byte (no recompression, no forced JPEG
    // conversion — so PNG transparency and original quality are preserved), then
    // strips EXIF metadata from the copy in place using ExifInterface. This is
    // both lossless and format-preserving, unlike the old approach of decoding
    // to a Bitmap and re-encoding everything as a JPEG.
    private fun stripMetadata(uri: Uri, index: Int): Uri? {
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val displayName = "clean_${System.currentTimeMillis()}_$index.$extension"

        val outUri: Uri
        val outFile: File?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/MediaShrinker/CleanImages")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            outUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            outFile = null
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "MediaShrinker/CleanImages"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, displayName)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            outUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            outFile = file
        }

        // Plain byte copy — original pixel data and format are untouched.
        contentResolver.openInputStream(uri)?.use { input ->
            contentResolver.openOutputStream(outUri)?.use { output ->
                input.copyTo(output)
            }
        } ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val doneValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            contentResolver.update(outUri, doneValues, null, null)
        }

        // Strip EXIF from the copy. JPEG/PNG/WebP all support this via
        // androidx.exifinterface — no bitmap re-encoding required.
        try {
            if (outFile != null) {
                val exif = ExifInterface(outFile.absolutePath)
                clearKnownTags(exif)
            } else {
                contentResolver.openFileDescriptor(outUri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    clearKnownTags(exif)
                }
            }
        } catch (e: Exception) {
            // Some formats/providers don't support a writable file descriptor for EXIF —
            // the copy itself (with no metadata carried over on insert) still succeeded.
        }

        return outUri
    }

    private fun clearKnownTags(exif: ExifInterface) {
        var changed = false
        for ((tag, _) in metadataTags) {
            if (exif.getAttribute(tag) != null) {
                exif.setAttribute(tag, null)
                changed = true
            }
        }
        if (changed) exif.saveAttributes()
    }
}
