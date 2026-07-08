package com.aaditya.mediashrinker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhotoPreviewActivity : AppCompatActivity() {

    private lateinit var previewRecyclerView: RecyclerView
    private lateinit var previewCountText: TextView
    private lateinit var continueToHomeButton: Button

    private lateinit var photoList: MutableList<Uri>
    private lateinit var adapter: PhotoPreviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_preview)

        previewRecyclerView = findViewById(R.id.previewRecyclerView)
        previewCountText = findViewById(R.id.previewCountText)
        continueToHomeButton = findViewById(R.id.continueToHomeButton)

        val receivedUris: ArrayList<Uri> = intent.getParcelableArrayListExtra("photo_uris") ?: arrayListOf()
        photoList = receivedUris.toMutableList()

        previewRecyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoPreviewAdapter(photoList) { updateCountText() }
        previewRecyclerView.adapter = adapter

        updateCountText()

        continueToHomeButton.setOnClickListener {
            returnResultAndFinish()
        }
    }

    private fun updateCountText() {
        previewCountText.text = if (photoList.isEmpty())
            "No photos left. Tap continue to go back."
        else
            "${photoList.size} photo(s) selected. Tap the ✕ on a photo to remove it"
    }

    private fun returnResultAndFinish() {
        val resultIntent = Intent()
        resultIntent.putParcelableArrayListExtra("updated_uris", ArrayList(photoList))
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        returnResultAndFinish()
        super.onBackPressed()
    }
}
