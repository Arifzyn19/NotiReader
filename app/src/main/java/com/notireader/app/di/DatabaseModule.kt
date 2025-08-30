package com.notireader.app.di

import android.content.Context
import androidx.room.Room
import com.notireader.app.data.repository.DefaultRepository
import com.notireader.app.data.database.MessageDao
import com.notireader.app.data.database.NotiDatabase
import com.notireader.app.domain.repository.NotiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): NotiDatabase {
        return Room.databaseBuilder(
            appContext,
            NotiDatabase::class.java,
            NotiDatabase.DB_NAME
        ).build()
    }

    @Provides
    fun provideMessageDao(notiDatabase: NotiDatabase): MessageDao {
        return notiDatabase.messageDao()
    }

    @Provides
    @Singleton
    fun provideNotiRepository(messageDao: MessageDao): NotiRepository {
        return DefaultRepository(messageDao)
    }
}