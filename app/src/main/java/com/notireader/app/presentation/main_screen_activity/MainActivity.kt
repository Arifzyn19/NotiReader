package com.notireader.app.presentation.main_screen_activity

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.notireader.app.R
import com.notireader.app.databinding.ActivityMainBinding
import com.notireader.app.domain.models.AdOptionsModel
import com.notireader.app.domain.services.RateUs
import com.notireader.app.domain.services.ShareApp
import com.notireader.app.presentation.business_screen_fragment.BusinessFragment
import com.notireader.app.presentation.customer_support_screen_activity.CustomerSupportActivity
import com.notireader.app.presentation.get_premium_screen_activity.GetPremiumActivity
import com.notireader.app.presentation.guide_screen_activity.GuideActivity
import com.notireader.app.presentation.privacy_policy_screen_activity.PrivacyPolicyActivity
import com.notireader.app.presentation.terms_and_condition_screen_activity.TermsAndConditionActivity
import com.notireader.app.presentation.whatsapp_screen_fragment.WhatsappFragment
import com.notireader.app.util.AdManager
import com.notireader.app.util.AdType
import com.notireader.app.util.ExitConfirmationDialog
import com.notireader.app.util.PremiumManager
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQUEST_CODE_OPEN_DOCUMENT_TREE = 102
        private const val REQUEST_CODE_NOTIFICATION_LISTENER_SETTINGS = 103
        private const val PREFS_NAME = "noti_reader_prefs"
        private const val KEY_FOLDER_URI = "folder_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isPremium = PremiumManager.isPremiumUnlocked(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.outerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        if (!isNotificationServiceEnabled()) {
//            NotificationPermissionBottomSheet().show(supportFragmentManager, "notification_permission")
//        } else if (!isStoragePermissionGranted()) {
//            StoragePermissionBottomSheet().show(supportFragmentManager, "storage_permission")
//        }

        val fragments = listOf(
            WhatsappFragment.newInstance(), BusinessFragment.newInstance()
        )
        val fragmentNames = listOf("WA Recovery", "Business")

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.setPageTransformer { page, position ->
            page.alpha = 1 - abs(position)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_whatsapp -> binding.viewPager.currentItem = 0

                R.id.nav_business -> binding.viewPager.currentItem = 1

                R.id.nav_remove_ads -> {
                    if (isPremium) {
                        Toast.makeText(this, "You already have premium version!", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(this, GetPremiumActivity::class.java)
                        startActivity(intent)
                    }
                }

                R.id.nav_customer_support -> {
                    val intent = Intent(this, CustomerSupportActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_rate_us -> {
                    RateUs.rate(this)
                }

                R.id.nav_privacy_policy -> {
                    val intent = Intent(this, PrivacyPolicyActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_terms_and_condition -> {
                    val intent = Intent(this, TermsAndConditionActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_app_not_working -> {
                    val intent = Intent(this, GuideActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_share_app -> {
                    ShareApp.share(this)
                }
            }
            binding.drawerLayout.closeDrawers()
            true
        }
        binding.viewPager.currentItem = 0
        binding.name.text = fragmentNames[0]

        binding.hamBurgerButton.setOnClickListener {
            binding.drawerLayout.open()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.name.text = fragmentNames[position]
            }
        })

        binding.guideButton.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }

        if (isPremium) {
            binding.adViewBanner.visibility = android.view.View.GONE
            binding.goPremiumBtn.visibility = android.view.View.GONE
            binding.navigationView.menu.findItem(R.id.nav_remove_ads)?.isVisible = false
        } else {
            binding.goPremiumBtn.setOnClickListener {
                val intent = Intent(this, GetPremiumActivity::class.java)
                startActivity(intent)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ExitConfirmationDialog.show(this@MainActivity) {
                    finish()
                }
            }
        })
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
        if (!isNotificationServiceEnabled()) {
            NotificationPermissionBottomSheet().show(supportFragmentManager, "notification_permission")
        } else if (!isStoragePermissionGranted()) {
            StoragePermissionBottomSheet().show(supportFragmentManager, "storage_permission")
        } else {
            validateSavedFolderPermission()
        }
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
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString(KEY_FOLDER_URI, null) != null
    }

    private fun validateSavedFolderPermission() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedUri = prefs.getString(KEY_FOLDER_URI, null)

        if (savedUri != null) {
            val requiredUri = "content://com.android.externalstorage.documents/tree/primary:Android/media"
            val decodedSavedUri = Uri.decode(savedUri)
            val decodedRequiredUri = Uri.decode(requiredUri)
            if (decodedSavedUri == decodedRequiredUri || decodedSavedUri.endsWith("primary:Android/media")) {
                Log.d("xyz", "validateSavedFolderPermission: picked up correct folder $savedUri")
            } else {
                Log.w("xyz", "Invalid folder saved: $savedUri, forcing re-pick.")
                prefs.edit { remove(KEY_FOLDER_URI) }
                Toast.makeText(this, "Please select only Android/media folder", Toast.LENGTH_SHORT).show()
                checkAndRequestFolderPermission()
            }
        } else {
            checkAndRequestFolderPermission()
        }
    }

    internal fun checkAndRequestFolderPermission() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedUri = prefs.getString(KEY_FOLDER_URI, null)

        if (savedUri == null) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(
                    "android.provider.extra.INITIAL_URI", "content://com.android.externalstorage.documents/document/primary:Android/media".toUri()
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE)
        } else {
            Log.d("xyz", "Already have folder permission: $savedUri")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_NOTIFICATION_LISTENER_SETTINGS) {
            // After returning from notification listener settings, check and request folder permission
            checkAndRequestFolderPermission()
        }
        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE) {
            if (resultCode == Activity.RESULT_OK) {
                val treeUri: Uri? = data?.data
                if (treeUri != null) {
                    contentResolver.takePersistableUriPermission(
                        treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                        putString(KEY_FOLDER_URI, treeUri.toString())
                    }

                    Log.d("xyz", "Folder permission granted: $treeUri")

                    val docFile = DocumentFile.fromTreeUri(this, treeUri)
                    docFile?.listFiles()?.forEach {
                        Log.d("xyz", "File: ${it.name}")
                    }
                }
            } else {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                Log.i("xyz", "User cancelled folder picker or something went wrong.")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }
}