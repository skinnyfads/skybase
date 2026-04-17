package com.example.skybase.jm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class JmSubmenu(val label: String) {
    FEED("Feed"),
    FLASHCARDS("Flashcards"),
    SETTINGS("Settings")
}

@Composable
fun JmFragment(
    modifier: Modifier = Modifier,
    selectedSubmenu: JmSubmenu = JmSubmenu.FEED,
    languageFilter: String = "",
    levelFilter: String = "",
    flashcardDirection: JmFlashcardDirection = JmFlashcardDirection.WORD_FIRST,
    hideExampleMeaning: Boolean = false,
    dailyReminderEnabled: Boolean = false,
    dailyReminderHour: Int = 20,
    dailyReminderMinute: Int = 0,
    onLanguageFilterChange: (String) -> Unit = {},
    onLevelFilterChange: (String) -> Unit = {},
    onFlashcardDirectionChange: (JmFlashcardDirection) -> Unit = {},
    onHideExampleMeaningChange: (Boolean) -> Unit = {},
    onDailyReminderEnabledChange: (Boolean) -> Unit = {},
    onDailyReminderTimeChange: (Int, Int) -> Unit = { _, _ -> }
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (selectedSubmenu) {
            JmSubmenu.FEED -> {
                JmFeedFragment(
                    modifier = Modifier.fillMaxSize(),
                    languageFilter = languageFilter,
                    levelFilter = levelFilter
                )
            }

            JmSubmenu.FLASHCARDS -> {
                JmFlashcardsFragment(
                    modifier = Modifier.fillMaxSize(),
                    languageFilter = languageFilter,
                    levelFilter = levelFilter,
                    flashcardDirection = flashcardDirection,
                    hideExampleMeaning = hideExampleMeaning
                )
            }

            JmSubmenu.SETTINGS -> {
                JmSettingsFragment(
                    modifier = Modifier.fillMaxSize(),
                    selectedLanguage = languageFilter,
                    selectedLevel = levelFilter,
                    selectedFlashcardDirection = flashcardDirection,
                    hideExampleMeaning = hideExampleMeaning,
                    dailyReminderEnabled = dailyReminderEnabled,
                    dailyReminderHour = dailyReminderHour,
                    dailyReminderMinute = dailyReminderMinute,
                    onLanguageChange = onLanguageFilterChange,
                    onLevelChange = onLevelFilterChange,
                    onFlashcardDirectionChange = onFlashcardDirectionChange,
                    onHideExampleMeaningChange = onHideExampleMeaningChange,
                    onDailyReminderEnabledChange = onDailyReminderEnabledChange,
                    onDailyReminderTimeChange = onDailyReminderTimeChange
                )
            }
        }
    }
}
