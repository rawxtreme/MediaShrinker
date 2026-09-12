package com.aaditya.mediashrinker

import android.Manifest
import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A fully in-app camera screen — the system camera app is never opened.
 * Two modes:
 *  - "single": captures exactly one photo and returns immediately.
 *  - "multiple": user can keep tapping the shutter as many times as they
 *    want (no artificial cap), review/remove shots in the thumbnail strip,
 *    then tap Done to hand all of them back to the caller in one go.
 *
 * Every capture is saved straight into MediaStore (Pictures/MediaShrinker),
 * so photos show up in the device gallery immediately — no separate
 * "save this?" round trip needed.
 */
class InAppCameraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "capture_mode"
        const val MODE_SINGLE = "single"
        const val MODE_MULTIPLE = "multiple"
        const val EXTRA_RESULT_URIS = "captured_uris"
        private const val CAMERA_PERMISSION_REQUEST = 900
    }

    private lateinit var previewView: PreviewView
    private lateinit var shutterButton: View
    private lateinit var captureFlashOverlay: View
    private lateinit var closeBtn: ImageView
    private lateinit var flashBtn: ImageView
    private lateinit var flipBtn: ImageView
    private lateinit var doneBtn: TextView
    private lateinit var modeBadge: TextView
    private lateinit var countText: TextView
    private lateinit var thumbStrip: RecyclerView

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    private var mode = MODE_MULTIPLE
    private val capturedUris = mutableListOf<Uri>()
    private lateinit var thumbAdapter: ThumbAdapter
    private var isCapturing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_camera)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_MULTIPLE

        previewView = findViewById(R.id.cameraPreview)
        shutterButton = findViewById(R.id.shutterButton)
        captureFlashOverlay = findViewById(R.id.captureFlashOverlay)
        closeBtn = findViewById(R.id.cameraCloseBtn)
        flashBtn = findViewById(R.id.cameraFlashBtn)
        flipBtn = findViewById(R.id.cameraFlipBtn)
        doneBtn = findViewById(R.id.cameraDoneBtn)
        modeBadge = findViewById(R.id.cameraModeBadge)
        countText = findViewById(R.id.cameraCaptureCountText)
        thumbStrip = findViewById(R.id.capturedThumbStrip)

        modeBadge.text = if (mode == MODE_SINGLE) "Single Photo" else "Multiple Photos"

        if (mode == MODE_MULTIPLE) {
            thumbStrip.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            thumbAdapter = ThumbAdapter(capturedUris) { removedUri ->
                try { contentResolver.delete(removedUri, null, null) } catch (e: Exception) { }
                capturedUris.remove(removedUri)
                thumbAdapter.notifyDataSetChanged()
                refreshMultiModeUi()
            }
            thumbStrip.adapter = thumbAdapter
        }

        closeBtn.setOnClickListener { handleClose() }
        flashBtn.setOnClickListener { toggleFlash() }
        flipBtn.setOnClickListener { flipCamera() }
        shutterButton.setOnClickListener { onShutterTapped() }
        doneBtn.setOnClickListener { finishWithResult() }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is needed to take photos", Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun flipCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        bindCameraUseCases()
    }

    private fun toggleFlash() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = flashMode
        val label = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> "Flash: On"
            ImageCapture.FLASH_MODE_AUTO -> "Flash: Auto"
            else -> "Flash: Off"
        }
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
    }

    private fun onShutterTapped() {
        if (isCapturing) return
        val capture = imageCapture ?: return
        isCapturing = true

        animateShutterPress()

        val name = "MEDIASHRINKER_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())}.jpg"
        val outputOptions = buildOutputOptions(name)

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    val savedUri = output.savedUri ?: return
                    flashScreen()
                    onPhotoCaptured(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    Toast.makeText(this@InAppCameraActivity, "Capture failed, try again", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // MediaStore's RELATIVE_PATH column only works from Android 10 (API 29) onward.
    // Below that, we have to give it a concrete file path on external storage instead,
    // otherwise the insert silently fails on older devices.
    private fun buildOutputOptions(displayName: String): ImageCapture.OutputFileOptions {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MediaShrinker")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "MediaShrinker"
                )
                if (!dir.exists()) dir.mkdirs()
                put(MediaStore.Images.Media.DATA, File(dir, displayName).absolutePath)
            }
        }

        return ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
    }

    private fun onPhotoCaptured(uri: Uri) {
        // Clear IS_PENDING so the photo shows up in the gallery right away (API 29+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                contentResolver.update(uri, values, null, null)
            } catch (e: Exception) { }
        }

        capturedUris.add(uri)

        if (mode == MODE_SINGLE) {
            finishWithResult()
            return
        }

        thumbAdapter.notifyItemInserted(capturedUris.size - 1)
        thumbStrip.scrollToPosition(capturedUris.size - 1)
        refreshMultiModeUi()
    }

    private fun refreshMultiModeUi() {
        val count = capturedUris.size
        thumbStrip.visibility = if (count > 0) View.VISIBLE else View.GONE
        countText.visibility = if (count > 0) View.VISIBLE else View.GONE
        countText.text = if (count == 1) "1 photo captured" else "$count photos captured"
        doneBtn.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun animateShutterPress() {
        shutterButton.animate().scaleX(0.82f).scaleY(0.82f).setDuration(80).withEndAction {
            shutterButton.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun flashScreen() {
        captureFlashOverlay.alpha = 0.85f
        ObjectAnimator.ofFloat(captureFlashOverlay, "alpha", 0.85f, 0f).apply {
            duration = 180
            start()
        }
    }

    private fun handleClose() {
        if (mode == MODE_MULTIPLE && capturedUris.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Discard photos?")
                .setMessage("You've captured ${capturedUris.size} photo(s). Leaving now will discard them.")
                .setPositiveButton("Discard") { _, _ ->
                    capturedUris.forEach { uri -> try { contentResolver.delete(uri, null, null) } catch (e: Exception) { } }
                    setResult(RESULT_CANCELED)
                    finish()
                }
                .setNegativeButton("Keep Shooting", null)
                .show()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun finishWithResult() {
        if (capturedUris.isEmpty()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val resultIntent = android.content.Intent()
        resultIntent.putParcelableArrayListExtra(EXTRA_RESULT_URIS, ArrayList(capturedUris))
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        handleClose()
    }

    private inner class ThumbAdapter(
        private val uris: MutableList<Uri>,
        private val onRemove: (Uri) -> Unit
    ) : RecyclerView.Adapter<ThumbAdapter.ThumbVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_camera_thumb, parent, false)
            return ThumbVH(v)
        }

        override fun onBindViewHolder(holder: ThumbVH, position: Int) {
            val uri = uris[position]
            holder.image.setImageURI(uri)
            holder.remove.setOnClickListener { onRemove(uri) }
        }

        override fun getItemCount() = uris.size

        inner class ThumbVH(v: View) : RecyclerView.ViewHolder(v) {
            val image: ImageView = v.findViewById(R.id.thumbImage)
            val remove: TextView = v.findViewById(R.id.thumbRemoveBtn)
        }
    }
}
