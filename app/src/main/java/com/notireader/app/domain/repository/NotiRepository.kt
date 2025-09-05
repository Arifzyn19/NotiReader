package com.notireader.app.domain.repository

import androidx.lifecycle.LiveData
import com.notireader.app.domain.models.MessageModel

interface NotiRepository {
    fun getAllMessages(): LiveData<List<MessageModel>>
    suspend fun insertMessage(message: MessageModel)
    suspend fun onWhatsAppNotificationReceived(message: MessageModel)
    suspend fun markMessageAsDeletedByDetails(sender: String, message: String, timestamp: Long)
    fun getMessagesForSender(sender: String): LiveData<List<MessageModel>>
    fun getMessagesForPackage(sourcePackage: String): LiveData<List<MessageModel>>
    suspend fun isDuplicateMessage(sender: String, message: String, timestamp: Long): Boolean
}