package com.notireader.app.domain.services

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri

object RateUs {

    fun rate(context: Context) {
        try {
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                data = "market://details?id=${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (playStoreIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(playStoreIntent)
                Log.d("xyz", "Opened app rating in Play Store app")
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Log.d("xyz", "Opened app rating in browser")
            }

        } catch (e: Exception) {
            Log.e("xyz", "Error opening rate us: ${e.message}")
            Toast.makeText(context, "Developer's message: You got us there, this feature is under development...!!", Toast.LENGTH_SHORT).show()
        }
    }
}