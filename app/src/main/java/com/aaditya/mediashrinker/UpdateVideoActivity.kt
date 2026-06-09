package com.aaditya.mediashrinker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class UpdateVideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_update_video)

        val videoView =
            findViewById<VideoView>(R.id.videoView)

        val enterButton =
            findViewById<Button>(R.id.enterButton)

        val videoUri =
            "android.resource://$packageName/${R.raw.startup_video}"

        videoView.setVideoPath(videoUri)

        videoView.setOnPreparedListener {
            it.isLooping = true
            videoView.start()
        }

        enterButton.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
            )

            finish()
        }
    }
}