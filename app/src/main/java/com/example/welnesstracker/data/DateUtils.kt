package com.example.welnesstracker.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private fun keyFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun today(): String = keyFormat().format(Date())

    /** Returns [n] day keys ending with today, oldest first. */
    fun lastDays(n: Int): List<String> {
        val fmt = keyFormat()
        val cal = Calendar.getInstance()
        val result = ArrayList<String>(n)
        repeat(n) {
            result.add(0, fmt.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result
    }

    fun label(key: String, pattern: String = "EEE, d MMM"): String {
        val date = keyFormat().parse(key) ?: return key
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }
}
