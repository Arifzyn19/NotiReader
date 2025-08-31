package com.notireader.app.presentation.whatsapp_screen_fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WhatsappViewModel @Inject constructor(
    private val notiRepository: NotiRepository
) : ViewModel() {
    val allMessages: LiveData<List<MessageModel>> = notiRepository.getAllMessages()
}