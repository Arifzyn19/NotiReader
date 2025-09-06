package com.notireader.app.presentation.business_screen_fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.notireader.app.R
import com.notireader.app.databinding.ItemSenderBinding
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.util.TimeUtils

class BusinessAdapter(
    private val onClick: (String) -> Unit,
    private val getUnreadCount: (String, (Int) -> Unit) -> Unit
) : RecyclerView.Adapter<BusinessAdapter.SenderViewHolder>() {

    private var messages: List<MessageModel> = emptyList()

    fun updateData(newMessages: List<MessageModel>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SenderViewHolder {
        val binding = ItemSenderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SenderViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SenderViewHolder,
        position: Int
    ) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class SenderViewHolder(private val binding: ItemSenderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageModel) {
            binding.senderName.text = message.sender
            message.iconRes?.let {
                binding.profilePicture.setImageBitmap(it)
            } ?: binding.profilePicture.setImageResource(R.drawable.ic_user)
            getUnreadCount(message.sender) { count ->
                binding.numberOfUnReads.text = count.toString()
                binding.numberOfUnReads.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
            binding.latestTime.text = TimeUtils.formatTimestamp(message.timestamp, TimeUtils.TIME)
            binding.latestMessage.text = message.message
            binding.root.setOnClickListener { onClick(message.sender) }
        }
    }
}