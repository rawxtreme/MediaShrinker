package com.aaditya.mediashrinker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhotoPreviewActivity : AppCompatActivity() {

    private lateinit var previewRecyclerView: RecyclerView
    private lateinit var previewCountText: TextView
    private lateinit var continueToHomeButton: Button
    private lateinit var deleteAllBtn: TextView
    private lateinit var deleteSelectionBar: LinearLayout
    private lateinit var deleteSelectionText: TextView
    private lateinit var deleteSelectionYesBtn: TextView
    private lateinit var deleteSelectionNoBtn: TextView

    private lateinit var photoList: MutableList<Uri>
    private lateinit var adapter: PhotoPreviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_preview)

        previewRecyclerView = findViewById(R.id.previewRecyclerView)
        previewCountText = findViewById(R.id.previewCountText)
        continueToHomeButton = findViewById(R.id.continueToHomeButton)
        deleteAllBtn = findViewById(R.id.deleteAllBtn)
        deleteSelectionBar = findViewById(R.id.deleteSelectionBar)
        deleteSelectionText = findViewById(R.id.deleteSelectionText)
        deleteSelectionYesBtn = findViewById(R.id.deleteSelectionYesBtn)
        deleteSelectionNoBtn = findViewById(R.id.deleteSelectionNoBtn)

        val receivedUris: ArrayList<Uri> = intent.getParcelableArrayListExtra("photo_uris") ?: arrayListOf()
        photoList = receivedUris.toMutableList()

        previewRecyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoPreviewAdapter(
            photoList,
            onListChanged = { updateCountText() },
            onMarksChanged = { count -> updateDeleteSelectionBar(count) }
        )
        previewRecyclerView.adapter = adapter

        updateCountText()

        continueToHomeButton.setOnClickListener {
            returnResultAndFinish()
        }

        deleteAllBtn.setOnClickListener {
            if (photoList.isEmpty()) return@setOnClickListener
            showDeleteAllChooser()
        }

        deleteSelectionYesBtn.setOnClickListener {
            adapter.deleteMarkedPhotos()
            exitIndividualDeleteMode()
        }

        deleteSelectionNoBtn.setOnClickListener {
            // "No" just deselects everything and exits the mode — nothing is deleted.
            adapter.clearMarks()
            exitIndividualDeleteMode()
        }
    }

    private fun showDeleteAllChooser() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_all_chooser, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<LinearLayout>(R.id.deleteAllOption).setOnClickListener {
            dialog.dismiss()
            adapter.deleteAllPhotos()
        }

        dialogView.findViewById<LinearLayout>(R.id.deleteIndividuallyOption).setOnClickListener {
            dialog.dismiss()
            enterIndividualDeleteMode()
        }

        dialogView.findViewById<Button>(R.id.deleteAllChooserCancelBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun enterIndividualDeleteMode() {
        adapter.isIndividualDeleteMode = true
        deleteSelectionBar.visibility = View.VISIBLE
        updateDeleteSelectionBar(0)
    }

    private fun exitIndividualDeleteMode() {
        adapter.isIndividualDeleteMode = false
        deleteSelectionBar.visibility = View.GONE
        updateCountText()
    }

    private fun updateDeleteSelectionBar(markedCount: Int) {
        deleteSelectionText.text = if (markedCount == 0)
            "Tap photos to select for deletion"
        else
            "Delete $markedCount selected image(s)?"
    }

    private fun updateCountText() {
        previewCountText.text = if (photoList.isEmpty())
            "No photos left. Tap continue to go back."
        else
            "${photoList.size} photo(s) selected. Tap ✕ to remove • 🔍 to enlarge"
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
