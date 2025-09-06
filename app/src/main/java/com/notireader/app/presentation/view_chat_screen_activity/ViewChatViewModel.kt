package com.notireader.app.presentation.view_chat_screen_activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewChatViewModel @Inject constructor(
    private val notiRepository: NotiRepository
) : ViewModel() {
    fun getMessagesForSender(sender: String): LiveData<List<MessageModel>> =
        notiRepository.getMessagesForSender(sender)

    suspend fun markMessagesAsRead(sender: String) {
        notiRepository.markMessageAsRead(sender)
    }
}
