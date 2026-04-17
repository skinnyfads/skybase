package com.example.skybase.jm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.skybase.MainActivity

class JmDailyReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val preferences = context.getSharedPreferences(MainActivity.THEME_PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = preferences.getBoolean(MainActivity.JM_DAILY_REMINDER_ENABLED_KEY, false)
        val hour = preferences.getInt(MainActivity.JM_DAILY_REMINDER_HOUR_KEY, 20)
        val minute = preferences.getInt(MainActivity.JM_DAILY_REMINDER_MINUTE_KEY, 0)
        val language = preferences.getString(MainActivity.JM_LANGUAGE_KEY, "").orEmpty()
        val level = preferences.getString(MainActivity.JM_LEVEL_KEY, "").orEmpty()

        JmDailyReminderScheduler.updateSchedule(
            context = context,
            enabled = enabled,
            hour = hour,
            minute = minute,
            languageFilter = language,
            levelFilter = level
        )
    }
}
