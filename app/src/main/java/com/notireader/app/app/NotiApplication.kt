package com.notireader.app.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NotiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}