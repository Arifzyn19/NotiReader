package com.notireader.app.presentation.business_screen_fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BusinessViewModel @Inject constructor(
    private val notiRepository: NotiRepository
) : ViewModel() {
    val allMessages: LiveData<List<MessageModel>> = notiRepository.getMessagesForPackage("com.whatsapp.w4b")
    fun countUnread(sender: String): LiveData<Int> {
        return notiRepository.countUnreadMessagesForSender(sender)
    }
}