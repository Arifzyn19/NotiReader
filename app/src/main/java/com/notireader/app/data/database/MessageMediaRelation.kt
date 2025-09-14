package com.notireader.app.data.database

import androidx.room.Embedded
import androidx.room.Relation

data class MessageMediaRelation(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "messageId",
        entityColumn = "messageOwnerId"
    )
    val media: List<MediaEntity>
)
