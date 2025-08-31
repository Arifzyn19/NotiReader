package com.notireader.app.presentation.main_screen_activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.notireader.app.R
import com.notireader.app.databinding.ActivityMainBinding
import com.notireader.app.presentation.business_screen_fragment.BusinessFragment
import com.notireader.app.presentation.whatsapp_screen_fragment.WhatsappFragment
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.outerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fragments = listOf(
            WhatsappFragment.newInstance(),
            BusinessFragment.newInstance()
        )
        val fragmentNames = listOf("WhatsApp", "Business")

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }
        binding.viewPager.isUserInputEnabled = false

        // Set fade animation for ViewPager2
        binding.viewPager.setPageTransformer { page, position ->
            page.alpha = 1 - abs(position)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_whatsapp -> binding.viewPager.currentItem = 0
                R.id.nav_business -> binding.viewPager.currentItem = 1
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
    }
}