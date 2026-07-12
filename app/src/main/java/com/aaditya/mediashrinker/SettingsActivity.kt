package com.aaditya.mediashrinker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val startupVideoSwitch = findViewById<SwitchCompat>(R.id.startupVideoSwitch)
        val privacyPolicyRow = findViewById<LinearLayout>(R.id.privacyPolicyRow)
        val howToUseRow = findViewById<LinearLayout>(R.id.howToUseRow)
        val checkUpdatesRow = findViewById<LinearLayout>(R.id.checkUpdatesRow)
        val updateStatusText = findViewById<TextView>(R.id.updateStatusText)
        val updateStatusIcon = findViewById<ImageView>(R.id.updateStatusIcon)
        val updateStatusLayout = findViewById<LinearLayout>(R.id.updateStatusLayout)

        val prefs = getSharedPreferences("MediaShrinkerSettings", MODE_PRIVATE)

        // --- Back Button ---
        backButton.setOnClickListener { finish() }

        // --- Startup Video Toggle ---
        startupVideoSwitch.isChecked = prefs.getBoolean("show_startup_video", true)
        startupVideoSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_startup_video", isChecked).apply()
        }

        // --- Privacy Policy ---
        privacyPolicyRow.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        // --- How to Use ---
        howToUseRow.setOnClickListener {
            startActivity(Intent(this, HowToUseActivity::class.java))
        }

        // --- Check for Updates ---
        checkUpdatesRow.setOnClickListener {
            checkForUpdates(updateStatusLayout, updateStatusText, updateStatusIcon)
        }
    }

    private fun checkForUpdates(
        statusLayout: LinearLayout,
        statusText: TextView,
        statusIcon: ImageView
    ) {
        // Check internet first
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetworkInfo

        if (network == null || !network.isConnected) {
            // No internet
            statusLayout.visibility = View.VISIBLE
            statusLayout.setBackgroundResource(R.drawable.update_status_bg_error)
            statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            statusText.text = "No internet connection.\nPlease check your Wi-Fi or mobile data and try again."
            return
        }

        // Show checking state
        statusLayout.visibility = View.VISIBLE
        statusLayout.setBackgroundResource(R.drawable.update_status_bg_checking)
        statusIcon.setImageResource(android.R.drawable.ic_popup_sync)
        statusText.text = "Checking for updates..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/rawxtreme/MediaShrinker/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(response)
                    val latestTag = json.getString("tag_name").trimStart('v', 'V')
                    val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName
                    // Points directly at the APK file inside the latest release —
                    // GitHub always resolves this to whatever asset is currently
                    // attached to the newest release, as long as the filename
                    // stays "MediaShrinker.apk" on every upload. No release page,
                    // no scrolling, no Assets section — tap and it downloads.
                    val directDownloadUrl = "https://github.com/rawxtreme/MediaShrinker/releases/latest/download/MediaShrinker.apk"

                    withContext(Dispatchers.Main) {
                        val latestParts = latestTag.split(".").map { it.toIntOrNull() ?: 0 }
                        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

                        var updateAvailable = false
                        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
                            val l = latestParts.getOrElse(i) { 0 }
                            val c = currentParts.getOrElse(i) { 0 }
                            if (l > c) { updateAvailable = true; break }
                            if (l < c) { break }
                        }

                        if (updateAvailable) {
                            statusLayout.setBackgroundResource(R.drawable.update_status_bg_update)
                            statusIcon.setImageResource(android.R.drawable.stat_sys_download)
                            statusText.text = "Update available! v$latestTag is out.\nTap here to download."
                            statusLayout.setOnClickListener {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(directDownloadUrl)))
                            }
                        } else {
                            statusLayout.setBackgroundResource(R.drawable.update_status_bg_ok)
                            statusIcon.setImageResource(android.R.drawable.checkbox_on_background)
                            statusText.text = "You're on the latest version (v$currentVersion)."
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        statusLayout.setBackgroundResource(R.drawable.update_status_bg_error)
                        statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                        statusText.text = "Could not reach GitHub. Try again later."
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusLayout.setBackgroundResource(R.drawable.update_status_bg_error)
                    statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    statusText.text = "Failed to connect with server.\nPlease check your Wi-Fi or connect to internet."
                }
            }
        }
    }
}