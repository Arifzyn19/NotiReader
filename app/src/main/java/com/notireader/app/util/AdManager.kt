package com.notireader.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.notireader.app.domain.models.AdOptionsModel

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun show(
        context: Context,
        adType: AdType,
        container: ViewGroup? = null,
        options: AdOptionsModel = AdOptionsModel()
    ) {
        when (adType) {
            AdType.BANNER -> {
                Log.d("xyz", "Attempting to show banner ad")
                val unitId = "ca-app-pub-8900849690463057/5193336448"
                showBanner(context, unitId, container, options)
            }
            AdType.INTERSTITIAL -> {
                Log.d("xyz", "Attempting to show interstitial ad")
                val unitId = "ca-app-pub-8900849690463057/4002859881"
                showInterstitial(context, unitId, options)
            }
            AdType.REWARDED -> {
                Log.d("xyz", "Attempting to show rewarded ad")
                val unitId = ""
                showRewarded(context, unitId, options)
            }
            AdType.NATIVE -> {
                Log.d("xyz", "Attempting to show native ad")
                val unitId = ""
                showNative(context, unitId, container, options)
            }
        }
    }

    private fun showBanner(
        context: Context,
        unitId: String,
        container: ViewGroup?,
        options: AdOptionsModel
    ) {
        if (container == null) {
            Log.e("xyz", "Banner container is null")
            options.onAdFailed?.invoke("Banner container is null")
            return
        }
        val adView = AdView(context).apply {
            adUnitId = unitId
            setAdSize(options.size)
        }
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("xyz", "Banner ad loaded")
                options.onAdLoaded?.invoke()
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("xyz", "Banner ad failed to load: ${error.message}")
                options.onAdFailed?.invoke(error.message)
            }
        }
        container.removeAllViews()
        container.addView(adView)
        Log.d("xyz", "Loading banner ad...")
        adView.loadAd(AdRequest.Builder().apply {
            options.targeting?.let { addNetworkExtrasBundle(AdMobAdapter::class.java, it) }
        }.build())
    }

    private fun showInterstitial(
        context: Context,
        unitId: String,
        options: AdOptionsModel
    ) {
        if (interstitialAd != null) {
            Log.d("xyz", "Showing cached interstitial ad")
            interstitialAd?.show(context as Activity)
            return
        }
        Log.d("xyz", "Loading interstitial ad...")
        InterstitialAd.load(context, unitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("xyz", "Interstitial ad loaded")
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d("xyz", "Interstitial ad dismissed")
                            interstitialAd = null
                        }
                    }
                    ad.show(context as Activity)
                    options.onAdLoaded?.invoke()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("xyz", "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    options.onAdFailed?.invoke(error.message)
                }
            })
    }

    private fun showRewarded(
        context: Context,
        unitId: String,
        options: AdOptionsModel
    ) {
        if (rewardedAd != null) {
            Log.d("xyz", "Showing cached rewarded ad")
            rewardedAd?.show(context as Activity) { options.onRewarded?.invoke() }
            return
        }
        Log.d("xyz", "Loading rewarded ad...")
        RewardedAd.load(context, unitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("xyz", "Rewarded ad loaded")
                    rewardedAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d("xyz", "Rewarded ad dismissed")
                            rewardedAd = null
                        }
                    }
                    ad.show(context as Activity) { options.onRewarded?.invoke() }
                    options.onAdLoaded?.invoke()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("xyz", "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                    options.onAdFailed?.invoke(error.message)
                }
            })
    }

    private fun showNative(
        context: Context,
        unitId: String,
        container: ViewGroup?,
        options: AdOptionsModel
    ) {
        Log.d("xyz", "Native ad requested (not implemented)")
        options.onAdFailed?.invoke("Native ads not implemented in this snippet")
    }
}

enum class AdType { BANNER, INTERSTITIAL, REWARDED, NATIVE }