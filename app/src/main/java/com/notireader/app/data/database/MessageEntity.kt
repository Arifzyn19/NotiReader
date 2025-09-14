package com.notireader.app.data.database

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String = UUID.randomUUID().toString(),
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isDeleted: Boolean = false,
    val sourcePackage: String,
    val isRead: Boolean = false,
    val iconRes: Bitmap? = null
)