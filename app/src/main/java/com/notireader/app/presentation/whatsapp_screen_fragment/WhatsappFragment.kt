package com.notireader.app.presentation.whatsapp_screen_fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.notireader.app.databinding.FragmentWhatsappBinding
import com.notireader.app.presentation.view_chat_screen_activity.ViewChatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WhatsappFragment : Fragment() {

    companion object {
        fun newInstance() = WhatsappFragment()
    }

    private val viewModel: WhatsappViewModel by viewModels()

    private var _binding: FragmentWhatsappBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WhatsappAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhatsappBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WhatsappAdapter(
            onClick = { senderName ->
                val intent = Intent(requireContext(), ViewChatActivity::class.java)
                intent.putExtra("sender_name", senderName)
                startActivity(intent)
            },
            getUnreadCount = { sender, onResult ->
                viewModel.countUnread(sender).observe(viewLifecycleOwner) { count ->
                    onResult(count)
                }
            }
        )

        binding.senderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.senderRecyclerView.adapter = adapter

        viewModel.allMessages.observe(viewLifecycleOwner, Observer { messages ->
            if (!messages.isNullOrEmpty()) {
                binding.imgNoMessages.visibility = View.GONE
                binding.senderRecyclerView.visibility = View.VISIBLE

                val uniqueMessage = messages
                    .groupBy { it.sender }
                    .map { (_, senderMessages) ->
                        senderMessages.maxByOrNull { it.timestamp }!!
                    }

                adapter.updateData(uniqueMessage)
            } else {
                binding.senderRecyclerView.visibility = View.GONE
                binding.imgNoMessages.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}