package com.notireader.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    const val TIME = "hh:mm a"
    const val DATE_TIME = "dd MMM yyyy, hh:mm a"
    const val DAY_TIME = "EEE, hh:mm a"
    const val DATE_ONLY = "dd/MM/yy"

    fun formatTimestamp(timestamp: Long, pattern: String = DATE_TIME): String {
        return try {
            val date = Date(timestamp)
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    fun formatSmartTimestamp(timestamp: Long): String {
        return try {
            val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            when {
                // Today - show only time
                isSameDay(messageDate, today) -> {
                    formatTimestamp(timestamp, TIME)
                }
                // Yesterday - show "Yesterday"
                isSameDay(messageDate, yesterday) -> {
                    "Yesterday"
                }
                // Older - show date only
                else -> {
                    formatTimestamp(timestamp, DATE_ONLY)
                }
            }
        } catch (e: Exception) {
            formatTimestamp(timestamp, DATE_TIME)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}