package com.example.skybase.jm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

object JmDailyReminderScheduler {
    const val ACTION_DAILY_FLASHCARDS_REMINDER = "com.example.skybase.jm.action.DAILY_FLASHCARDS_REMINDER"
    const val EXTRA_LANGUAGE_FILTER = "extra_language_filter"
    const val EXTRA_LEVEL_FILTER = "extra_level_filter"

    private const val REQUEST_CODE_REMINDER = 4101

    fun updateSchedule(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        languageFilter: String,
        levelFilter: String
    ) {
        if (!enabled) {
            cancel(context)
            return
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val pendingIntent = createReminderPendingIntent(context, languageFilter, levelFilter)

        alarmManager.cancel(pendingIntent)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAtMillis(safeHour, safeMinute),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(createReminderPendingIntent(context, "", ""))
    }

    private fun createReminderPendingIntent(
        context: Context,
        languageFilter: String,
        levelFilter: String
    ): PendingIntent {
        val intent = Intent(context, JmDailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_FLASHCARDS_REMINDER
            putExtra(EXTRA_LANGUAGE_FILTER, languageFilter)
            putExtra(EXTRA_LEVEL_FILTER, levelFilter)
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerAtMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        return next
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
