package com.aaditya.mediashrinker

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StorageAnalyticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_analytics)
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        loadStats()
    }

    private fun loadStats() {
        val prefs = getSharedPreferences("MediaShrinkerHistory", MODE_PRIVATE)
        val historySet = prefs.getStringSet("history", emptySet()) ?: emptySet()

        var totalOriginalKB = 0L
        var totalCompressedKB = 0L
        val totalImages = historySet.size

        for (item in historySet) {
            val parts = item.split("|")
            if (parts.size >= 4) {
                val orig = parts[1].replace(" KB", "").trim().toLongOrNull() ?: 0L
                val comp = parts[2].replace(" KB", "").trim().toLongOrNull() ?: 0L
                totalOriginalKB += orig
                totalCompressedKB += comp
            }
        }

        val totalSavedKB = totalOriginalKB - totalCompressedKB
        val avgReduction = if (totalOriginalKB > 0) ((totalSavedKB * 100) / totalOriginalKB).toInt() else 0

        val savedMB = if (totalSavedKB > 1024) String.format("%.1f MB", totalSavedKB / 1024f) else "$totalSavedKB KB"
        val origMB = if (totalOriginalKB > 1024) String.format("%.1f MB", totalOriginalKB / 1024f) else "$totalOriginalKB KB"
        val compMB = if (totalCompressedKB > 1024) String.format("%.1f MB", totalCompressedKB / 1024f) else "$totalCompressedKB KB"

        findViewById<TextView>(R.id.totalImagesValue).text = "$totalImages"
        findViewById<TextView>(R.id.totalSavedValue).text = savedMB
        findViewById<TextView>(R.id.totalOriginalValue).text = origMB
        findViewById<TextView>(R.id.totalCompressedValue).text = compMB
        findViewById<TextView>(R.id.avgReductionValue).text = "$avgReduction%"

        val progressFill = findViewById<android.view.View>(R.id.savingsProgressFill)
        val progressBar = findViewById<FrameLayout>(R.id.savingsProgressBar)
        progressBar.post {
            val fillWidth = (progressBar.width * avgReduction / 100)
            val params = progressFill.layoutParams
            params.width = fillWidth.coerceAtLeast(0)
            progressFill.layoutParams = params
        }

        findViewById<TextView>(R.id.motivText).text = when {
            totalImages == 0 -> "Start compressing images to see your stats here!"
            totalSavedKB > 102400 -> "You have saved over 100MB! Incredible work!"
            totalSavedKB > 51200 -> "Over 50MB saved. You are a pro!"
            totalSavedKB > 10240 -> "Over 10MB saved. Keep going!"
            else -> "Great start! Every KB counts."
        }
    }
}