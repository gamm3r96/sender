package com.example.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {

    private val fullDateTimeFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm:ss", Locale.getDefault())
    private val compactDateTimeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val detailedFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' HH:mm", Locale.getDefault())

    /**
     * Formats timestamp into an intelligent human-friendly relative time
     * (e.g., "Just now", "3m ago", "2h ago", "Yesterday at 14:20", "Aug 14, 2026")
     */
    fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0L) return "Unknown"
        val diffMillis = now - timestamp

        if (diffMillis < 0) {
            return formatCompact(timestamp)
        }

        val seconds = diffMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            seconds < 45 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 && isSameDay(timestamp, now) -> "${hours}h ago"
            isYesterday(timestamp, now) -> "Yesterday at " + timeOnlyFormat.format(Date(timestamp))
            diffMillis < 7 * DateUtils.DAY_IN_MILLIS -> {
                val days = diffMillis / DateUtils.DAY_IN_MILLIS
                "${days}d ago (${timeOnlyFormat.format(Date(timestamp))})"
            }
            else -> dateOnlyFormat.format(Date(timestamp))
        }
    }

    /**
     * Full date and time with seconds: "Aug 17, 2026 · 09:55:12"
     */
    fun formatFullDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"
        return fullDateTimeFormat.format(Date(timestamp))
    }

    /**
     * Detailed representation: "Monday, August 17, 2026 at 09:55"
     */
    fun formatDetailed(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"
        return detailedFormat.format(Date(timestamp))
    }

    /**
     * Compact date and time: "Aug 17, 09:55"
     */
    fun formatCompact(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"
        return compactDateTimeFormat.format(Date(timestamp))
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(time: Long, now: Long): Boolean {
        val calTime = Calendar.getInstance().apply { timeInMillis = time }
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        return calNow.get(Calendar.YEAR) == calTime.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calTime.get(Calendar.DAY_OF_YEAR)
    }
}
