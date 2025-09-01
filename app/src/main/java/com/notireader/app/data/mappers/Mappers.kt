package com.notireader.app.data.mappers

import com.notireader.app.data.database.MessageEntity
import com.notireader.app.domain.models.MessageModel

fun MessageModel.toMessageEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        sender = sender,
        message = message,
        timestamp = timestamp,
        isDeleted = isDeleted,
        mediaPath = mediaPath
    )
}

fun MessageEntity.toMessageModel(): MessageModel {
    return MessageModel(
        id = id,
        sender = sender,
        message = message,
        timestamp = timestamp,
        isDeleted = isDeleted,
        mediaPath = mediaPath
    )
}