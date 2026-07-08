package com.aaditya.mediashrinker

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HowToUseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_to_use)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val newFeaturesBadge = findViewById<TextView>(R.id.newFeaturesBadge)
        val blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink)
        newFeaturesBadge.startAnimation(blinkAnimation)
    }
}