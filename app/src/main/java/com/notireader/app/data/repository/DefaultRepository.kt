package com.notireader.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.notireader.app.data.database.MessageDao
import com.notireader.app.data.mappers.toMessageEntity
import com.notireader.app.data.mappers.toMessageModel
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DefaultRepository(
    private val messageDao: MessageDao
) : NotiRepository {
    private val mutex = Mutex()

    override fun getAllMessages(): LiveData<List<MessageModel>> {
        return messageDao.getAllMessages().map { messageEntities ->
            messageEntities.map { messageEntity ->
                messageEntity.toMessageModel()
            }
        }
    }

    override fun countUnreadMessagesForSender(sender: String): LiveData<Int> {
        return messageDao.countUnreadMessagesForSender(sender)
    }

    override suspend fun markMessageAsRead(sender: String) {
        messageDao.markMessagesAsRead(sender)
    }

    override suspend fun insertMessage(message: MessageModel) {
        withContext(Dispatchers.IO) {
            messageDao.insertMessage(message.toMessageEntity())
        }
    }

    override suspend fun markMessageAsDeletedByDetails(sender: String, message: String, timestamp: Long) {
        withContext(Dispatchers.IO) {
            messageDao.markAsDeletedByDetails(sender, message, timestamp)
        }
    }

    override suspend fun onWhatsAppNotificationReceived(message: MessageModel) {
        Log.d("xyz", "onWhatsAppNotificationReceived: $message")
        val isDuplicate = mutex.withLock {
            messageDao.countSimilarMessages(message.sender, message.message, message.timestamp) > 0
        }
        if (!isDuplicate) {
            insertMessage(message)
        } else {
            Log.d("xyz", "Duplicate message detected, skipping insert: $message")
        }
    }

    override fun getMessagesForSender(sender: String): LiveData<List<MessageModel>> {
        return messageDao.getMessagesForSender(sender).map { messageEntities ->
            messageEntities.map { it.toMessageModel() }
        }
    }

    override fun getMessagesForPackage(sourcePackage: String): LiveData<List<MessageModel>> {
        return messageDao.getMessagesForPackage(sourcePackage).map { messageEntities ->
            messageEntities.map { it.toMessageModel() }
        }
    }

    override suspend fun isDuplicateMessage(sender: String, message: String, timestamp: Long): Boolean {
        return withContext(Dispatchers.IO) {
            messageDao.countSimilarMessages(sender, message, timestamp) > 0
        }
    }
}