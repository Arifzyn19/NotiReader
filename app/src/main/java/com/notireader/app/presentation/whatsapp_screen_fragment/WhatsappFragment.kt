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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhatsappBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.allMessages.observe(viewLifecycleOwner, Observer { messages ->
            if (!messages.isNullOrEmpty()) {
                val uniqueSenders = messages.map { it.sender }.distinct()
                val adapter = WhatsappAdapter(uniqueSenders) { senderName ->
                    val intent = Intent(requireContext(), ViewChatActivity::class.java)
                    intent.putExtra("sender_name", senderName)
                    startActivity(intent)
                }
                binding.senderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.senderRecyclerView.adapter = adapter
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}