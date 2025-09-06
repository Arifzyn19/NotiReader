package com.notireader.app.presentation.business_screen_fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.notireader.app.databinding.FragmentBusinessBinding
import com.notireader.app.presentation.view_chat_screen_activity.ViewChatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BusinessFragment : Fragment() {

    companion object {
        fun newInstance() = BusinessFragment()
    }

    private val viewModel: BusinessViewModel by viewModels()
    private var _binding: FragmentBusinessBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BusinessAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBusinessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BusinessAdapter(
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