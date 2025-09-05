package com.notireader.app.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): LiveData<List<MessageEntity>>

    @Query("UPDATE messages SET isDeleted = 1 WHERE sender = :sender AND message = :message AND timestamp = :timestamp")
    suspend fun markAsDeletedByDetails(sender: String, message: String, timestamp: Long)

    @Query("SELECT * FROM messages WHERE sender = :sender ORDER BY timestamp DESC")
    fun getMessagesForSender(sender: String): LiveData<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE sender = :sender AND message = :message AND ABS(timestamp - :timestamp) < 10000")
    suspend fun countSimilarMessages(sender: String, message: String, timestamp: Long): Int

    @Query("SELECT * FROM messages WHERE sourcePackage = :sourcePackage ORDER BY timestamp DESC")
    fun getMessagesForPackage(sourcePackage: String): LiveData<List<MessageEntity>>
}