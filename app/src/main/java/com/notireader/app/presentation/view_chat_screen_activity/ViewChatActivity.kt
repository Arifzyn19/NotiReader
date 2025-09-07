package com.notireader.app.presentation.view_chat_screen_activity

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.notireader.app.databinding.ActivityViewChatBinding
import com.notireader.app.domain.models.AdOptionsModel
import com.notireader.app.util.AdManager
import com.notireader.app.util.AdType
import com.notireader.app.util.PremiumManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewChatActivity : AppCompatActivity() {
    private val viewModel: ViewChatViewModel by viewModels()
    private lateinit var binding: ActivityViewChatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val isPremium = PremiumManager.isPremiumUnlocked(this)
        val senderName = intent.getStringExtra("sender_name") ?: ""
        binding.senderTitle.text = senderName
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(this)
        viewModel.getMessagesForSender(senderName).observe(this, Observer { messages ->
            binding.messageRecyclerView.adapter = ViewChatAdapter(messages)
        })
        lifecycleScope.launch {
            viewModel.markMessagesAsRead(senderName)
        }
        binding.backButton.setOnClickListener {
            finish()
        }
        if (isPremium) {
            binding.adViewBanner.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        if (!PremiumManager.isPremiumUnlocked(this)) {
            // TODO: for development purposes
            AdManager.show(
                context = this,
                adType = AdType.INTERSTITIAL,
                unitId = "ca-app-pub-3940256099942544/1033173712",
                container = binding.adViewBanner,
                options = AdOptionsModel()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PremiumManager.isPremiumUnlocked(this)) {
            AdManager.show(
                context = this,
                adType = AdType.BANNER,
                unitId = "ca-app-pub-3940256099942544/9214589741",
                container = binding.adViewBanner,
                options = AdOptionsModel()
            )
        }
    }
}