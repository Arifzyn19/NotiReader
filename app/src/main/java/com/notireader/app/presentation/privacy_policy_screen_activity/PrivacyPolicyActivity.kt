package com.notireader.app.presentation.privacy_policy_screen_activity

import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notireader.app.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup glass morphism effects
        setupGlassMorphismEffects()

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupGlassMorphismEffects() {
        // Wait for the content container to be laid out
        binding.contentContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove the listener to avoid multiple calls
                binding.contentContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Get the actual height of the content container
                val contentHeight = binding.contentContainer.height

                // Apply dynamic height to glass overlay and ambient glow effects
                binding.glassOverlay.layoutParams.height = contentHeight
                binding.ambientGlow.layoutParams.height = contentHeight

                // Request layout to apply changes
                binding.glassOverlay.requestLayout()
                binding.ambientGlow.requestLayout()

                // Apply sophisticated glass morphism animations
                applyGlassMorphismAnimations()
            }
        })
    }

    private fun applyGlassMorphismAnimations() {
        // Animate glass overlay with subtle fade-in
        binding.glassOverlay.apply {
            alpha = 0f
            animate()
                .alpha(0.6f)
                .setDuration(800)
                .setStartDelay(200)
                .start()
        }

        // Animate ambient glow with gentle pulse
        binding.ambientGlow.apply {
            alpha = 0f
            animate()
                .alpha(0.3f)
                .setDuration(1200)
                .setStartDelay(400)
                .start()
        }

        // Animate content container with smooth emergence
        binding.contentContainer.apply {
            alpha = 0f
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setStartDelay(100)
                .start()
        }
    }
}