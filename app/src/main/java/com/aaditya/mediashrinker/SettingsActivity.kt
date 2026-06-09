package com.aaditya.mediashrinker

import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        val startupSwitch =
            findViewById<Switch>(R.id.startupVideoSwitch)

        val prefs =
            getSharedPreferences(
                "MediaShrinkerSettings",
                MODE_PRIVATE
            )

        startupSwitch.isChecked =
            prefs.getBoolean(
                "show_startup_video",
                true
            )

        startupSwitch.setOnCheckedChangeListener { _, isChecked ->

            prefs.edit()
                .putBoolean(
                    "show_startup_video",
                    isChecked
                )
                .apply()
        }
    }
}