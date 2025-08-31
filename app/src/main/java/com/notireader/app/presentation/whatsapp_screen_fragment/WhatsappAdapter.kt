package com.notireader.app.presentation.whatsapp_screen_fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.notireader.app.databinding.ItemSenderBinding

class WhatsappAdapter(
    private val senders: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<WhatsappAdapter.SenderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SenderViewHolder {
        val binding = ItemSenderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SenderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SenderViewHolder, position: Int) {
        holder.bind(senders[position])
    }

    override fun getItemCount(): Int = senders.size

    inner class SenderViewHolder(private val binding: ItemSenderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(sender: String) {
            binding.senderName.text = sender
            binding.root.setOnClickListener { onClick(sender) }
        }
    }
}
