package com.notireader.app.domain.models

data class MessageModel(
    val id: Int = 0,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isDeleted: Boolean = false,
    val mediaPath: String? = null
)