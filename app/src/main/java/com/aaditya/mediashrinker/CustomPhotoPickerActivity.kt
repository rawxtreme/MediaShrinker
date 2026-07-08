package com.aaditya.mediashrinker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CustomPhotoPickerActivity : AppCompatActivity() {

    private lateinit var pickerRecyclerView: RecyclerView
    private lateinit var selectedCountText: TextView
    private lateinit var doneButton: TextView
    private lateinit var backButton: TextView

    private lateinit var adapter: PhotoPickerAdapter
    private val allPhotoUris = mutableListOf<Uri>()

    private val maxSelection = 100
    private val permissionRequestCode = 501

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_photo_picker)

        pickerRecyclerView = findViewById(R.id.pickerRecyclerView)
        selectedCountText = findViewById(R.id.selectedCountText)
        doneButton = findViewById(R.id.doneButton)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { finish() }

        doneButton.setOnClickListener {
            confirmSelectionAndFinish()
        }

        if (hasPermission()) {
            loadPhotosFromDevice()
        } else {
            requestPermission()
        }
    }

    // Android 13 (Tiramisu / API 33) introduced a separate photo-only permission.
    // Below that, apps used the broader READ_EXTERNAL_STORAGE permission.
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

        ActivityCompat.requestPermissions(this, arrayOf(permission), permissionRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadPhotosFromDevice()
            } else {
                showPermissionRequiredDialog()
            }
        }
    }

    // If the user goes to Settings and grants permission manually, then presses back,
    // this screen resumes — so we recheck permission here and load photos automatically.
    // The !::adapter.isInitialized check makes sure we don't reload if already loaded.
    override fun onResume() {
        super.onResume()
        if (hasPermission() && !::adapter.isInitialized) {
            loadPhotosFromDevice()
        }
    }

    private fun showPermissionRequiredDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permission_required, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.permissionCancelBtn).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialogView.findViewById<Button>(R.id.permissionOpenSettingsBtn).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }

        dialog.show()
    }

    // Queries MediaStore directly to get every image on the device,
    // newest first, without opening any other app.
    private fun loadPhotosFromDevice() {
        allPhotoUris.clear()

        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                allPhotoUris.add(uri)
            }
        }

        pickerRecyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoPickerAdapter(allPhotoUris, maxSelection) { selectedCount ->
            updateSelectedCountText(selectedCount)
        }
        pickerRecyclerView.adapter = adapter

        updateSelectedCountText(0)
    }

    private fun updateSelectedCountText(count: Int) {
        selectedCountText.text = "$count / $maxSelection Selected"
    }

    private fun confirmSelectionAndFinish() {
        val selectedUris = adapter.getSelectedUris()

        if (selectedUris.isEmpty()) {
            Toast.makeText(this, "Select at least 1 photo", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent()
        resultIntent.putParcelableArrayListExtra("selected_uris", ArrayList(selectedUris))
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
