package com.notireader.app.data.database

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isDeleted: Boolean = false,
    val mediaPath: String? = null,
    val sourcePackage: String,
    val isRead: Boolean = false,
    val iconRes: Bitmap? = null
)