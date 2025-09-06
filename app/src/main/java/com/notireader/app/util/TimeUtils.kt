package com.notireader.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {

    const val TIME = "hh:mm a"
    const val DATE_TIME = "dd MMM yyyy, hh:mm a"
    const val DAY_TIME = "EEE, hh:mm a"

    fun formatTimestamp(timestamp: Long, pattern: String = DATE_TIME): String {
        return try {
            val date = Date(timestamp)
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            ""
        }
    }
}