package com.notireader.app.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleMedia(media: MediaEntity)

    @Transaction
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesWithMedia(): LiveData<List<MessageMediaRelation>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): LiveData<List<MessageEntity>>

    @Query("UPDATE messages SET isDeleted = 1 WHERE sender = :sender AND message = :message AND timestamp = :timestamp")
    suspend fun markAsDeletedByDetails(sender: String, message: String, timestamp: Long)

    @Transaction
    @Query("SELECT * FROM messages WHERE sender = :sender ORDER BY timestamp DESC")
    fun getMessagesWithMediaForSender(sender: String): LiveData<List<MessageMediaRelation>>

    @Query("SELECT * FROM messages WHERE sender = :sender ORDER BY timestamp DESC")
    fun getMessagesForSender(sender: String): LiveData<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE sender = :sender AND message = :message AND ABS(timestamp - :timestamp) < 10000")
    suspend fun countSimilarMessages(sender: String, message: String, timestamp: Long): Int

    @Transaction
    @Query("SELECT * FROM messages WHERE sourcePackage = :sourcePackage ORDER BY timestamp DESC")
    fun getMessagesWithMediaForPackage(sourcePackage: String): LiveData<List<MessageMediaRelation>>

    @Query("SELECT * FROM messages WHERE sourcePackage = :sourcePackage ORDER BY timestamp DESC")
    fun getMessagesForPackage(sourcePackage: String): LiveData<List<MessageEntity>>

    @Query("UPDATE messages SET isRead = 1 WHERE sender = :sender")
    suspend fun markMessagesAsRead(sender: String)

    @Query("SELECT COUNT(*) FROM messages WHERE sender = :sender AND isRead = 0")
    fun countUnreadMessagesForSender(sender: String): LiveData<Int>

    // Helper method to save message with media in a transaction
    @Transaction
    suspend fun insertMessageWithMedia(message: MessageEntity, mediaPaths: List<String>) {
        insertMessage(message)
        if (mediaPaths.isNotEmpty()) {
            val mediaList = mediaPaths.map { path ->
                MediaEntity(messageOwnerId = message.messageId, path = path)
            }
            insertMedia(mediaList)
        }
    }
}