package com.notireader.app.presentation.splash_screen_activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notireader.app.R
import com.notireader.app.databinding.ActivitySplashScreenBinding
import com.notireader.app.presentation.get_premium_screen_activity.GetPremiumActivity

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fullText = getString(R.string.splash_message)
        val spannable = SpannableString(fullText)
        val recoveredWord = "Recovered"
        val recoveredStart = fullText.indexOf(recoveredWord)
        if (recoveredStart != -1) {
            val recoveredEnd = recoveredStart + recoveredWord.length
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.whatsapp_primary_green)),
                recoveredStart,
                recoveredEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD_ITALIC),
                recoveredStart,
                recoveredEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val header = "Welcome to NotiReader"
            val headerEnd = fullText.indexOf("\n")

            if (headerEnd != -1) {
                spannable.setSpan(
                    RelativeSizeSpan(0.75f),
                    headerEnd + 1,
                    recoveredStart,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    RelativeSizeSpan(0.75f),
                    recoveredEnd,
                    fullText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.splashMessage.text = spannable

            binding.appLogo.setImageResource(R.mipmap.ic_launcher)
            binding.appLogo.contentDescription = getString(R.string.app_name)
            binding.main.postDelayed({
                val intent = Intent(this, GetPremiumActivity::class.java)
                startActivity(intent)
                // TODO: for development purposes
//            AdManager.show(
//                context = this,
//                adType = AdType.INTERSTITIAL,
//                unitId = "ca-app-pub-3940256099942544/1033173712",
//                container = binding.adViewBanner,
//                options = AdOptionsModel()
//            )
                finish()
            }, 3000)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.progressBar.show()
    }
}