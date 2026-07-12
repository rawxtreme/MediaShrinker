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
import android.text.Editable
import android.text.TextWatcher
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
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var removeImageBtn: TextView
    private lateinit var previewImagesBtn: TextView
    private lateinit var taskCompleteBanner: LinearLayout
    private lateinit var taskCompleteBannerTitle: TextView
    private lateinit var taskCompleteBannerSubtitle: TextView
    private lateinit var taskCompleteBannerClearBtn: TextView
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
    private var selectPhotosTapCount = 0
    private var compressedImageUri: Uri? = null
    private var originalImageUri: Uri? = null
    private var lastSavedUri: Uri? = null
    private var lastSavedFilename: String? = null
    private var vibrator: Vibrator? = null

    // Slider lock — true means user typed manually, slider blocked
    private var isManualInput = false

    private var customSavePath = "DCIM/MediaShrinker"
    private val saveLocationPaths = arrayOf(
        "DCIM/MediaShrinker",
        "Pictures/MediaShrinker",
        "Downloads/MediaShrinker"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val prefs = getSharedPreferences("MediaShrinkerSettings", MODE_PRIVATE)
        customSavePath = prefs.getString("save_location", "DCIM/MediaShrinker") ?: "DCIM/MediaShrinker"

        bindViews()
        setupListeners()
        requestNotificationPermissionIfNeeded()
        handleIncomingShareIntent(intent)
        checkCompletedTaskIntent(intent)
    }

    // Fires when the app is ALREADY open and the user taps the completion
    // notification — Android reuses the existing MainActivity instance
    // (because of FLAG_ACTIVITY_SINGLE_TOP) and delivers the new intent here
    // instead of calling onCreate() again. Without this override, the banner
    // would only show on a cold start, not when the app was already open.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
            checkCompletedTaskIntent(intent)
        }
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
        removeImageBtn = findViewById(R.id.removeImageBtn)
        previewImagesBtn = findViewById(R.id.previewImagesBtn)
        taskCompleteBanner = findViewById(R.id.taskCompleteBanner)
        taskCompleteBannerTitle = findViewById(R.id.taskCompleteBannerTitle)
        taskCompleteBannerSubtitle = findViewById(R.id.taskCompleteBannerSubtitle)
        taskCompleteBannerClearBtn = findViewById(R.id.taskCompleteBannerClearBtn)
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
        saveFolderText.text = "📁 Save to: $customSavePath  (tap to change)"
    }

    private fun setupListeners() {
        menuButton.setOnClickListener {
            hapticLight()
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // =============================================
        // SLIDER LOCK — only works when input is empty
        // =============================================
        targetSizeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isManualInput = !s.isNullOrEmpty()
                if (isManualInput) {
                    qualitySeekBar.isEnabled = false
                    qualityText.alpha = 0.4f
                } else {
                    qualitySeekBar.isEnabled = true
                    qualityText.alpha = 1f
                }
            }
        })

        qualitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isManualInput) {
                    selectedQuality = progress
                    qualityText.text = "$progress%"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        ultraModeButton.setOnClickListener {
            hapticLight()
            targetSizeInput.setText("")
            setMode(95, "Ultra • 95%")
        }

        balancedModeButton.setOnClickListener {
            hapticLight()
            targetSizeInput.setText("")
            setMode(75, "Balanced • 75%")
        }

        maxModeButton.setOnClickListener {
            hapticLight()
            targetSizeInput.setText("")
            setMode(40, "Max • 40%")
        }

        // Drawer
        contactDeveloperOption.setOnClickListener { openUrl("https://www.instagram.com/carryon.aditya") }
        instagramOption.setOnClickListener { openUrl("https://www.instagram.com/carryon.aditya") }
        aboutAppOption.setOnClickListener { startAndClose(AboutActivity::class.java) }
        suggestionOption.setOnClickListener {
            hapticLight()
            drawerLayout.closeDrawer(GravityCompat.START)
            showEmailConfirmationDialog()
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

        selectImageButton.setOnClickListener {
            hapticLight()

            val maxReselections = 3
            if (selectPhotosTapCount >= maxReselections) {
                Toast.makeText(
                    this,
                    "You can only reselect photos up to $maxReselections times. Please continue with your current selection.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            selectPhotosTapCount++

            val intent = Intent(this, CustomPhotoPickerActivity::class.java)
            intent.putParcelableArrayListExtra("pre_selected_uris", ArrayList(selectedImageUris))
            startActivityForResult(intent, 100)
        }

        removeImageBtn.setOnClickListener {
            hapticLight()
            selectedImageUris.clear()
            originalImageUri = null
            selectPhotosTapCount = 0
            updateImageSelectionUI()
        }

        previewImagesBtn.setOnClickListener {
            hapticLight()
            val intent = Intent(this, PhotoPreviewActivity::class.java)
            intent.putParcelableArrayListExtra("photo_uris", ArrayList(selectedImageUris))
            startActivityForResult(intent, 200)
        }

        previewQualityButton.setOnClickListener {
            hapticLight()
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showQualityPreview()
        }

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

            val targetText = targetSizeInput.text.toString()

            if (targetText.isNotEmpty()) {
                // User entered target KB — use accurate binary search
                val targetKB = targetText.toIntOrNull()
                if (targetKB == null || targetKB <= 0) {
                    Toast.makeText(this, "Enter a valid KB value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Validate against original size
                val uri = selectedImageUris[0]
                CoroutineScope(Dispatchers.IO).launch {
                    val bytes = contentResolver.openInputStream(uri)?.readBytes()
                    val originalKB = (bytes?.size ?: 0) / 1024
                    withContext(Dispatchers.Main) {
                        if (targetKB >= originalKB) {
                            showSizeError(originalKB, targetKB)
                        } else {
                            compressAllImages(targetKB, useTargetMode = true)
                        }
                    }
                }
            } else {
                // Slider mode — use selectedQuality directly
                compressAllImages(0, useTargetMode = false)
            }
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

            val maxPdfPages = 50
            if (selectedImageUris.size > maxPdfPages) {
                Toast.makeText(this, "You can convert up to $maxPdfPages photos into a PDF at once", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            showPdfProgressDialogAndCreate()
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
    // ACCURATE COMPRESSION ENGINE
    // =============================================

    private fun compressToTargetKB(bitmap: Bitmap, targetKB: Int): ByteArray {
        var low = 1
        var high = 100
        var bestBytes = ByteArrayOutputStream().also {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)
        }.toByteArray()

        repeat(14) {
            val mid = (low + high) / 2
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, mid, out)
            val bytes = out.toByteArray()
            val sizeKB = bytes.size / 1024

            if (sizeKB <= targetKB) {
                bestBytes = bytes
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return bestBytes
    }

    private fun showSizeError(originalKB: Int, enteredKB: Int) {
        val suggest25 = (originalKB * 0.75).toInt()
        val suggest50 = (originalKB * 0.50).toInt()
        val suggest75 = (originalKB * 0.25).toInt()

        val message = "Target size (${enteredKB} KB) is equal to or larger than the original (${originalKB} KB).\n\n" +
            "Compressing would make the file LARGER, not smaller!\n\n" +
            "Suggested targets:\n" +
            "• Light → ${suggest25} KB\n" +
            "• Medium → ${suggest50} KB\n" +
            "• Heavy → ${suggest75} KB"

        AlertDialog.Builder(this)
            .setTitle("Invalid Target Size")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
        hapticMedium()
    }

    private fun compressAllImages(targetKB: Int, useTargetMode: Boolean) {
        val total = selectedImageUris.size
        val hasPng = selectedImageUris.any { contentResolver.getType(it) == "image/png" }

        if (hasPng && total == 1 && !useTargetMode) {
            AlertDialog.Builder(this)
                .setTitle("PNG Detected")
                .setMessage("Convert to JPEG for better compression?")
                .setPositiveButton("YES") { _, _ -> startBatchCompress(targetKB, convertToJpg = true, useTargetMode) }
                .setNegativeButton("NO") { _, _ -> startBatchCompress(targetKB, convertToJpg = false, useTargetMode) }
                .show()
        } else {
            startBatchCompress(targetKB, convertToJpg = true, useTargetMode)
        }
    }

    private fun startBatchCompress(targetKB: Int, convertToJpg: Boolean, useTargetMode: Boolean) {
        val total = selectedImageUris.size
        showProgressDialog(total)
        ProcessingService.start(this, "Compressing Photos")

        CoroutineScope(Dispatchers.IO).launch {
            var lastSavedKB = 0
            var lastReducedPercent = 0

            for ((index, uri) in selectedImageUris.withIndex()) {
                val current = index + 1
                withContext(Dispatchers.Main) { updateProgress(current, total, "Processing image $current...") }
                ProcessingService.updateProgress(current, total, "Compressing $current of $total")
                val result = compressImageAsync(uri, targetKB, convertToJpg, useTargetMode)
                lastSavedKB = result.first
                lastReducedPercent = result.second
                withContext(Dispatchers.Main) { updateProgress(current, total, "Image $current done") }
                delay(80)
            }

            withContext(Dispatchers.Main) {
                dismissProgressDialog()
                ProcessingService.stop(this@MainActivity)
                ProcessingService.showCompletionNotification(
                    this@MainActivity,
                    "Compression Complete",
                    "Tap to view your compressed photo(s)",
                    "compress"
                )
                resultText.text = "$total image(s) compressed"
                selectPhotosTapCount = 0
                if (total > 0) showSuccessAnimation(lastSavedKB, lastReducedPercent)
            }
        }
    }

    private fun compressImageAsync(
        imageUri: Uri,
        targetKB: Int,
        convertToJpg: Boolean,
        useTargetMode: Boolean
    ): Pair<Int, Int> {
        return try {
            val bytes = contentResolver.openInputStream(imageUri)?.readBytes() ?: return Pair(0, 0)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val originalKB = bytes.size / 1024
            val mimeType = contentResolver.getType(imageUri)

            val finalBytes: ByteArray

            if (useTargetMode) {
                // Accurate binary search to hit target KB
                val effectiveTarget = if (originalKB > 200) targetKB.coerceAtLeast(200) else targetKB
                finalBytes = compressToTargetKB(bitmap, effectiveTarget)
            } else {
                // Slider quality mode
                val out = ByteArrayOutputStream()
                if (convertToJpg) bitmap.compress(Bitmap.CompressFormat.JPEG, selectedQuality, out)
                else bitmap.compress(Bitmap.CompressFormat.PNG, selectedQuality, out)
                finalBytes = out.toByteArray()
            }

            val finalKB = finalBytes.size / 1024
            val reduction = if (originalKB > 0) 100 - ((finalKB * 100) / originalKB) else 0
            val savedKB = originalKB - finalKB

            val ext = if (convertToJpg) ".jpg" else ".png"
            val mime = if (convertToJpg) "image/jpeg" else "image/png"
            // Readable date/time name instead of a raw 13-digit timestamp —
            // still unique enough for a batch (down to the millisecond) but
            // no longer looks like meaningless numbers to the user.
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val filename = "MediaShrinker_$timeStamp$ext"

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
    // QUALITY PREVIEW
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
                        .setPositiveButton("Compress Now") { _, _ ->
                            compressAllImages(estimatedKB, useTargetMode = true)
                        }
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
    // CUSTOM SAVE LOCATION
    // =============================================

    private fun showSaveLocationPicker() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_location, null)

        val optionRows = listOf(
            dialogView.findViewById<LinearLayout>(R.id.saveLocOption0),
            dialogView.findViewById<LinearLayout>(R.id.saveLocOption1),
            dialogView.findViewById<LinearLayout>(R.id.saveLocOption2)
        )
        val checkMarks = listOf(
            dialogView.findViewById<TextView>(R.id.saveLocCheck0),
            dialogView.findViewById<TextView>(R.id.saveLocCheck1),
            dialogView.findViewById<TextView>(R.id.saveLocCheck2)
        )

        var selectedIndex = saveLocationPaths.indexOf(customSavePath).coerceAtLeast(0)

        fun refreshSelection() {
            for (i in optionRows.indices) {
                if (i == selectedIndex) {
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
                hapticLight()
                selectedIndex = i
                refreshSelection()
            }
        }

        dialogView.findViewById<Button>(R.id.saveLocCancelBtn).setOnClickListener {
            hapticLight()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.saveLocSaveBtn).setOnClickListener {
            hapticLight()
            customSavePath = saveLocationPaths[selectedIndex]
            getSharedPreferences("MediaShrinkerSettings", MODE_PRIVATE)
                .edit().putString("save_location", customSavePath).apply()
            updateSaveLocationLabel()
            Toast.makeText(this, "Save location updated!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // =============================================
    // PROGRESS + SUCCESS DIALOGS
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
            .setView(dialogView).setCancelable(false).create()
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
        dialogView.findViewById<TextView>(R.id.successSaved).text = "Saved $savedKB KB"
        dialogView.findViewById<TextView>(R.id.successReduction).text = "Reduced by $reducedPercent%"

        val icon = dialogView.findViewById<TextView>(R.id.successIcon)
        val scaleAnim = ScaleAnimation(0f, 1.2f, 0f, 1.2f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
            .apply { duration = 300; fillAfter = true }
        val scaleBack = ScaleAnimation(1.2f, 1f, 1.2f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
            .apply { duration = 150; startOffset = 300; fillAfter = true }
        val animSet = AnimationSet(false).apply { addAnimation(scaleAnim); addAnimation(scaleBack) }
        icon.startAnimation(animSet)

        val successDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()
        successDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.successShareBtn).setOnClickListener {
            hapticLight(); successDialog.dismiss()
            compressedImageUri?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "image/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(shareIntent, "Share Compressed Image"))
            }
            showTaskCompleteBanner("Compression Complete", "View in History")
        }
        dialogView.findViewById<Button>(R.id.successRenameBtn).setOnClickListener {
            hapticLight(); successDialog.dismiss(); showRenameDialog()
        }
        dialogView.findViewById<Button>(R.id.successDoneBtn).setOnClickListener {
            hapticLight(); successDialog.dismiss()
            showTaskCompleteBanner("Compression Complete", "View in History")
        }

        successDialog.show()
        hapticSuccess()
    }

    private fun showRenameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null)
        val input = dialogView.findViewById<EditText>(R.id.renameInput)
        // Deliberately left blank instead of pre-filling with the internal
        // timestamp-based filename — that name is only for uniqueness on disk,
        // not something the user should see or need to edit around.
        input.setText("")

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.renameCancelBtn).setOnClickListener {
            hapticLight()
            dialog.dismiss()
            showTaskCompleteBanner("Compression Complete", "View in History")
        }

        dialogView.findViewById<Button>(R.id.renameConfirmBtn).setOnClickListener {
            hapticLight()
            val newName = input.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "Enter a filename", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lastSavedUri != null) {
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
            dialog.dismiss()
            showTaskCompleteBanner("Compression Complete", "View in History")
        }

        dialog.show()
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
    // EMAIL CONFIRMATION DIALOG (Suggestions / Bug Report)
    // =============================================

    private fun showEmailConfirmationDialog() {
        val message = "Please Note: The email you send will usually be read directly by the developer or a member of the development team. This helps us understand your suggestion or report and provide you with a faster and more accurate response. By continuing, you agree that the developer may contact you by email regarding your suggestion, feedback, or issue. It is recommended that you read the •Email to Developer• section in Settings → How to Use the App before sending your email.We recommend reading the •Email to Developer• section in Settings → How to Use the App before sending your email."

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_email_confirm, null)
        dialogView.findViewById<TextView>(R.id.emailDialogMessage).text = message

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.emailDialogCancelBtn).setOnClickListener {
            hapticLight()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.emailDialogContinueBtn).setOnClickListener {
            hapticLight()
            dialog.dismiss()
            sendSuggestionEmail()
        }

        dialog.show()
    }


    private fun sendSuggestionEmail() {
        val deviceInfo = """
            Device Name: ${Build.DEVICE}
            Device Model: ${Build.MODEL}
            Android Version: ${Build.VERSION.RELEASE}
            Operating System: Android
            Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}
            Time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}
        """.trimIndent()

        val body = "$deviceInfo\n\n"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("scope8xaditya@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "MediaShrinker - Suggestions / Bug Report")
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

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
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0,50,50,100), intArrayOf(0,100,0,200), -1))
        else @Suppress("DEPRECATION") vibrator?.vibrate(longArrayOf(0,50,50,100), -1)
    }

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
    // TASK COMPLETE SLIDING BANNER
    // Replaces the old blocking "Start New Activity?" dialog. This banner
    // slides down from the top, shows for 3 seconds, then slides back out —
    // no tap required. It's triggered from two places:
    //   1) Directly after compress/PDF finishes, if the app is in foreground.
    //   2) From onCreate, if the app was opened by tapping the completion
    //      notification — see checkCompletedTaskIntent().
    // =============================================

    private fun showTaskCompleteBanner(title: String, subtitle: String) {
        taskCompleteBannerTitle.text = title
        taskCompleteBannerSubtitle.text = subtitle

        taskCompleteBanner.visibility = View.VISIBLE
        taskCompleteBanner.translationY = -300f
        taskCompleteBanner.alpha = 1f
        taskCompleteBanner.animate()
            .translationY(0f)
            .setDuration(300)
            .start()

        taskCompleteBanner.setOnClickListener {
            hapticLight()
            if (subtitle.contains("PDF")) {
                startActivity(Intent(this, PdfHistoryActivity::class.java))
            } else {
                startActivity(Intent(this, HistoryActivity::class.java))
            }
        }

        // One-tap way to reset for a fresh task — replaces the old blocking
        // "Start New Activity?" Yes/No dialog with a quicker inline action.
        taskCompleteBannerClearBtn.setOnClickListener {
            hapticLight()
            selectedImageUris.clear()
            originalImageUri = null
            compressedImageUri = null
            selectPhotosTapCount = 0
            updateImageSelectionUI()
            Toast.makeText(this, "Selection cleared", Toast.LENGTH_SHORT).show()

            taskCompleteBanner.animate()
                .translationY(-300f)
                .setDuration(300)
                .withEndAction {
                    taskCompleteBanner.visibility = View.GONE
                }
                .start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (taskCompleteBanner.visibility == View.VISIBLE) {
                taskCompleteBanner.animate()
                    .translationY(-300f)
                    .setDuration(300)
                    .withEndAction {
                        taskCompleteBanner.visibility = View.GONE
                    }
                    .start()
            }
        }, 3000)
    }

    // Called from onCreate — if this launch came from tapping the completion
    // notification, show the banner fresh right now (instead of relying on
    // whatever happened while the app was in the background).
    private fun checkCompletedTaskIntent(incomingIntent: Intent) {
        when (incomingIntent.getStringExtra("completed_task")) {
            "compress" -> showTaskCompleteBanner("Compression Complete", "View in History")
            "pdf" -> showTaskCompleteBanner("PDF Creation Complete", "View in PDF History")
        }
    }

    // =============================================
    // PDF PROGRESS DIALOG (Convert to PDF)
    // =============================================

    private fun showPdfProgressDialogAndCreate() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pdf_progress, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.pdfProgressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.pdfProgressText)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val total = selectedImageUris.size
        progressText.text = "0 / $total photos processed"
        ProcessingService.start(this, "Creating PDF")

        // Runs the heavy PDF-building work on a background thread (Dispatchers.IO)
        // so the UI stays smooth. The onProgress callback below fires from that
        // background thread, so we hop back with runOnUiThread to safely touch views.
        CoroutineScope(Dispatchers.Main).launch {
            val pdfUri = withContext(Dispatchers.IO) {
                PdfUtils.createPdf(this@MainActivity, selectedImageUris) { current, totalCount ->
                    runOnUiThread {
                        val percent = (current * 100) / totalCount
                        progressBar.progress = percent
                        progressText.text = "$current / $totalCount photos processed"
                    }
                    ProcessingService.updateProgress(current, totalCount, "$current / $totalCount photos processed")
                }
            }

            dialog.dismiss()
            ProcessingService.stop(this@MainActivity)

            if (pdfUri != null) {
                savePdfHistory(pdfUri.toString())
                resultText.text = "PDF Created!"
                selectPhotosTapCount = 0
                ProcessingService.showCompletionNotification(
                    this@MainActivity,
                    "PDF Creation Complete",
                    "Tap to view your PDF",
                    "pdf"
                )
                Toast.makeText(this@MainActivity, "Saved in Documents/MediaShrinker", Toast.LENGTH_LONG).show()
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(pdfUri, "application/pdf")
                    i.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, "No PDF viewer found", Toast.LENGTH_SHORT).show()
                }
                showTaskCompleteBanner("PDF Creation Complete", "View in PDF History")
            } else {
                resultText.text = "PDF creation failed"
                Toast.makeText(this@MainActivity, "PDF creation failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            val pickedUris = data.getParcelableArrayListExtra<Uri>("selected_uris") ?: arrayListOf()
            selectedImageUris.clear()
            selectedImageUris.addAll(pickedUris)
            originalImageUri = selectedImageUris.firstOrNull()
            updateImageSelectionUI()
        } else if (requestCode == 200 && resultCode == Activity.RESULT_OK && data != null) {
            val updatedUris = data.getParcelableArrayListExtra<Uri>("updated_uris") ?: arrayListOf()
            selectedImageUris.clear()
            selectedImageUris.addAll(updatedUris)
            originalImageUri = selectedImageUris.firstOrNull()
            updateImageSelectionUI()
        }
    }

    private fun updateImageSelectionUI() {
        when {
            selectedImageUris.isEmpty() -> {
                imagePreview.setImageDrawable(null)
                resultText.text = "No Image Selected"
                removeImageBtn.visibility = View.GONE
                previewImagesBtn.visibility = View.GONE
            }
            selectedImageUris.size == 1 -> {
                imagePreview.setImageURI(selectedImageUris[0])
                resultText.text = "1 Image Selected"
                removeImageBtn.visibility = View.VISIBLE
                previewImagesBtn.visibility = View.GONE
            }
            else -> {
                imagePreview.setImageURI(selectedImageUris[0])
                resultText.text = "${selectedImageUris.size} Images Selected"
                removeImageBtn.visibility = View.GONE
                previewImagesBtn.visibility = View.VISIBLE
            }
        }
    }

    // =============================================
    // SHARE-TO-COMPRESS (receiving photos shared from other apps)
    // =============================================

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 601)
            }
        }
    }

    private fun handleIncomingShareIntent(incomingIntent: Intent) {
        when (incomingIntent.action) {
            Intent.ACTION_SEND -> {
                if (incomingIntent.type?.startsWith("image/") == true) {
                    val uri = incomingIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uri != null) {
                        applyImportedPhotos(listOf(uri))
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (incomingIntent.type?.startsWith("image/") == true) {
                    val uris = incomingIntent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uris != null) {
                        applyImportedPhotos(uris)
                    }
                }
            }
        }
    }

    // Applies photos shared in from another app (WhatsApp, Gallery, etc.) directly
    // as the current selection — no loading step, they appear selected instantly.
    private fun applyImportedPhotos(uris: List<Uri>) {
        val maxImportLimit = 100

        if (uris.size > maxImportLimit) {
            // Entire batch is rejected, not just the extra ones — avoids silently
            // dropping photos the user expected to be included.
            Toast.makeText(
                this,
                "Maximum $maxImportLimit photos allowed. Please share fewer photos.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        selectedImageUris.clear()
        selectedImageUris.addAll(uris)
        originalImageUri = selectedImageUris.firstOrNull()
        selectPhotosTapCount = 0
        updateImageSelectionUI()

        showImportSuccessDialog(uris.size)
    }

    private fun showImportSuccessDialog(count: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_success, null)
        val message = if (count == 1) "1 Photo Imported Successfully" else "$count Photos Imported Successfully"
        dialogView.findViewById<TextView>(R.id.importSuccessMessage).text = message

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Auto-dismiss after a short moment — no button needed for a simple confirmation.
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) dialog.dismiss()
        }, 1500)
    }

    // =============================================
    // EXIT CONFIRMATION (Back button only — Home button is untouched
    // and follows normal Android behavior automatically)
    // =============================================

    override fun onBackPressed() {
        showExitConfirmDialog()
    }

    private fun showExitConfirmDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_exit_confirm, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.exitNoBtn).setOnClickListener {
            hapticLight()
            dialog.dismiss()
            // Stay in the app — nothing changes.
        }

        dialogView.findViewById<Button>(R.id.exitYesBtn).setOnClickListener {
            hapticLight()
            dialog.dismiss()
            selectedImageUris.clear()
            originalImageUri = null
            compressedImageUri = null
            selectPhotosTapCount = 0
            // finishAffinity() closes the ENTIRE task (all activities in the back
            // stack), not just this screen — so the app truly exits. Next launch
            // creates brand new Activity instances with fresh, empty fields.
            finishAffinity()
        }

        dialog.show()
    }
}