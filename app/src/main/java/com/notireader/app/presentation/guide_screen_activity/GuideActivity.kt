package com.notireader.app.presentation.guide_screen_activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notireader.app.databinding.ActivityGuideBinding
import com.notireader.app.domain.models.AdOptionsModel
import com.notireader.app.presentation.main_screen_activity.NotificationPermissionBottomSheet
import com.notireader.app.presentation.main_screen_activity.StoragePermissionBottomSheet
import com.notireader.app.util.AdManager
import com.notireader.app.util.AdType
import com.notireader.app.util.PremiumManager

class GuideActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuideBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val isPremium = PremiumManager.isPremiumUnlocked(this)
        setupDropdownSections()
        binding.btnEnable.setOnClickListener {
            if (isNotificationServiceEnabled()) {
                Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show()
            } else {
                NotificationPermissionBottomSheet().show(supportFragmentManager, "notification_permission")
            }
        }
        binding.btnAllow.setOnClickListener {
            if (isStoragePermissionGranted()) {
                Toast.makeText(this, "Storage permission already granted", Toast.LENGTH_SHORT).show()
            } else {
                StoragePermissionBottomSheet().show(supportFragmentManager, "storage_permission")
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        if (isPremium) {
            binding.adViewBanner.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        val isPremium = PremiumManager.isPremiumUnlocked(this)
        if (!isPremium) {
            // TODO: for development purposes
//            AdManager.show(
//                context = this,
//                adType = AdType.INTERSTITIAL,
//                unitId = "ca-app-pub-3940256099942544/1033173712",
//                container = binding.adViewBanner,
//                options = AdOptionsModel()
//            )
        }
    }

    override fun onResume() {
        super.onResume()
        val isPremium = PremiumManager.isPremiumUnlocked(this)
        if (!isPremium) {
            AdManager.show(
                context = this,
                adType = AdType.BANNER,
                unitId = "ca-app-pub-3940256099942544/9214589741",
                container = binding.adViewBanner,
                options = AdOptionsModel()
            )
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val cn = ComponentName(this, com.notireader.app.domain.services.WhatsAppNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun isStoragePermissionGranted(): Boolean {
        val prefs = getSharedPreferences("noti_reader_prefs", MODE_PRIVATE)
        return prefs.getString("folder_uri", null) != null
    }

    private fun setupDropdownSections() {
        setupDropdown(
            binding.headerNotificationAccess,
            binding.contentNotificationAccess,
            binding.arrowNotificationAccess,
            true
        )
        setupDropdown(
            binding.headerAllowAccess,
            binding.contentAllowAccess,
            binding.arrowAllowAccess,
            false
        )
        setupDropdown(
            binding.headerUnmuteNotifications,
            binding.contentUnmuteNotifications,
            binding.arrowUnmuteNotifications,
            false
        )
        setupDropdown(
            binding.headerMediaAutodownload,
            binding.contentMediaAutodownload,
            binding.arrowMediaAutodownload,
            false
        )
    }

    private fun setupDropdown(
        header: LinearLayout,
        content: LinearLayout,
        arrow: ImageView,
        initiallyExpanded: Boolean
    ) {
        content.visibility = if (initiallyExpanded) View.VISIBLE else View.GONE
        arrow.rotation = if (initiallyExpanded) 180f else 0f
        header.setOnClickListener {
            toggleDropdown(content, arrow)
        }
    }

    private fun toggleDropdown(content: LinearLayout, arrow: ImageView) {
        val isExpanded = content.visibility == View.VISIBLE
        if (isExpanded) {
            collapseView(content)
            rotateArrow(arrow, 180f, 0f)
        } else {
            expandView(content)
            rotateArrow(arrow, 0f, 180f)
        }
    }

    private fun expandView(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                view.layoutParams.height = if (interpolatedTime == 1f) {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    (targetHeight * interpolatedTime).toInt()
                }
                view.requestLayout()
            }

            override fun willChangeBounds(): Boolean = true
        }
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        view.startAnimation(animation)
    }

    private fun collapseView(view: View) {
        val initialHeight = view.measuredHeight
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (interpolatedTime == 1f) {
                    view.visibility = View.GONE
                } else {
                    view.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                    view.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean = true
        }
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        view.startAnimation(animation)
    }

    private fun rotateArrow(arrow: ImageView, fromDegree: Float, toDegree: Float) {
        val rotateAnimation = RotateAnimation(
            fromDegree,
            toDegree,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotateAnimation.duration = 300
        rotateAnimation.interpolator = DecelerateInterpolator()
        rotateAnimation.fillAfter = true
        arrow.startAnimation(rotateAnimation)
    }

    // Optional: Extension functions for slideDown/slideUp
    private fun View.slideDown(duration: Long = 300) {
        this.measure(
            View.MeasureSpec.makeMeasureSpec((this.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = this.measuredHeight
        this.layoutParams.height = 0
        this.visibility = View.VISIBLE
        ValueAnimator.ofInt(0, targetHeight).apply {
            addUpdateListener { animation ->
                this@slideDown.layoutParams.height = animation.animatedValue as Int
                this@slideDown.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    this@slideDown.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            })
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }.start()
    }

    private fun View.slideUp(duration: Long = 300) {
        ValueAnimator.ofInt(this.measuredHeight, 0).apply {
            addUpdateListener { animation ->
                this@slideUp.layoutParams.height = animation.animatedValue as Int
                this@slideUp.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    this@slideUp.visibility = View.GONE
                }
            })
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }.start()
    }
}