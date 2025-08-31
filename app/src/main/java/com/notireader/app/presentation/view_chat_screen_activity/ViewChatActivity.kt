package com.notireader.app.presentation.view_chat_screen_activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.notireader.app.databinding.ActivityViewChatBinding
import dagger.hilt.android.AndroidEntryPoint

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
        val senderName = intent.getStringExtra("sender_name") ?: ""
        binding.senderTitle.text = senderName
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(this)
        viewModel.getMessagesForSender(senderName).observe(this, Observer { messages ->
            binding.messageRecyclerView.adapter = ViewChatAdapter(messages)
        })
    }
}