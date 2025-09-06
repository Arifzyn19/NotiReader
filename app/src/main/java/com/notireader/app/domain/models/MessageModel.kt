package com.notireader.app.domain.models

import android.graphics.Bitmap

data class MessageModel(
    val id: Int = 0,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isDeleted: Boolean = false,
    val mediaPath: String? = null,
    val sourcePackage: String,
    val isRead: Boolean = false,
    val iconRes: Bitmap? = null
)