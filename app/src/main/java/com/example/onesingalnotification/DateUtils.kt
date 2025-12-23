package com.example.onesingalnotification

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun getChatDateLabel(time: Long): String {
        val msgDate = Calendar.getInstance().apply {
            timeInMillis = time
        }

        val today = Calendar.getInstance()

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        return when {
            isSameDay(msgDate, today) -> "Today"
            isSameDay(msgDate, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(time))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
