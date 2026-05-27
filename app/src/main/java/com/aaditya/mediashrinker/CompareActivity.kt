package com.aaditya.mediashrinker

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CompareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_compare)

        val beforeImage =
            findViewById<ImageView>(R.id.beforeImage)

        val afterImage =
            findViewById<ImageView>(R.id.afterImage)

        val beforeUri =
            intent.getStringExtra("before")

        val afterUri =
            intent.getStringExtra("after")

        if (beforeUri != null) {

            beforeImage.setImageURI(
                Uri.parse(beforeUri)
            )
        }

        if (afterUri != null) {

            afterImage.setImageURI(
                Uri.parse(afterUri)
            )
        }
    }
}