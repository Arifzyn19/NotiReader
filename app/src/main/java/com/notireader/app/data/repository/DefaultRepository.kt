package com.notireader.app.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.notireader.app.data.database.MessageDao
import com.notireader.app.data.mappers.toMessageEntity
import com.notireader.app.data.mappers.toMessageModel
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultRepository(
    private val messageDao: MessageDao
) : NotiRepository {
    override fun getAllMessages(): LiveData<List<MessageModel>> {
        return messageDao.getAllMessages().map { messageEntities ->
            messageEntities.map { messageEntity ->
                messageEntity.toMessageModel()
            }
        }
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
        insertMessage(message)
    }
}