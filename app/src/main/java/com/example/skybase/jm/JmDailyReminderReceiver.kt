package com.example.skybase.jm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.skybase.EXTRA_OPEN_JM_SUBMENU
import com.example.skybase.EXTRA_OPEN_TAB
import com.example.skybase.MainActivity
import com.example.skybase.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JmDailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != JmDailyReminderScheduler.ACTION_DAILY_FLASHCARDS_REMINDER) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        ensureNotificationChannel(context)

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val language = intent.getStringExtra(JmDailyReminderScheduler.EXTRA_LANGUAGE_FILTER).orEmpty()
        val level = intent.getStringExtra(JmDailyReminderScheduler.EXTRA_LEVEL_FILTER).orEmpty()
        val contentPendingIntent = createContentIntent(context)

        NotificationManagerCompat.from(context).notify(
            notificationId,
            buildLoadingNotification(context, contentPendingIntent)
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = JmRepository(JmApiClient.service)
                val normalizedLanguage = normalizeFilter(language)
                val normalizedLevel = if (normalizedLanguage == null) null else normalizeFilter(level)
                val vocabularies = repository.fetchRandomVocabularies(
                    limit = DAILY_VOCAB_LIMIT,
                    language = normalizedLanguage,
                    level = normalizedLevel,
                    withExamples = false
                ).take(DAILY_VOCAB_LIMIT)

                val notification = buildVocabNotification(
                    context = context,
                    vocabularies = vocabularies,
                    contentPendingIntent = contentPendingIntent
                )

                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (_: Exception) {
                val fallback = buildFallbackNotification(context, contentPendingIntent)
                NotificationManagerCompat.from(context).notify(notificationId, fallback)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildLoadingNotification(
        context: Context,
        contentPendingIntent: PendingIntent
    ): android.app.Notification {
        val text = "Preparing..."

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Daily JM Flashcards")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    private fun buildVocabNotification(
        context: Context,
        vocabularies: List<JmVocabularyResponse>,
        contentPendingIntent: PendingIntent
    ): android.app.Notification {
        val vocabLines = vocabularies.mapIndexed { index, vocabulary ->
            formatVocabLine(index, vocabulary)
        }

        if (vocabLines.isEmpty()) {
            return buildFallbackNotification(context, contentPendingIntent)
        }

        val bigText = buildString {
            vocabLines.forEach { line ->
                append(line)
                append('\n')
            }
        }.trimEnd()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Daily JM Flashcards")
            .setContentText(vocabLines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    private fun buildFallbackNotification(
        context: Context,
        contentPendingIntent: PendingIntent
    ): android.app.Notification {
        val text = "Could not load vocab list. Tap to open flashcards."

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Daily JM Flashcards")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    private fun createContentIntent(context: Context): PendingIntent {
        val openFlashcardsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, 2)
            putExtra(EXTRA_OPEN_JM_SUBMENU, JmSubmenu.FLASHCARDS.ordinal)
        }

        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_FLASHCARDS,
            openFlashcardsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatVocabLine(index: Int, vocabulary: JmVocabularyResponse): String {
        val word = vocabulary.word.orEmpty().ifBlank { "-" }
        val meaning = vocabulary.meanings.firstOrNull { it.isNotBlank() } ?: "-"
        return "${index + 1}. $word - $meaning"
    }

    private fun normalizeFilter(value: String): String? {
        val trimmed = value.trim()
        return trimmed.takeIf { it.isNotEmpty() }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "JM Flashcards",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily reminders for JM flashcard practice"
        }

        manager.createNotificationChannel(channel)
    }

    private fun buildFilterText(language: String, level: String): String {
        val languageLabel = if (language.isBlank()) "All languages" else language
        val levelLabel = if (level.isBlank()) "All levels" else level
        return "$languageLabel, $levelLabel"
    }

    private companion object {
        const val CHANNEL_ID = "jm_flashcards_daily"
        const val REQUEST_CODE_OPEN_FLASHCARDS = 4102
        const val DAILY_VOCAB_LIMIT = 5
    }
}
