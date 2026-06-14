package com.aaditya.mediashrinker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private val pages = listOf(
        Triple("🗜️", "Smart Compression", "Reduce image size without losing quality. Choose Ultra, Balanced, or Max mode — or set a custom target in KB."),
        Triple("📄", "Create PDFs Instantly", "Select multiple photos and combine them into a single PDF in one tap. Saved automatically to your Documents."),
        Triple("🔄", "Convert & Resize", "Convert between PNG, JPG, and WEBP. Resize to standard dimensions. Remove metadata for privacy. All offline."),
        Triple("📊", "Track Your Savings", "MediaShrinker tracks how much space you have saved across all compressions — visible in Storage Analytics.")
    )

    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        updatePage(0)

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            if (currentPage < pages.size - 1) {
                currentPage++
                updatePage(currentPage)
            } else {
                finishOnboarding()
            }
        }

        findViewById<TextView>(R.id.skipText).setOnClickListener {
            finishOnboarding()
        }
    }

    private fun updatePage(index: Int) {
        val (emoji, title, desc) = pages[index]

        val emojiView = findViewById<TextView>(R.id.onboardEmoji)
        val titleView = findViewById<TextView>(R.id.onboardTitle)
        val descView = findViewById<TextView>(R.id.onboardDesc)
        val nextBtn = findViewById<Button>(R.id.nextButton)
        val dotsLayout = findViewById<LinearLayout>(R.id.dotsLayout)

        // Fade animation
        val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 150; fillAfter = true }
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300; startOffset = 150; fillAfter = true }

        emojiView.startAnimation(fadeOut)
        titleView.startAnimation(fadeOut)
        descView.startAnimation(fadeOut)

        emojiView.postDelayed({
            emojiView.text = emoji
            titleView.text = title
            descView.text = desc
            emojiView.startAnimation(fadeIn)
            titleView.startAnimation(fadeIn)
            descView.startAnimation(fadeIn)
        }, 150)

        nextBtn.text = if (index == pages.size - 1) "Get Started" else "Next →"

        // Dots
        dotsLayout.removeAllViews()
        for (i in pages.indices) {
            val dot = TextView(this)
            dot.text = if (i == index) "●" else "○"
            dot.textSize = 14f
            dot.setTextColor(if (i == index) 0xFF3B82F6.toInt() else 0x44FFFFFF.toInt())
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(6, 0, 6, 0)
            dot.layoutParams = params
            dotsLayout.addView(dot)
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("MediaShrinkerSettings", MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}