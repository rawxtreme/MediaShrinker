package com.aaditya.mediashrinker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    private lateinit var splashOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val backButton = findViewById<ImageView>(R.id.aboutBackButton)
        val instagramButton = findViewById<Button>(R.id.instagramButton)
        val githubButton = findViewById<Button>(R.id.githubButton)
        splashOverlay = findViewById(R.id.splashOverlay)

        backButton.setOnClickListener { finish() }

        // Apply professional squeeze & splash animations
        attachSplashAndSqueeze(instagramButton)
        attachSplashAndSqueeze(githubButton)

        instagramButton.setOnClickListener {
            openUrl("https://www.instagram.com/carryon.aditya")
        }

        githubButton.setOnClickListener {
            openUrl("https://github.com/rawxtreme")
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
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
}
