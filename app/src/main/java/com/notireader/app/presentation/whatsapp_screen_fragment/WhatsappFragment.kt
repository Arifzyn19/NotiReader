package com.notireader.app.presentation.whatsapp_screen_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.notireader.app.R

class WhatsappFragment : Fragment() {

    companion object {
        fun newInstance() = WhatsappFragment()
    }

    private val viewModel: WhatsappViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_whatsapp, container, false)
    }
}