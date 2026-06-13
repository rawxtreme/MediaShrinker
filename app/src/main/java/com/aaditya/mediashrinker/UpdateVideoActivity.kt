package com.aaditya.mediashrinker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class UpdateVideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_video)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val enterButton = findViewById<Button>(R.id.enterButton)
        val skipText = findViewById<TextView>(R.id.skipText)
        val appLogoOverlay = findViewById<ImageView>(R.id.appLogoOverlay)
        val appNameOverlay = findViewById<TextView>(R.id.appNameOverlay)
        val taglineOverlay = findViewById<TextView>(R.id.taglineOverlay)
        val bottomCard = findViewById<View>(R.id.bottomCard)

        val videoUri = "android.resource://$packageName/${R.raw.startup_video}"
        videoView.setVideoPath(videoUri)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoView.start()

            // Fade in bottom card after 800ms
            bottomCard.postDelayed({
                val fadeIn = AlphaAnimation(0f, 1f)
                fadeIn.duration = 600
                fadeIn.fillAfter = true
                bottomCard.startAnimation(fadeIn)
                bottomCard.visibility = View.VISIBLE
            }, 800)

            // Fade in overlay elements
            listOf(appLogoOverlay, appNameOverlay, taglineOverlay).forEach { view ->
                view.postDelayed({
                    val fadeIn = AlphaAnimation(0f, 1f)
                    fadeIn.duration = 800
                    fadeIn.fillAfter = true
                    view.startAnimation(fadeIn)
                    view.visibility = View.VISIBLE
                }, 400)
            }
        }

        enterButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        skipText.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        val videoView = findViewById<VideoView>(R.id.videoView)
        videoView?.pause()
    }

    override fun onResume() {
        super.onResume()
        val videoView = findViewById<VideoView>(R.id.videoView)
        videoView?.start()
    }
}