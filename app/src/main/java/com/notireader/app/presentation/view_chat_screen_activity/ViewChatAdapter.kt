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

class ViewChatAdapter : RecyclerView.Adapter<ViewChatAdapter.MessageViewHolder>() {

    // Data class to represent individual message items (including individual media files)
    data class MessageItem(
        val messageModel: MessageModel,
        val mediaPath: String? = null,
        val mediaIndex: Int = 0,
        val totalMediaCount: Int = 0
    )

    private var messageItems: List<MessageItem> = emptyList()

    fun updateMessages(messages: List<MessageModel>) {
        messageItems = expandMessagesWithMedia(messages)
        notifyDataSetChanged()
    }

    private fun expandMessagesWithMedia(messages: List<MessageModel>): List<MessageItem> {
        val expandedItems = mutableListOf<MessageItem>()

        for (message in messages) {
            if (message.mediaPaths.isNotEmpty()) {
                // Create separate items for each media file in REVERSE order
                message.mediaPaths.asReversed().forEachIndexed { reverseIndex, mediaPath ->
                    // Calculate the original index for proper counter display
                    val originalIndex = message.mediaPaths.size - 1 - reverseIndex
                    expandedItems.add(
                        MessageItem(
                            messageModel = message,
                            mediaPath = mediaPath,
                            mediaIndex = originalIndex,
                            totalMediaCount = message.mediaPaths.size
                        )
                    )
                }
            } else {
                // Create single item for text-only message
                expandedItems.add(MessageItem(messageModel = message))
            }
        }

        return expandedItems
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messageItems[position])
    }

    override fun getItemCount(): Int = messageItems.size

    inner class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(messageItem: MessageItem) {
            val message = messageItem.messageModel
            val context = binding.root.context

            // Set message text - modify for media items
            if (messageItem.mediaPath != null) {
                // For media items, show message with media index if multiple
                val messageText = if (messageItem.totalMediaCount > 1) {
                    "${message.message} (${messageItem.mediaIndex + 1}/${messageItem.totalMediaCount})"
                } else {
                    message.message
                }
                binding.messageText.text = messageText
            } else {
                binding.messageText.text = message.message
            }

            binding.timeStamp.text = TimeUtils.formatTimestamp(message.timestamp, TimeUtils.TIME)

            // Reset visibility
            binding.mediaImageView.visibility = View.GONE
            binding.playButton.visibility = View.GONE
            binding.openDocButton.visibility = View.GONE

            // Handle media if present
            messageItem.mediaPath?.let { mediaPath ->
                val file = File(mediaPath)
                if (!file.exists()) {
                    Log.w("xyz", "Media file not found: $mediaPath")
                    return@let
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val lower = mediaPath.lowercase()
                Log.d("xyz", "bind media: $lower")

                when {
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> {
                        binding.mediaImageView.visibility = View.VISIBLE
                        Glide.with(context)
                            .load(uri)
                            .into(binding.mediaImageView)
                    }

                    lower.endsWith(".gif") -> {
                        binding.mediaImageView.visibility = View.VISIBLE
                        Glide.with(context)
                            .asGif()
                            .load(uri)
                            .into(binding.mediaImageView)
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

                    lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") ||
                            lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".ppt") ||
                            lower.endsWith(".pptx") || lower.endsWith(".txt") -> {
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
