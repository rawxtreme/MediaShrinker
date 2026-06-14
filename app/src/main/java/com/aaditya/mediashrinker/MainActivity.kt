package com.aaditya.mediashrinker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import kotlinx.coroutines.*
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
    private lateinit var settingsOption: TextView
    private lateinit var formatConverterOption: TextView
    private lateinit var metadataRemoverOption: TextView
    private lateinit var analyticsOption: TextView
    private lateinit var imageInfoOption: TextView

    private lateinit var imagePreview: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var compressButton: Button
    private lateinit var shareButton: Button
    private lateinit var openFolderButton: Button
    private lateinit var createPdfButton: Button
    private lateinit var compareButton: Button
    private lateinit var previewQualityButton: Button
    private lateinit var saveFolderText: TextView

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

    private var progressDialog: AlertDialog? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressSubText: TextView

    private var selectedQuality = 80
    private var selectedImageUris = mutableListOf<Uri>()
    private var compressedImageUri: Uri? = null
    private var originalImageUri: Uri? = null
    private var lastSavedUri: Uri? = null
    private var lastSavedFilename: String? = null
    private var vibrator: Vibrator? = null

    // Feature: Custom Save Location
    private var customSavePath = "DCIM/MediaShrinker"
    private val saveLocationOptions = arrayOf(
        "Gallery → DCIM/MediaShrinker",
        "Pictures/MediaShrinker",
        "Downloads/MediaShrinker"
    )
    private val saveLocationPaths = arrayOf(
        "DCIM/MediaShrinker",
        "Pictures/MediaShrinker",
        "Downloads/MediaShrinker"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // Load saved location
        val prefs = getSharedPreferences("MediaShrinkerSettings", MODE_PRIVATE)
        customSavePath = prefs.getString("save_location", "DCIM/MediaShrinker") ?: "DCIM/MediaShrinker"

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.menuButton)
        contactDeveloperOption = findViewById(R.id.contactDeveloperOption)
        instagramOption = findViewById(R.id.instagramOption)
        aboutAppOption = findViewById(R.id.aboutAppOption)
        suggestionOption = findViewById(R.id.suggestionOption)
        historyOption = findViewById(R.id.historyOption)
        pdfHistoryOption = findViewById(R.id.pdfHistoryOption)
        resizeOption = findViewById(R.id.resizeOption)
        formatConverterOption = findViewById(R.id.formatConverterOption)
        buyCoffeeOption = findViewById(R.id.buyCoffeeOption)
        settingsOption = findViewById(R.id.settingsOption)
        metadataRemoverOption = findViewById(R.id.metadataRemoverOption)
        analyticsOption = findViewById(R.id.analyticsOption)
        imageInfoOption = findViewById(R.id.imageInfoOption)
        imagePreview = findViewById(R.id.imagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        compressButton = findViewById(R.id.compressButton)
        shareButton = findViewById(R.id.shareButton)
        openFolderButton = findViewById(R.id.openFolderButton)
        createPdfButton = findViewById(R.id.createPdfButton)
        compareButton = findViewById(R.id.compareButton)
        previewQualityButton = findViewById(R.id.previewQualityButton)
        saveFolderText = findViewById(R.id.saveFolderText)
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

        updateSaveLocationLabel()
    }

    private fun updateSaveLocationLabel() {
        saveFolderText.text = "Save to: $customSavePath"
    }

    private fun setupListeners() {
        menuButton.setOnClickListener {
            hapticLight()
            drawerLayout.openDrawer(GravityCompat.START)
        }

        qualitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedQuality = progress
                qualityText.text = "${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        ultraModeButton.setOnClickListener { hapticLight(); setMode(95, "Ultra • 95%") }
        balancedModeButton.setOnClickListener { hapticLight(); setMode(75, "Balanced • 75%") }
        maxModeButton.setOnClickListener { hapticLight(); setMode(40, "Max • 40%") }

        // Drawer
        contactDeveloperOption.setOnClickListener { openUrl("https://www.instagram.com/carryon.aditya") }
        instagramOption.setOnClickListener { openUrl("https://www.instagram.com/carryon.aditya") }
        aboutAppOption.setOnClickListener { startAndClose(AboutActivity::class.java) }
        suggestionOption.setOnClickListener {
            val i = Intent(Intent.ACTION_SENDTO)
            i.data = Uri.parse("mailto:mediashrinker.app@gmail.com")
            i.putExtra(Intent.EXTRA_SUBJECT, "MediaShrinker Suggestion")
            startActivity(i)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        historyOption.setOnClickListener { startAndClose(HistoryActivity::class.java) }
        pdfHistoryOption.setOnClickListener { startAndClose(PdfHistoryActivity::class.java) }
        resizeOption.setOnClickListener { startAndClose(ResizeActivity::class.java) }
        settingsOption.setOnClickListener { startAndClose(SettingsActivity::class.java) }
        formatConverterOption.setOnClickListener { startAndClose(FormatConverterActivity::class.java) }
        metadataRemoverOption.setOnClickListener { startAndClose(MetadataRemoverActivity::class.java) }
        buyCoffeeOption.setOnClickListener { startAndClose(DonateActivity::class.java) }
        analyticsOption.setOnClickListener { startAndClose(StorageAnalyticsActivity::class.java) }
        imageInfoOption.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            if (originalImageUri != null) {
                val i = Intent(this, ImageInfoActivity::class.java)
                i.putExtra("imageUri", originalImageUri.toString())
                startActivity(i)
            } else {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        // Main buttons
        selectImageButton.setOnClickListener {
            hapticLight()
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        // Feature 4: Quality Preview before compress
        previewQualityButton.setOnClickListener {
            hapticLight()
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showQualityPreview()
        }

        // Feature 5: Custom Save Location
        saveFolderText.setOnClickListener {
            hapticLight()
            showSaveLocationPicker()
        }

        compressButton.setOnClickListener {
            hapticMedium()
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
            hapticLight()
            if (originalImageUri == null || compressedImageUri == null) {
                Toast.makeText(this, "Compress an image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, CompareActivity::class.java)
            i.putExtra("before", originalImageUri.toString())
            i.putExtra("after", compressedImageUri.toString())
            startActivity(i)
        }

        createPdfButton.setOnClickListener {
            hapticLight()
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Select photos first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resultText.text = "Creating PDF..."
            val pdfUri = PdfUtils.createPdf(this, selectedImageUris)
            if (pdfUri != null) {
                savePdfHistory(pdfUri.toString())
                resultText.text = "PDF Created!"
                Toast.makeText(this, "Saved in Documents/MediaShrinker", Toast.LENGTH_LONG).show()
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(pdfUri, "application/pdf")
                    i.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show()
                }
            } else {
                resultText.text = "PDF creation failed"
            }
        }

        openFolderButton.setOnClickListener {
            hapticLight()
            Toast.makeText(this, "Saved in Gallery → $customSavePath", Toast.LENGTH_LONG).show()
        }

        shareButton.setOnClickListener {
            hapticLight()
            if (compressedImageUri == null) {
                Toast.makeText(this, "No compressed image yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, compressedImageUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        }
    }

    // =============================================
    // FEATURE 4: QUALITY PREVIEW
    // =============================================

    private fun showQualityPreview() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = selectedImageUris[0]
                val bytes = contentResolver.openInputStream(uri)?.readBytes()
                val originalKB = (bytes?.size ?: 0) / 1024

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes?.size ?: 0)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, selectedQuality, out)
                val estimatedKB = out.size() / 1024
                val reduction = if (originalKB > 0) 100 - ((estimatedKB * 100) / originalKB) else 0

                withContext(Dispatchers.Main) {
                    val dialogView = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.dialog_quality_preview, null)

                    dialogView.findViewById<TextView>(R.id.previewOriginalSize).text = "$originalKB KB"
                    dialogView.findViewById<TextView>(R.id.previewEstSize).text = "~$estimatedKB KB"
                    dialogView.findViewById<TextView>(R.id.previewReduction).text = "~$reduction%"

                    AlertDialog.Builder(this@MainActivity)
                        .setView(dialogView)
                        .setPositiveButton("Compress Now") { _, _ -> compressAllImages(estimatedKB) }
                        .setNegativeButton("Adjust Quality", null)
                        .create()
                        .also { it.window?.setBackgroundDrawableResource(android.R.color.transparent) }
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Could not preview", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // =============================================
    // FEATURE 5: CUSTOM SAVE LOCATION
    // =============================================

    private fun showSaveLocationPicker() {
        var selectedIndex = saveLocationPaths.indexOf(customSavePath).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Save Location")
            .setSingleChoiceItems(saveLocationOptions, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Save") { _, _ ->
                customSavePath = saveLocationPaths[selectedIndex]
                getSharedPreferences("MediaShrinkerSettings", MODE_PRIVATE)
                    .edit().putString("save_location", customSavePath).apply()
                updateSaveLocationLabel()
                Toast.makeText(this, "Save location updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =============================================
    // HELPERS
    // =============================================

    private fun setMode(quality: Int, label: String) {
        selectedQuality = quality
        qualitySeekBar.progress = quality
        qualityText.text = label
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun startAndClose(cls: Class<*>) {
        startActivity(Intent(this, cls))
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    // =============================================
    // PROGRESS + SUCCESS
    // =============================================

    private fun showProgressDialog(total: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)
        progressBar = dialogView.findViewById(R.id.progressBar)
        progressText = dialogView.findViewById(R.id.progressText)
        progressSubText = dialogView.findViewById(R.id.progressSubText)
        progressBar.max = total
        progressBar.progress = 0
        progressText.text = "Starting..."
        progressSubText.text = "0 of $total images"
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        progressDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        progressDialog?.show()
    }

    private fun updateProgress(current: Int, total: Int, msg: String) {
        progressBar.progress = current
        progressText.text = "Compressing $current of $total"
        progressSubText.text = msg
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showSuccessAnimation(savedKB: Int, reducedPercent: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)
        dialogView.findViewById<TextView>(R.id.successTitle).text = "Compression Complete!"
        dialogView.findViewById<TextView>(R.id.successSaved).text = "Saved ${savedKB} KB"
        dialogView.findViewById<TextView>(R.id.successReduction).text = "Reduced by ${reducedPercent}%"

        val icon = dialogView.findViewById<TextView>(R.id.successIcon)
        val scaleAnim = ScaleAnimation(0f, 1.2f, 0f, 1.2f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply { duration = 300; fillAfter = true }
        val scaleBack = ScaleAnimation(1.2f, 1f, 1.2f, 1f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply { duration = 150; startOffset = 300; fillAfter = true }
        val animSet = AnimationSet(false).apply { addAnimation(scaleAnim); addAnimation(scaleBack) }
        icon.startAnimation(animSet)

        val successDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        successDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.successShareBtn).setOnClickListener {
            hapticLight()
            successDialog.dismiss()
            compressedImageUri?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "image/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(shareIntent, "Share Compressed Image"))
            }
        }

        dialogView.findViewById<Button>(R.id.successRenameBtn).setOnClickListener {
            hapticLight()
            successDialog.dismiss()
            showRenameDialog()
        }

        dialogView.findViewById<Button>(R.id.successDoneBtn).setOnClickListener {
            hapticLight()
            successDialog.dismiss()
        }

        successDialog.show()
        hapticSuccess()
    }

    private fun showRenameDialog() {
        val input = EditText(this)
        input.hint = "Enter new filename"
        input.setText(lastSavedFilename?.removeSuffix(".jpg")?.removeSuffix(".png") ?: "compressed_image")
        input.setTextColor(0xFFFFFFFF.toInt())
        input.setHintTextColor(0x88FFFFFF.toInt())
        input.setPadding(32, 24, 32, 24)
        input.background = null

        AlertDialog.Builder(this)
            .setTitle("Rename File")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && lastSavedUri != null) {
                    val ext = if (lastSavedFilename?.endsWith(".png") == true) ".png" else ".jpg"
                    val finalName = if (newName.endsWith(".jpg") || newName.endsWith(".png")) newName else "$newName$ext"
                    try {
                        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, finalName) }
                        contentResolver.update(lastSavedUri!!, values, null, null)
                        Toast.makeText(this, "Renamed to $finalName", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =============================================
    // COMPRESSION
    // =============================================

    private fun compressAllImages(targetKB: Int) {
        val total = selectedImageUris.size
        val hasPng = selectedImageUris.any { contentResolver.getType(it) == "image/png" }

        if (hasPng && total == 1) {
            AlertDialog.Builder(this)
                .setTitle("PNG Detected")
                .setMessage("Convert to JPEG for better compression?")
                .setPositiveButton("YES") { _, _ -> startBatchCompress(targetKB, true) }
                .setNegativeButton("NO") { _, _ -> startBatchCompress(targetKB, false) }
                .show()
        } else {
            startBatchCompress(targetKB, true)
        }
    }

    private fun startBatchCompress(targetKB: Int, convertToJpg: Boolean) {
        val total = selectedImageUris.size
        showProgressDialog(total)

        CoroutineScope(Dispatchers.IO).launch {
            var lastSavedKB = 0
            var lastReducedPercent = 0

            for ((index, uri) in selectedImageUris.withIndex()) {
                val current = index + 1
                withContext(Dispatchers.Main) { updateProgress(current, total, "Processing image $current...") }
                val result = compressImageAsync(uri, targetKB, convertToJpg)
                lastSavedKB = result.first
                lastReducedPercent = result.second
                withContext(Dispatchers.Main) { updateProgress(current, total, "Image $current done") }
                delay(80)
            }

            withContext(Dispatchers.Main) {
                dismissProgressDialog()
                resultText.text = "$total image(s) compressed"
                if (total > 0) showSuccessAnimation(lastSavedKB, lastReducedPercent)
            }
        }
    }

    private fun compressImageAsync(imageUri: Uri, targetKB: Int, convertToJpg: Boolean): Pair<Int, Int> {
        return try {
            val bytes = contentResolver.openInputStream(imageUri)?.readBytes() ?: return Pair(0, 0)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val originalKB = bytes.size / 1024
            val mimeType = contentResolver.getType(imageUri)

            val out = ByteArrayOutputStream()
            if (convertToJpg) bitmap.compress(Bitmap.CompressFormat.JPEG, selectedQuality, out)
            else bitmap.compress(Bitmap.CompressFormat.PNG, selectedQuality, out)

            val finalBytes = out.toByteArray()
            val finalKB = finalBytes.size / 1024
            val reduction = if (originalKB > 0) 100 - ((finalKB * 100) / originalKB) else 0
            val savedKB = originalKB - finalKB

            val ext = if (convertToJpg) ".jpg" else ".png"
            val mime = if (convertToJpg) "image/jpeg" else "image/png"
            val filename = "compressed_${System.currentTimeMillis()}$ext"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, customSavePath)
            }

            val savedUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (savedUri != null) {
                compressedImageUri = savedUri
                lastSavedUri = savedUri
                lastSavedFilename = filename
                contentResolver.openOutputStream(savedUri)?.use { it.write(finalBytes) }
                saveToHistory(savedUri.toString(), "${originalKB} KB", "${finalKB} KB", "${reduction}%")
            }

            Handler(Looper.getMainLooper()).post {
                originalSizeText.text = "$originalKB KB"
                compressedSizeText.text = "$finalKB KB"
                storageSavedText.text = "$savedKB KB saved"
                reductionText.text = "$reduction% less"
                resolutionText.text = "${bitmap.width}x${bitmap.height}"
                formatText.text = if (mimeType == "image/png" && convertToJpg) "PNG→JPG" else "JPG"
            }

            Pair(savedKB, reduction)
        } catch (e: Exception) { Pair(0, 0) }
    }

    // =============================================
    // HAPTIC
    // =============================================

    private fun hapticLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator?.vibrate(30)
    }

    private fun hapticMedium() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator?.vibrate(60)
    }

    private fun hapticSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 100), intArrayOf(0, 100, 0, 200), -1))
        else @Suppress("DEPRECATION") vibrator?.vibrate(longArrayOf(0, 50, 50, 100), -1)
    }

    // =============================================
    // HISTORY
    // =============================================

    private fun saveToHistory(imageUri: String, originalSize: String, compressedSize: String, reducedPercent: String) {
        val prefs = getSharedPreferences("MediaShrinkerHistory", MODE_PRIVATE)
        val set = prefs.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add("$imageUri|$originalSize|$compressedSize|$reducedPercent")
        prefs.edit().putStringSet("history", set).apply()
    }

    private fun savePdfHistory(pdfUri: String) {
        val prefs = getSharedPreferences("MediaShrinkerPdfHistory", MODE_PRIVATE)
        val set = prefs.getStringSet("pdf_history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(pdfUri)
        prefs.edit().putStringSet("pdf_history", set).apply()
    }

    // =============================================
    // ACTIVITY RESULT
    // =============================================

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUris.clear()
            if (data.clipData != null) {
                val clip = data.clipData!!
                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri
                    selectedImageUris.add(uri)
                    if (i == 0) originalImageUri = uri
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