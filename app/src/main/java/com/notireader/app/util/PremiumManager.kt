package com.notireader.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PremiumManager {
    private const val PREFS_NAME = "premium_prefs"
    private const val KEY_PREMIUM_UNLOCKED = "premium_unlocked"

    fun isPremiumUnlocked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREMIUM_UNLOCKED, false)
    }

    fun setPremiumUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_PREMIUM_UNLOCKED, unlocked) }
    }
}

