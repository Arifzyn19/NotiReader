package com.notireader.app.domain.models

import android.os.Bundle
import com.google.android.gms.ads.AdSize

data class AdOptionsModel(
    val size: AdSize = AdSize.BANNER,
    val targeting: Bundle? = null,
    val onAdLoaded: (() -> Unit)? = null,
    val onAdFailed: ((String) -> Unit)? = null,
    val onRewarded: (() -> Unit)? = null
)
