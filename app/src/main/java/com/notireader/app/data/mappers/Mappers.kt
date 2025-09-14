package com.notireader.app.data.mappers

import com.notireader.app.data.database.MessageEntity
import com.notireader.app.data.database.MessageMediaRelation
import com.notireader.app.domain.models.MessageModel
import java.util.UUID

fun MessageModel.toMessageEntity(): MessageEntity {
    return MessageEntity(
        messageId = id.ifBlank { UUID.randomUUID().toString() },
        sender = sender,
        message = message,
        timestamp = timestamp,
        isDeleted = isDeleted,
        sourcePackage = sourcePackage,
        isRead = isRead,
        iconRes = iconRes
    )
}

fun MessageEntity.toMessageModel(): MessageModel {
    return MessageModel(
        id = messageId,
        sender = sender,
        message = message,
        timestamp = timestamp,
        isDeleted = isDeleted,
        mediaPaths = emptyList(), // populated with the help of relation
        sourcePackage = sourcePackage,
        isRead = isRead,
        iconRes = iconRes
    )
}

fun MessageMediaRelation.toMessageModel(): MessageModel {
    return MessageModel(
        id = message.messageId,
        sender = message.sender,
        message = message.message,
        timestamp = message.timestamp,
        isDeleted = message.isDeleted,
        mediaPaths = media.map { it.path },
        sourcePackage = message.sourcePackage,
        isRead = message.isRead,
        iconRes = message.iconRes
    )
}
