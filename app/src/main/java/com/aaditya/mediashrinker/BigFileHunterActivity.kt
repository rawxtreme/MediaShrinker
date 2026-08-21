package com.aaditya.mediashrinker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class BigFileHunterActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var actionLayout: View
    private lateinit var totalText: TextView
    private lateinit var shrinkButton: Button
    private lateinit var backButton: ImageView
    private lateinit var thresholdInput: android.widget.EditText
    private lateinit var searchButton: Button
    private lateinit var emptyState: View
    private lateinit var emptyText: TextView
    private lateinit var fallbackButton: Button
    private lateinit var loadingProgress: ProgressBar

    private val bigFiles = mutableListOf<BigFileItem>()
    private val selectedFiles = mutableSetOf<BigFileItem>()
    private var currentThresholdMB = 5

    data class BigFileItem(val uri: Uri, val name: String, val size: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_big_file_hunter)

        recyclerView = findViewById(R.id.hunterRecyclerView)
        actionLayout = findViewById(R.id.hunterActionLayout)
        totalText = findViewById(R.id.hunterTotalSelectedText)
        shrinkButton = findViewById(R.id.hunterShrinkButton)
        backButton = findViewById(R.id.hunterBackButton)
        thresholdInput = findViewById(R.id.hunterThresholdInput)
        searchButton = findViewById(R.id.hunterSearchBtn)
        emptyState = findViewById(R.id.hunterEmptyState)
        emptyText = findViewById(R.id.hunterEmptyText)
        fallbackButton = findViewById(R.id.hunterFallbackBtn)
        loadingProgress = findViewById(R.id.hunterLoadingProgress)

        backButton.setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)

        if (hasPermission()) {
            scanBigFiles(5)
        } else {
            requestPermission()
        }

        searchButton.setOnClickListener {
            val input = thresholdInput.text.toString().toIntOrNull()
            if (input == null || input < 1) {
                Toast.makeText(this, "Enter at least 1MB", Toast.LENGTH_SHORT).show()
            } else {
                currentThresholdMB = input
                scanBigFiles(input)
            }
        }

        fallbackButton.setOnClickListener {
            thresholdInput.setText("5")
            scanBigFiles(5)
        }

        shrinkButton.stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.button_click)

        attachSplashAndSqueeze(shrinkButton)
        attachSplashAndSqueeze(searchButton)
        attachSplashAndSqueeze(fallbackButton)

        shrinkButton.setOnClickListener {
            val uris = selectedFiles.map { it.uri }
            val intent = Intent(this, MainActivity::class.java).apply {
                putParcelableArrayListExtra("hunter_uris", ArrayList(uris))
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun hasPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 502) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanBigFiles(5)
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
        dialogView.findViewById<Button>(R.id.permissionCancelBtn).setOnClickListener { dialog.dismiss(); finish() }
        dialogView.findViewById<Button>(R.id.permissionOpenSettingsBtn).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
        dialog.show()
    }

    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        ActivityCompat.requestPermissions(this, arrayOf(permission), 502)
    }

    private fun scanBigFiles(thresholdMB: Int) {
        bigFiles.clear()
        selectedFiles.clear()
        updateSelectionUI()
        
        loadingProgress.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            val thresholdBytes = thresholdMB * 1024L * 1024L
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
            )
            val selection = "${MediaStore.Images.Media.SIZE} > ?"
            val selectionArgs = arrayOf(thresholdBytes.toString())
            val sortOrder = "${MediaStore.Images.Media.SIZE} DESC"

            val cursor: Cursor? = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            val tempFiles = mutableListOf<BigFileItem>()
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol)
                    val size = it.getLong(sizeCol)
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    tempFiles.add(BigFileItem(uri, name, size))
                }
            }

            withContext(Dispatchers.Main) {
                loadingProgress.visibility = View.GONE
                bigFiles.addAll(tempFiles)
                
                if (bigFiles.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    emptyText.text = "No photos larger than ${thresholdMB}MB found in your gallery.\nTry a smaller size or let us help you."
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                    recyclerView.adapter = HunterAdapter()
                }
            }
        }
    }

    private fun updateSelectionUI() {
        if (selectedFiles.isEmpty()) {
            actionLayout.visibility = View.GONE
        } else {
            actionLayout.visibility = View.VISIBLE
            val totalSize = selectedFiles.sumOf { it.size }
            val sizeMB = totalSize / (1024 * 1024.0)
            totalText.text = String.format(Locale.getDefault(), "%d files selected (%.1f MB)", selectedFiles.size, sizeMB)
        }
    }

    inner class HunterAdapter : RecyclerView.Adapter<HunterAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val thumb: ImageView = v.findViewById(R.id.bigFileThumb)
            val name: TextView = v.findViewById(R.id.bigFileName)
            val size: TextView = v.findViewById(R.id.bigFileSize)
            val check: CheckBox = v.findViewById(R.id.bigFileCheckbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_big_file, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = bigFiles[position]
            holder.name.text = item.name
            holder.size.text = String.format(Locale.getDefault(), "%.1f MB", item.size / (1024 * 1024.0))
            holder.thumb.setImageURI(item.uri)
            
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = selectedFiles.contains(item)
            
            holder.check.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedFiles.add(item) else selectedFiles.remove(item)
                updateSelectionUI()
            }
            
            holder.itemView.setOnClickListener {
                holder.check.isChecked = !holder.check.isChecked
            }
        }

        override fun getItemCount() = bigFiles.size
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
        val splashOverlay = findViewById<View>(R.id.splashOverlay) ?: return
        splashOverlay.visibility = View.VISIBLE
        val offset = (50 * resources.displayMetrics.density).toInt()
        splashOverlay.x = rawX - offset
        splashOverlay.y = rawY - offset
        val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.water_splash)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(a: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            override fun onAnimationEnd(a: android.view.animation.Animation?) {
                splashOverlay.visibility = View.GONE
            }
        })
        splashOverlay.startAnimation(anim)
    }
}
