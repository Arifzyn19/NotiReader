package com.notireader.app.domain.models

import android.graphics.Bitmap

data class MessageModel(
    val id: String = "",
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isDeleted: Boolean = false,
    val mediaPaths: List<String> = emptyList(),
    val sourcePackage: String,
    val isRead: Boolean = false,
    val iconRes: Bitmap? = null
)