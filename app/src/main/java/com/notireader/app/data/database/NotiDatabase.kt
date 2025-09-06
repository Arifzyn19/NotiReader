package com.notireader.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.notireader.app.data.mappers.Converters

@Database(
    entities = [MessageEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NotiDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        const val DB_NAME = "NotiReader.db"

        @Volatile
        private var INSTANCE: NotiDatabase? = null

        fun getInstance(context: Context): NotiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotiDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}