package com.aaditya.mediashrinker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        Handler().postDelayed({

            val prefs =
                getSharedPreferences(
                    "MediaShrinkerSettings",
                    MODE_PRIVATE
                )

            val showVideo =
                prefs.getBoolean(
                    "show_startup_video",
                    true
                )

            if (showVideo) {

                startActivity(
                    Intent(
                        this,
                        UpdateVideoActivity::class.java
                    )
                )

            } else {

                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )
            }

            finish()

        }, 2500)
    }
}