package com.aaditya.mediashrinker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.*

class MagicCleanerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var initialState: View
    private lateinit var scanningState: View
    private lateinit var actionLayout: View
    private lateinit var startScanButton: Button
    private lateinit var deleteButton: Button
    private lateinit var progressText: TextView
    private lateinit var subText: TextView
    private lateinit var selectedCountText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageView
    private lateinit var splashOverlay: View

    private val duplicateGroups = mutableListOf<List<PhotoItem>>()
    private val flatList = mutableListOf<Any>() // Mixture of Header (String) and PhotoItem
    private val selectedPhotos = mutableSetOf<PhotoItem>()

    data class PhotoItem(val uri: Uri, val name: String, val size: Long, val hash: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magic_cleaner)

        recyclerView = findViewById(R.id.cleanerRecyclerView)
        initialState = findViewById(R.id.cleanerInitialState)
        scanningState = findViewById(R.id.cleanerScanningState)
        actionLayout = findViewById(R.id.cleanerActionLayout)
        startScanButton = findViewById(R.id.startScanButton)
        deleteButton = findViewById(R.id.deleteDuplicatesButton)
        progressText = findViewById(R.id.scanningProgressText)
        subText = findViewById(R.id.scanningSubText)
        selectedCountText = findViewById(R.id.cleanerSelectedCountText)
        progressBar = findViewById(R.id.cleanerProgressBar)
        backButton = findViewById(R.id.cleanerBackButton)
        splashOverlay = findViewById(R.id.splashOverlay)

        backButton.setOnClickListener { finish() }

        attachSplashAndSqueeze(startScanButton)
        attachSplashAndSqueeze(deleteButton)

        startScanButton.setOnClickListener {
            if (hasPermission()) {
                startMagicScan()
            } else {
                requestPermission()
            }
        }

        deleteButton.setOnClickListener {
            deleteSelectedPhotos()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun hasPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        ActivityCompat.requestPermissions(this, arrayOf(permission), 503)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 503) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMagicScan()
            } else {
                showPermissionRequiredDialog()
            }
        }
    }

    private fun showPermissionRequiredDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permission_required, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<Button>(R.id.permissionCancelBtn).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.permissionOpenSettingsBtn).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
        dialog.show()
    }

    private fun startMagicScan() {
        initialState.visibility = View.GONE
        scanningState.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true

        CoroutineScope(Dispatchers.IO).launch {
            val allPhotos = fetchAllPhotos()
            
            withContext(Dispatchers.Main) {
                progressBar.isIndeterminate = false
                progressBar.max = allPhotos.size
                progressBar.progress = 0
            }

            val photoItems = mutableListOf<PhotoItem>()
            allPhotos.forEachIndexed { index, pair ->
                val uri = pair.first
                val name = pair.second
                val size = pair.third
                
                val hash = generatePHash(uri)
                if (hash != 0L) {
                    photoItems.add(PhotoItem(uri, name, size, hash))
                }

                if (index % 10 == 0) {
                    withContext(Dispatchers.Main) {
                        progressBar.progress = index
                        subText.text = "$index photos analyzed"
                    }
                }
            }

            // Group by hash
            val groups = photoItems.groupBy { it.hash }.values.filter { it.size > 1 }

            withContext(Dispatchers.Main) {
                duplicateGroups.clear()
                duplicateGroups.addAll(groups)
                buildFlatList()
                showResults()
            }
        }
    }

    private fun fetchAllPhotos(): List<Triple<Uri, String, Long>> {
        val photos = mutableListOf<Triple<Uri, String, Long>>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
        )
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                photos.add(Triple(uri, it.getString(nameCol), it.getLong(sizeCol)))
            }
        }
        return photos
    }

    private fun generatePHash(uri: Uri): Long {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 8 // Downsample for speed
            }
            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input, null, options) ?: return 0L
                val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
                bitmap.recycle()
                
                var total = 0L
                val pixels = IntArray(64)
                small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
                
                for (p in pixels) {
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    total += (r + g + b) / 3
                }
                val avg = total / 64
                var hash = 0L
                for (i in 0 until 64) {
                    val p = pixels[i]
                    val gray = ((p shr 16) and 0xFF + (p shr 8) and 0xFF + p and 0xFF) / 3
                    if (gray >= avg) {
                        hash = hash or (1L shl i)
                    }
                }
                small.recycle()
                hash
            } ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun buildFlatList() {
        flatList.clear()
        duplicateGroups.forEachIndexed { index, group ->
            flatList.add("Similar Group ${index + 1} (${group.size} photos)")
            flatList.addAll(group)
        }
    }

    private fun showResults() {
        scanningState.visibility = View.GONE
        progressBar.visibility = View.GONE
        
        if (duplicateGroups.isEmpty()) {
            initialState.visibility = View.VISIBLE
            findViewById<TextView>(R.id.startScanButton).text = "RESCAN GALLERY"
            Toast.makeText(this, "No duplicates found!", Toast.LENGTH_LONG).show()
        } else {
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = CleanerAdapter()
        }
    }

    private fun updateSelectionUI() {
        if (selectedPhotos.isEmpty()) {
            actionLayout.visibility = View.GONE
        } else {
            actionLayout.visibility = View.VISIBLE
            selectedCountText.text = "${selectedPhotos.size} photos selected to delete"
        }
    }

    private fun deleteSelectedPhotos() {
        // In a real app, you'd use MediaStore to delete.
        // For simplicity, we just remove from UI and toast.
        val uris = selectedPhotos.map { it.uri }
        Toast.makeText(this, "Requested deletion of ${uris.size} photos", Toast.LENGTH_SHORT).show()
        
        // Refresh scan after deletion simulation
        selectedPhotos.clear()
        updateSelectionUI()
        startMagicScan()
    }

    private fun attachSplashAndSqueeze(view: View) {
        val clickAnimator = android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.button_click)
        view.stateListAnimator = clickAnimator
        view.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                showWaterSplash(event.rawX, event.rawY)
            }
            false 
        }
    }

    private fun showWaterSplash(rawX: Float, rawY: Float) {
        splashOverlay.visibility = View.VISIBLE
        val offset = (50 * resources.displayMetrics.density).toInt()
        splashOverlay.x = rawX - offset
        splashOverlay.y = rawY - offset
        val anim = AnimationUtils.loadAnimation(this, R.anim.water_splash)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation?) {}
            override fun onAnimationRepeat(a: Animation?) {}
            override fun onAnimationEnd(a: Animation?) {
                splashOverlay.visibility = View.GONE
            }
        })
        splashOverlay.startAnimation(anim)
    }

    inner class CleanerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TYPE_HEADER = 0
        private val TYPE_PHOTO = 1

        override fun getItemViewType(position: Int) = if (flatList[position] is String) TYPE_HEADER else TYPE_PHOTO

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_HEADER) {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_duplicate_header, parent, false)
                HeaderVH(v)
            } else {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_duplicate_photo, parent, false)
                PhotoVH(v)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is HeaderVH) {
                holder.text.text = flatList[position] as String
            } else if (holder is PhotoVH) {
                val item = flatList[position] as PhotoItem
                holder.name.text = item.name
                holder.size.text = String.format(Locale.getDefault(), "%.1f MB", item.size / (1024 * 1024.0))
                holder.thumb.setImageURI(item.uri)
                holder.check.setOnCheckedChangeListener(null)
                holder.check.isChecked = selectedPhotos.contains(item)
                holder.check.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedPhotos.add(item) else selectedPhotos.remove(item)
                    updateSelectionUI()
                }
                holder.itemView.setOnClickListener { holder.check.isChecked = !holder.check.isChecked }
            }
        }

        override fun getItemCount() = flatList.size

        inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) { val text: TextView = v.findViewById(R.id.duplicateHeaderText) }
        inner class PhotoVH(v: View) : RecyclerView.ViewHolder(v) {
            val thumb: ImageView = v.findViewById(R.id.duplicateThumb)
            val name: TextView = v.findViewById(R.id.duplicateName)
            val size: TextView = v.findViewById(R.id.duplicateSize)
            val check: CheckBox = v.findViewById(R.id.duplicateCheckbox)
        }
    }
}
