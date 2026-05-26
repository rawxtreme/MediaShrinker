package com.aaditya.mediashrinker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_about
        )

        val instagramButton =
            findViewById<Button>(
                R.id.instagramButton
            )

        val githubButton =
            findViewById<Button>(
                R.id.githubButton
            )

        instagramButton.setOnClickListener {

            startActivity(

                Intent(

                    Intent.ACTION_VIEW,

                    Uri.parse(
                        "https://www.instagram.com/carryon.aditya"
                    )
                )
            )
        }

        githubButton.setOnClickListener {

            startActivity(

                Intent(

                    Intent.ACTION_VIEW,

                    Uri.parse(
                        "https://github.com/rawxtreme"
                    )
                )
            )
        }
    }
}