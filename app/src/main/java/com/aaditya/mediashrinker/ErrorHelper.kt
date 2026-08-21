package com.aaditya.mediashrinker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

object ErrorHelper {

    fun showFileMissingDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_file_missing, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView).setCancelable(true).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.reportProblemBtn).setOnClickListener {
            dialog.dismiss()
            showReportTroubleDialog(context)
        }
        dialogView.findViewById<Button>(R.id.missingCancelBtn).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showReportTroubleDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report_trouble, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView).setCancelable(true).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val input = dialogView.findViewById<EditText>(R.id.reportErrorInput)

        dialogView.findViewById<Button>(R.id.reportCancelBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.reportSendBtn).setOnClickListener {
            val errorMsg = input.text.toString().trim()
            if (errorMsg.isEmpty()) {
                input.error = "Please describe the error"
                return@setOnClickListener
            }
            dialog.dismiss()
            sendReportEmail(context, errorMsg)
        }
        dialog.show()
    }

    private fun sendReportEmail(context: Context, userMessage: String) {
        val deviceInfo = """
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            App Version: 10.0
            Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
        """.trimIndent()

        val body = "Error Description:\n$userMessage\n\n---\nTechnical Info:\n$deviceInfo"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("scope8xaditya@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "MediaShrinker - Troubleshooting Report")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    }
}
