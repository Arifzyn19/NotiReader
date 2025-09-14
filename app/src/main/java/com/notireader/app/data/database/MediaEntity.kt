package com.notireader.app.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    foreignKeys = [ForeignKey(
        entity = MessageEntity::class,
        parentColumns = ["messageId"],
        childColumns = ["messageOwnerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("messageOwnerId")]
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val messageOwnerId: String,   // links back to message
    val path: String
)
