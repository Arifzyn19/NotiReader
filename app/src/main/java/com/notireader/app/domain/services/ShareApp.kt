package com.notireader.app.domain.services

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.notireader.app.R

object ShareApp {

    fun share(context: Context) {
        try {
            val appName = context.getString(R.string.app_name)
            val packageName = context.packageName
            val playStoreLink = "https://play.google.com/store/apps/details?id=$packageName"

            val shareText = """
                🔥 Check out this amazing app: $appName! 
                
                📱 Recover your deleted WhatsApp messages and media files effortlessly!
                
                ✨ Features:
                • Recover deleted messages
                • Save WhatsApp media automatically  
                • Works with both personal and business WhatsApp
                • Clean and easy interface
                
                Download now: $playStoreLink
                
                #WhatsAppRecovery #MessageRecovery #DataRecovery
            """.trimIndent()

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Check out $appName")
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share $appName via"))
        } catch (e: Exception) {
            Log.e("xyz", "Error sharing app: ${e.message}")
            Toast.makeText(context, "Unable to share app", Toast.LENGTH_SHORT).show()
        }
    }
}
