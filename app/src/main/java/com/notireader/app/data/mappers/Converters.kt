package com.notireader.app.data.mappers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

class Converters {
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?): ByteArray? {
        if (bitmap == null) return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? {
        return if (bytes == null) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.joinToString("||") // join with separator
    }

    @TypeConverter
    fun toList(data: String?): List<String>? {
        return data?.split("||")?.filter { it.isNotEmpty() }
    }
}