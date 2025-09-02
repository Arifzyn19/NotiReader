package com.notireader.app.presentation.main_screen_activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notireader.app.databinding.BottomSheetPermissionBinding

class StoragePermissionBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetPermissionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.permissionTitle.text = "Storage Permission"
        binding.permissionDescription.text = "This app needs access to your WhatsApp media folder to save and display media."
        binding.allowButton.setOnClickListener {
            (activity as? MainActivity)?.checkAndRequestFolderPermission()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

