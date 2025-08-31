package com.notireader.app.presentation.view_chat_screen_activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.notireader.app.databinding.ItemMessageBinding
import com.notireader.app.domain.models.MessageModel

class ViewChatAdapter(private val messages: List<MessageModel>) : RecyclerView.Adapter<ViewChatAdapter.MessageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageModel) {
            binding.messageText.text = message.message
            binding.timeStamp.text = message.timestamp.toString()
        }
    }
}

