package com.notireader.app.presentation.view_chat_screen_activity

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.notireader.app.databinding.ItemMessageBinding
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.util.TimeUtils
import java.io.File

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
            binding.timeStamp.text = TimeUtils.formatTimestamp(message.timestamp, TimeUtils.TIME)
            val mediaPath = message.mediaPath
            val context = binding.root.context
            binding.mediaImageView.visibility = View.GONE
            binding.playButton.visibility = View.GONE
            binding.openDocButton.visibility = View.GONE

            if (mediaPath != null) {
                val file = File(mediaPath)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val lower = mediaPath.lowercase()
                Log.d("xyz", "bind: $lower")
                when {
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> {
                        binding.mediaImageView.visibility = View.VISIBLE
                        Glide.with(context).load(uri).into(binding.mediaImageView)
                    }

                    lower.endsWith(".gif") -> {
                        binding.mediaImageView.visibility = View.VISIBLE
                        Glide.with(context).asGif().load(uri).into(binding.mediaImageView)
                    }

                    lower.endsWith(".mp4") || lower.endsWith(".3gp") || lower.endsWith(".mkv") -> {
                        binding.playButton.visibility = View.VISIBLE
                        binding.playButton.text = "Play Video"
                        binding.playButton.setOnClickListener {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "video/*")
                            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(intent)
                        }
                    }

                    lower.endsWith(".mp3") || lower.endsWith(".opus") || lower.endsWith(".wav") || lower.endsWith(".m4a") -> {
                        binding.playButton.visibility = View.VISIBLE
                        binding.playButton.text = "Play Audio"
                        binding.playButton.setOnClickListener {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "audio/*")
                            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(intent)
                        }
                    }

                    lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(
                        ".ppt"
                    ) || lower.endsWith(".pptx") || lower.endsWith(".txt") -> {
                        binding.openDocButton.visibility = View.VISIBLE
                        binding.openDocButton.setOnClickListener {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "application/*")
                            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}
