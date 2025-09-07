package com.notireader.app.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.notireader.app.databinding.CustomExitDialogBinding

object ExitConfirmationDialog {

    fun show(context: Context, onExitConfirmed: () -> Unit) {
        val binding = CustomExitDialogBinding.inflate(LayoutInflater.from(context))

        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(0.6f)
                setFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
                )
                // Add glass blur effect for modern devices
                attributes?.apply {
                    flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    dimAmount = 0.7f
                }
            }
            setCancelable(true)
        }

        // Setup glassmorphism button interactions
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnExit.setOnClickListener {
            dialog.dismiss()
            onExitConfirmed()
        }

        // Add entrance animation for smooth glass effect
        binding.root.alpha = 0f
        binding.root.scaleX = 0.9f
        binding.root.scaleY = 0.9f

        dialog.setOnShowListener {
            binding.root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        dialog.show()
    }
}
