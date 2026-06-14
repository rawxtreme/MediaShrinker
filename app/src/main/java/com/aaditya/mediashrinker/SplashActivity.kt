package com.aaditya.mediashrinker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<View>(R.id.splashLogo)
        val glowRing = findViewById<View>(R.id.glowRing)
        val appName = findViewById<View>(R.id.splashAppName)
        val tagline = findViewById<View>(R.id.splashTagline)
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)
        val versionLabel = findViewById<View>(R.id.splashVersion)

        // Start everything invisible except logo
        appName.alpha = 0f
        tagline.alpha = 0f
        versionLabel.alpha = 0f

        // Logo scale-in + glow pulse
        val logoScaleIn = ScaleAnimation(
            0.5f, 1f, 0.5f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
            fillAfter = true
        }
        val logoFadeIn = AlphaAnimation(0f, 1f).apply { duration = 500; fillAfter = true }
        val logoAnimSet = AnimationSet(false).apply {
            addAnimation(logoScaleIn)
            addAnimation(logoFadeIn)
        }
        logo.startAnimation(logoAnimSet)

        // Glow ring pulse (continuous)
        startGlowPulse(glowRing)

        // App name fade in (delayed)
        appName.postDelayed({
            val fade = AlphaAnimation(0f, 1f).apply { duration = 400; fillAfter = true }
            appName.startAnimation(fade)
            appName.alpha = 1f
        }, 400)

        // Tagline fade in (delayed more)
        tagline.postDelayed({
            val fade = AlphaAnimation(0f, 1f).apply { duration = 400; fillAfter = true }
            tagline.startAnimation(fade)
            tagline.alpha = 1f
        }, 600)

        // Version label fade in
        versionLabel.postDelayed({
            val fade = AlphaAnimation(0f, 0.6f).apply { duration = 400; fillAfter = true }
            versionLabel.startAnimation(fade)
            versionLabel.alpha = 0.6f
        }, 800)

        // Start dot loading animation
        startDotAnimation(dot1, dot2, dot3)

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 1800)
    }

    private fun startGlowPulse(view: View) {
        val pulseOut = ScaleAnimation(
            1f, 1.15f, 1f, 1.15f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            fillAfter = true
        }
        val pulseIn = ScaleAnimation(
            1.15f, 1f, 1.15f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            startOffset = 800
            fillAfter = true
        }
        val pulseSet = AnimationSet(false).apply {
            addAnimation(pulseOut)
            addAnimation(pulseIn)
            repeatMode = Animation.RESTART
        }
        view.startAnimation(pulseSet)

        pulseSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                if (!isFinishing) startGlowPulse(view)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private fun startDotAnimation(d1: View, d2: View, d3: View) {
        animateDot(d1, 0)
        animateDot(d2, 200)
        animateDot(d3, 400)
    }

    private fun animateDot(dot: View, delay: Long) {
        val bounceUp = AlphaAnimation(0.3f, 1f).apply {
            duration = 400
            startOffset = delay
            fillAfter = true
        }
        val bounceDown = AlphaAnimation(1f, 0.3f).apply {
            duration = 400
            startOffset = delay + 400
            fillAfter = true
        }
        val set = AnimationSet(false).apply {
            addAnimation(bounceUp)
            addAnimation(bounceDown)
        }
        dot.startAnimation(set)

        set.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                if (!isFinishing) animateDot(dot, 0)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private fun navigateNext() {
        val prefs = getSharedPreferences("MediaShrinkerSettings", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_done", false)
        val showVideo = prefs.getBoolean("show_startup_video", true)

        val nextActivity = when {
            !onboardingDone -> OnboardingActivity::class.java
            showVideo -> UpdateVideoActivity::class.java
            else -> MainActivity::class.java
        }

        startActivity(Intent(this, nextActivity))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}