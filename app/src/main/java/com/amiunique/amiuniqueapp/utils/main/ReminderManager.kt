package com.amiunique.amiuniqueapp.utils.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderManager {

    fun scheduleWeeklyReminder(
        context: Context,
        intialTime:String = "11:00",
        frequency:Int = 7  // days
    ) {
        val (hour, minute) = getHourAndMinute(intialTime)
        // The initial day/time to start from is today + frequency days at HH:MM
        val initialDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, frequency)
        }.time

        val calendar = Calendar.getInstance().apply {
            time = initialDay
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If today HH:MM is now/past, bump by frequency days until it's in the future
        val intent = Intent(context, WeeklyReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Set the repeating alarm
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * frequency,
            pi
        )
    }

    fun cancelWeeklyReminder(context: Context) {
        val intent = Intent(context, WeeklyReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }

    private fun getHourAndMinute(time:String): Pair<Int, Int> {
        val parts = time.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 11
            val minute = parts[1].toIntOrNull() ?: 0
            return Pair(hour, minute)
        }
        return Pair(11, 0) // Default to 11:00 if parsing fails
    }
}