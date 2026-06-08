package com.aaditya.mediashrinker

import android.app.Activity
import android.content.ActivityNotFoundException
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
    private lateinit var historyOption: TextView
    private lateinit var pdfHistoryOption: TextView
    private lateinit var resizeOption: TextView
    private lateinit var buyCoffeeOption: TextView

    private lateinit var imagePreview: ImageView

    private lateinit var selectImageButton: Button
    private lateinit var compressButton: Button
    private lateinit var shareButton: Button
    private lateinit var openFolderButton: Button
    private lateinit var createPdfButton: Button
    private lateinit var compareButton: Button

    private lateinit var targetSizeInput: EditText

    private lateinit var resultText: TextView
    private lateinit var originalSizeText: TextView
    private lateinit var compressedSizeText: TextView

    private lateinit var storageSavedText: TextView
    private lateinit var reductionText: TextView
    private lateinit var resolutionText: TextView
    private lateinit var formatText: TextView

    private lateinit var qualitySeekBar: SeekBar
    private lateinit var qualityText: TextView

    private lateinit var ultraModeButton: Button
    private lateinit var balancedModeButton: Button
    private lateinit var maxModeButton: Button

    private var selectedQuality = 80

    private var selectedImageUris = mutableListOf<Uri>()

    private var compressedImageUri: Uri? = null

    private var originalImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        showWhatsNewPopup()

        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.menuButton)
        contactDeveloperOption = findViewById(R.id.contactDeveloperOption)
        instagramOption = findViewById(R.id.instagramOption)
        aboutAppOption = findViewById(R.id.aboutAppOption)
        suggestionOption = findViewById(R.id.suggestionOption)
        historyOption = findViewById(R.id.historyOption)
        pdfHistoryOption = findViewById(R.id.pdfHistoryOption)
        resizeOption = findViewById(R.id.resizeOption)
        buyCoffeeOption = findViewById(R.id.buyCoffeeOption)
        imagePreview = findViewById(R.id.imagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        compressButton = findViewById(R.id.compressButton)
        shareButton = findViewById(R.id.shareButton)
        openFolderButton = findViewById(R.id.openFolderButton)
        createPdfButton = findViewById(R.id.createPdfButton)
        compareButton = findViewById(R.id.compareButton)
        targetSizeInput = findViewById(R.id.targetSizeInput)
        resultText = findViewById(R.id.resultText)
        originalSizeText = findViewById(R.id.originalSizeText)
        compressedSizeText = findViewById(R.id.compressedSizeText)
        storageSavedText = findViewById(R.id.storageSavedText)
        reductionText = findViewById(R.id.reductionText)
        resolutionText = findViewById(R.id.resolutionText)
        formatText = findViewById(R.id.formatText)
        qualitySeekBar = findViewById(R.id.qualitySeekBar)
        qualityText = findViewById(R.id.qualityText)
        ultraModeButton = findViewById(R.id.ultraModeButton)
        balancedModeButton = findViewById(R.id.balancedModeButton)
        maxModeButton = findViewById(R.id.maxModeButton)

        // --- Drawer ---

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // --- SeekBar ---

        qualitySeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedQuality = progress
                    qualityText.text = "Quality: ${progress}%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        // --- Mode Buttons ---

        ultraModeButton.setOnClickListener {
            selectedQuality = 95
            qualitySeekBar.progress = 95
            qualityText.text = "Mode: Ultra Quality • 95%"
        }

        balancedModeButton.setOnClickListener {
            selectedQuality = 75
            qualitySeekBar.progress = 75
            qualityText.text = "Mode: Balanced • 75%"
        }

        maxModeButton.setOnClickListener {
            selectedQuality = 40
            qualitySeekBar.progress = 40
            qualityText.text = "Mode: Maximum Compression • 40%"
        }

        // --- Drawer Options ---

        contactDeveloperOption.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/carryon.aditya"))
            )
        }

        instagramOption.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/carryon.aditya"))
            )
        }

        aboutAppOption.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        suggestionOption.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:blastergaming98611info@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "MediaShrinker Suggestion")
            startActivity(intent)
        }

        historyOption.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        pdfHistoryOption.setOnClickListener {
            startActivity(Intent(this, PdfHistoryActivity::class.java))
        }

        resizeOption.setOnClickListener {
            startActivity(Intent(this, ResizeActivity::class.java))
        }

        buyCoffeeOption.setOnClickListener {
            startActivity(Intent(this, DonateActivity::class.java))
        }

        // --- Main Buttons ---

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        compressButton.setOnClickListener {
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Select images first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetKB = targetSizeInput.text.toString()
            if (targetKB.isEmpty()) {
                Toast.makeText(this, "Enter target KB size", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            compressAllImages(targetKB.toInt())
        }

        compareButton.setOnClickListener {
            if (originalImageUri == null || compressedImageUri == null) {
                Toast.makeText(this, "Compress image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CompareActivity::class.java)
            intent.putExtra("before", originalImageUri.toString())
            intent.putExtra("after", compressedImageUri.toString())
            startActivity(intent)
        }

        createPdfButton.setOnClickListener {
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Select photos first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resultText.text = "Creating PDF..."

            val pdfUri = PdfUtils.createPdf(this, selectedImageUris)

            if (pdfUri != null) {
                savePdfHistory(pdfUri.toString())
                resultText.text = "PDF Created Successfully"
                Toast.makeText(this, "PDF Saved in Documents/MediaShrinker", Toast.LENGTH_LONG).show()

                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(pdfUri, "application/pdf")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "No PDF Viewer Found", Toast.LENGTH_SHORT).show()
                }

            } else {
                resultText.text = "PDF Creation Failed"
            }
        }

        openFolderButton.setOnClickListener {
            Toast.makeText(this, "Saved in Gallery → DCIM → MediaShrinker", Toast.LENGTH_LONG).show()
        }

        shareButton.setOnClickListener {
            if (compressedImageUri == null) {
                Toast.makeText(this, "No compressed image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, compressedImageUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        }
    }

    // --- Compression Logic ---

    private fun compressAllImages(targetKB: Int) {

        resultText.text = "Processing Images..."

        var completed = 0

        for (uri in selectedImageUris) {

            val mimeType = contentResolver.getType(uri)

            if (mimeType == "image/png") {
                AlertDialog.Builder(this)
                    .setTitle("PNG Image Detected")
                    .setMessage("Convert PNG to JPEG for better compression?")
                    .setPositiveButton("YES") { _, _ ->
                        compressImage(uri, targetKB, true)
                    }
                    .setNegativeButton("NO") { _, _ ->
                        compressImage(uri, targetKB, false)
                    }
                    .show()
            } else {
                compressImage(uri, targetKB, true)
            }

            completed++
        }

        resultText.text = "$completed Images Processed"
    }

    private fun compressImage(imageUri: Uri, targetKB: Int, convertToJpg: Boolean) {

        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val originalBytes = contentResolver.openInputStream(imageUri)?.readBytes()
            val originalKB = (originalBytes?.size ?: 0) / 1024

            val mimeType = contentResolver.getType(imageUri)

            val outputStream = ByteArrayOutputStream()

            if (convertToJpg) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, selectedQuality, outputStream)
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, selectedQuality, outputStream)
            }

            val bestBytes = outputStream.toByteArray()
            val finalSize = bestBytes.size / 1024
            val reducedPercent = 100 - ((finalSize * 100) / originalKB)
            val savedKB = originalKB - finalSize

            val extension = if (convertToJpg) ".jpg" else ".png"
            val mime = if (convertToJpg) "image/jpeg" else "image/png"
            val filename = "compressed_${System.currentTimeMillis()}$extension"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/MediaShrinker")
            }

            val savedUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (savedUri != null) {
                compressedImageUri = savedUri

                val stream = contentResolver.openOutputStream(savedUri)
                stream?.write(bestBytes)
                stream?.flush()
                stream?.close()

                saveToHistory(
                    savedUri.toString(),
                    "${originalKB} KB",
                    "${finalSize} KB",
                    "${reducedPercent}%"
                )
            }

            originalSizeText.text = "Original Size: ${originalKB} KB"
            compressedSizeText.text = "Compressed Size: ${finalSize} KB"
            storageSavedText.text = "Saved Space: ${savedKB} KB"
            reductionText.text = "Reduction: ${reducedPercent}%"
            resolutionText.text = "Resolution: ${bitmap.width} x ${bitmap.height}"

            if (mimeType == "image/png" && convertToJpg) {
                formatText.text = "Format: PNG → JPG"
                resultText.text = "PNG Converted to JPEG • Reduced by ${reducedPercent}%"
            } else {
                formatText.text = "Format: JPG"
                resultText.text = "Reduced by ${reducedPercent}%"
            }

        } catch (e: Exception) {
            resultText.text = "Compression Failed"
        }
    }

    // --- History ---

    private fun saveToHistory(
        imageUri: String,
        originalSize: String,
        compressedSize: String,
        reducedPercent: String
    ) {
        val prefs = getSharedPreferences("MediaShrinkerHistory", MODE_PRIVATE)

        val historySet = prefs.getStringSet("history", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()

        val historyItem = "$imageUri|$originalSize|$compressedSize|$reducedPercent"
        historySet.add(historyItem)

        prefs.edit().putStringSet("history", historySet).apply()
    }

    private fun savePdfHistory(pdfUri: String) {
        val prefs = getSharedPreferences("MediaShrinkerPdfHistory", MODE_PRIVATE)

        val pdfSet = prefs.getStringSet("pdf_history", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()

        pdfSet.add(pdfUri)

        prefs.edit().putStringSet("pdf_history", pdfSet).apply()
    }

    // --- What's New Popup ---

    private fun showWhatsNewPopup() {
        val prefs = getSharedPreferences("MediaShrinkerPrefs", MODE_PRIVATE)
        val currentVersion = "4.0"
        val lastSeenVersion = prefs.getString("last_seen_version", "")

        if (lastSeenVersion != currentVersion) {
            val message = """
✨ What's New in v4.0

• Real PDF Generator
• Compare Images
• Compression History
• PDF History
• Image Resizer
• Custom Resize Size
• Storage Analytics
• Live Compression Slider
• Smart PNG Detection
• Faster Compression
• Premium Modern UI
• Better Stability

Supported Formats:

• JPG
• JPEG
• PNG
• WEBP
• PDF

Developer Aaditya Shukla 🥂
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("MediaShrinker Updated")
                .setMessage(message)
                .setPositiveButton("Continue") { dialog, _ -> dialog.dismiss() }
                .show()

            prefs.edit().putString("last_seen_version", currentVersion).apply()
        }
    }

    // --- Activity Result ---

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {

            selectedImageUris.clear()

            if (data.clipData != null) {
                val clipData = data.clipData!!

                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    selectedImageUris.add(uri)

                    if (i == 0) {
                        originalImageUri = uri
                    }
                }

                imagePreview.setImageURI(selectedImageUris[0])
                resultText.text = "${selectedImageUris.size} Images Selected"

            } else if (data.data != null) {
                val uri = data.data!!
                selectedImageUris.add(uri)
                originalImageUri = uri
                imagePreview.setImageURI(uri)
                resultText.text = "1 Image Selected"
            }
        }
    }
}
