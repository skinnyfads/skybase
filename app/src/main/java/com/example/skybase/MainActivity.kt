package com.example.skybase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.skybase.jm.JmDailyReminderScheduler
import com.example.skybase.jm.JmFlashcardDirection
import com.example.skybase.jm.JmFragment
import com.example.skybase.jm.JmSubmenu
import com.example.skybase.jmnews.JmNewsFragment
import com.example.skybase.ui.theme.SkyBaseTheme
import com.example.skybase.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeModeIndex = preferences
            .getInt(THEME_MODE_INDEX_KEY, ThemeMode.DARK.ordinal)
            .coerceIn(0, ThemeMode.entries.lastIndex)
        val savedTabIndex = preferences
            .getInt(LAST_TAB_INDEX_KEY, 0)
            .coerceIn(0, 2)
        val savedJmSubmenuIndex = preferences
            .getInt(LAST_JM_SUBMENU_INDEX_KEY, 0)
            .coerceIn(0, JmSubmenu.entries.lastIndex)
        val launchTabIndex = intent?.getIntExtra(EXTRA_OPEN_TAB, savedTabIndex)
            ?.coerceIn(0, 2)
            ?: savedTabIndex
        val launchJmSubmenuIndex = intent?.getIntExtra(EXTRA_OPEN_JM_SUBMENU, savedJmSubmenuIndex)
            ?.coerceIn(0, JmSubmenu.entries.lastIndex)
            ?: savedJmSubmenuIndex

        val savedJmLanguage = preferences.getString(JM_LANGUAGE_KEY, "").orEmpty()
        val savedJmLevel = preferences.getString(JM_LEVEL_KEY, "").orEmpty()
        val savedJmFlashcardDirection = JmFlashcardDirection.fromValue(
            preferences.getString(JM_FLASHCARD_DIRECTION_KEY, JmFlashcardDirection.WORD_FIRST.value)
        )
        val savedJmHideExampleMeaning = preferences.getBoolean(JM_HIDE_EXAMPLE_MEANING_KEY, false)
        val savedJmDailyReminderEnabled = preferences.getBoolean(JM_DAILY_REMINDER_ENABLED_KEY, false)
        val savedJmDailyReminderHour = preferences.getInt(JM_DAILY_REMINDER_HOUR_KEY, 20).coerceIn(0, 23)
        val savedJmDailyReminderMinute = preferences.getInt(JM_DAILY_REMINDER_MINUTE_KEY, 0).coerceIn(0, 59)
        val savedFocusMode = preferences.getBoolean(FOCUS_MODE_KEY, false)

        JmDailyReminderScheduler.updateSchedule(
            context = this,
            enabled = savedJmDailyReminderEnabled,
            hour = savedJmDailyReminderHour,
            minute = savedJmDailyReminderMinute,
            languageFilter = savedJmLanguage,
            levelFilter = savedJmLevel
        )

        setContent {
            var selectedThemeModeIndex by rememberSaveable {
                mutableIntStateOf(savedThemeModeIndex)
            }
            var selectedTab by rememberSaveable { mutableIntStateOf(launchTabIndex) }
            var selectedJmSubmenuIndex by rememberSaveable { mutableIntStateOf(launchJmSubmenuIndex) }
            var jmLanguageFilter by rememberSaveable { mutableStateOf(savedJmLanguage) }
            var jmLevelFilter by rememberSaveable { mutableStateOf(savedJmLevel) }
            var jmFlashcardDirectionValue by rememberSaveable {
                mutableStateOf(savedJmFlashcardDirection.value)
            }
            var jmHideExampleMeaning by rememberSaveable {
                mutableStateOf(savedJmHideExampleMeaning)
            }
            var jmDailyReminderEnabled by rememberSaveable {
                mutableStateOf(savedJmDailyReminderEnabled)
            }
            var jmDailyReminderHour by rememberSaveable {
                mutableIntStateOf(savedJmDailyReminderHour)
            }
            var jmDailyReminderMinute by rememberSaveable {
                mutableIntStateOf(savedJmDailyReminderMinute)
            }
            var focusMode by rememberSaveable { mutableStateOf(savedFocusMode) }

            val context = LocalContext.current
            val themeMode = ThemeMode.entries[selectedThemeModeIndex]
            val selectedJmSubmenu = JmSubmenu.entries[selectedJmSubmenuIndex]
            val jmFlashcardDirection = JmFlashcardDirection.fromValue(jmFlashcardDirectionValue)

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                val enabled = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                jmDailyReminderEnabled = enabled
                preferences.edit()
                    .putBoolean(JM_DAILY_REMINDER_ENABLED_KEY, enabled)
                    .apply()
                JmDailyReminderScheduler.updateSchedule(
                    context = context,
                    enabled = enabled,
                    hour = jmDailyReminderHour,
                    minute = jmDailyReminderMinute,
                    languageFilter = jmLanguageFilter,
                    levelFilter = jmLevelFilter
                )
            }

            val jmNewsListState = rememberLazyListState()
            val learningListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            SkyBaseTheme(
                themeMode = themeMode,
                dynamicColor = themeMode != ThemeMode.AMOLED
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        ThemeMenuBar(
                            selectedMode = themeMode,
                            selectedTab = selectedTab,
                            selectedJmSubmenu = selectedJmSubmenu,
                            onModeSelected = { mode ->
                                val modeIndex = mode.ordinal
                                selectedThemeModeIndex = modeIndex
                                preferences.edit()
                                    .putInt(THEME_MODE_INDEX_KEY, modeIndex)
                                    .apply()
                            },
                            onJmSubmenuSelected = { submenu ->
                                val submenuIndex = submenu.ordinal
                                selectedJmSubmenuIndex = submenuIndex
                                preferences.edit()
                                    .putInt(LAST_JM_SUBMENU_INDEX_KEY, submenuIndex)
                                    .apply()
                            },
                            focusMode = focusMode,
                            onFocusModeChanged = { enabled ->
                                focusMode = enabled
                                preferences.edit()
                                    .putBoolean(FOCUS_MODE_KEY, enabled)
                                    .apply()
                            }
                        )
                    },
                    bottomBar = {
                        if (!focusMode) {
                            BottomNavBar(
                                selectedItem = selectedTab,
                                onItemSelected = { index ->
                                    if (selectedTab == index) {
                                        coroutineScope.launch {
                                            when (index) {
                                                0 -> jmNewsListState.animateScrollToItem(0)
                                                1 -> learningListState.animateScrollToItem(0)
                                                2 -> Unit
                                            }
                                        }
                                    } else {
                                        selectedTab = index
                                        preferences.edit()
                                            .putInt(LAST_TAB_INDEX_KEY, index)
                                            .apply()
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        when (selectedTab) {
                            0 -> JmNewsFragment(modifier = Modifier.fillMaxSize(), listState = jmNewsListState)
                            1 -> com.example.skybase.learning.LearningFragment(modifier = Modifier.fillMaxSize(), listState = learningListState)
                            2 -> JmFragment(
                                modifier = Modifier.fillMaxSize(),
                                selectedSubmenu = selectedJmSubmenu,
                                languageFilter = jmLanguageFilter,
                                levelFilter = jmLevelFilter,
                                flashcardDirection = jmFlashcardDirection,
                                hideExampleMeaning = jmHideExampleMeaning,
                                dailyReminderEnabled = jmDailyReminderEnabled,
                                dailyReminderHour = jmDailyReminderHour,
                                dailyReminderMinute = jmDailyReminderMinute,
                                onLanguageFilterChange = { language ->
                                    jmLanguageFilter = language
                                    if (language.isBlank() && jmLevelFilter.isNotBlank()) {
                                        jmLevelFilter = ""
                                        preferences.edit()
                                            .putString(JM_LEVEL_KEY, "")
                                            .apply()
                                    }
                                    preferences.edit()
                                        .putString(JM_LANGUAGE_KEY, language)
                                        .apply()

                                    if (jmDailyReminderEnabled) {
                                        JmDailyReminderScheduler.updateSchedule(
                                            context = context,
                                            enabled = true,
                                            hour = jmDailyReminderHour,
                                            minute = jmDailyReminderMinute,
                                            languageFilter = jmLanguageFilter,
                                            levelFilter = jmLevelFilter
                                        )
                                    }
                                },
                                onLevelFilterChange = { level ->
                                    jmLevelFilter = level
                                    preferences.edit()
                                        .putString(JM_LEVEL_KEY, level)
                                        .apply()

                                    if (jmDailyReminderEnabled) {
                                        JmDailyReminderScheduler.updateSchedule(
                                            context = context,
                                            enabled = true,
                                            hour = jmDailyReminderHour,
                                            minute = jmDailyReminderMinute,
                                            languageFilter = jmLanguageFilter,
                                            levelFilter = jmLevelFilter
                                        )
                                    }
                                },
                                onFlashcardDirectionChange = { direction ->
                                    jmFlashcardDirectionValue = direction.value
                                    preferences.edit()
                                        .putString(JM_FLASHCARD_DIRECTION_KEY, direction.value)
                                        .apply()
                                },
                                onHideExampleMeaningChange = { hide ->
                                    jmHideExampleMeaning = hide
                                    preferences.edit()
                                        .putBoolean(JM_HIDE_EXAMPLE_MEANING_KEY, hide)
                                        .apply()
                                },
                                onDailyReminderEnabledChange = { enabled ->
                                    if (
                                        enabled &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        jmDailyReminderEnabled = enabled
                                        preferences.edit()
                                            .putBoolean(JM_DAILY_REMINDER_ENABLED_KEY, enabled)
                                            .apply()
                                        JmDailyReminderScheduler.updateSchedule(
                                            context = context,
                                            enabled = enabled,
                                            hour = jmDailyReminderHour,
                                            minute = jmDailyReminderMinute,
                                            languageFilter = jmLanguageFilter,
                                            levelFilter = jmLevelFilter
                                        )
                                    }
                                },
                                onDailyReminderTimeChange = { hour, minute ->
                                    jmDailyReminderHour = hour.coerceIn(0, 23)
                                    jmDailyReminderMinute = minute.coerceIn(0, 59)
                                    preferences.edit()
                                        .putInt(JM_DAILY_REMINDER_HOUR_KEY, jmDailyReminderHour)
                                        .putInt(JM_DAILY_REMINDER_MINUTE_KEY, jmDailyReminderMinute)
                                        .apply()

                                    if (jmDailyReminderEnabled) {
                                        JmDailyReminderScheduler.updateSchedule(
                                            context = context,
                                            enabled = true,
                                            hour = jmDailyReminderHour,
                                            minute = jmDailyReminderMinute,
                                            languageFilter = jmLanguageFilter,
                                            levelFilter = jmLevelFilter
                                        )
                                    }
                                }
                            )
                            else -> Text(
                                text = "JM",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val THEME_PREFS_NAME = "skybase_theme_prefs"
        const val THEME_MODE_INDEX_KEY = "theme_mode_index"
        const val LAST_TAB_INDEX_KEY = "last_tab_index"
        const val LAST_JM_SUBMENU_INDEX_KEY = "last_jm_submenu_index"
        const val JM_LANGUAGE_KEY = "jm_language"
        const val JM_LEVEL_KEY = "jm_level"
        const val JM_FLASHCARD_DIRECTION_KEY = "jm_flashcard_direction"
        const val JM_HIDE_EXAMPLE_MEANING_KEY = "jm_hide_example_meaning"
        const val JM_DAILY_REMINDER_ENABLED_KEY = "jm_daily_reminder_enabled"
        const val JM_DAILY_REMINDER_HOUR_KEY = "jm_daily_reminder_hour"
        const val JM_DAILY_REMINDER_MINUTE_KEY = "jm_daily_reminder_minute"
        const val FOCUS_MODE_KEY = "focus_mode"
    }
}

@Composable
fun ThemeMenuBar(
    selectedMode: ThemeMode,
    selectedTab: Int,
    selectedJmSubmenu: JmSubmenu,
    focusMode: Boolean,
    onModeSelected: (ThemeMode) -> Unit,
    onJmSubmenuSelected: (JmSubmenu) -> Unit,
    onFocusModeChanged: (Boolean) -> Unit
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedTab == 2) {
                Box(modifier = Modifier.width(48.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JmSubmenu.entries.forEach { submenu ->
                        FilterChip(
                            selected = selectedJmSubmenu == submenu,
                            onClick = { onJmSubmenuSelected(submenu) },
                            label = { Text(text = submenu.label) }
                        )
                    }
                }
            } else {
                Text(
                    text = "SkyBase",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
            }

            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Open menu"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    Text(
                        text = "Theme",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        ThemeIconButton(
                            icon = Icons.Filled.LightMode,
                            label = ThemeMode.LIGHT.label,
                            selected = selectedMode == ThemeMode.LIGHT,
                            onClick = {
                                onModeSelected(ThemeMode.LIGHT)
                                showMenu = false
                            }
                        )
                        ThemeIconButton(
                            icon = Icons.Filled.DarkMode,
                            label = ThemeMode.DARK.label,
                            selected = selectedMode == ThemeMode.DARK,
                            onClick = {
                                onModeSelected(ThemeMode.DARK)
                                showMenu = false
                            }
                        )
                        ThemeIconButton(
                            icon = Icons.Filled.Contrast,
                            label = ThemeMode.AMOLED.label,
                            selected = selectedMode == ThemeMode.AMOLED,
                            onClick = {
                                onModeSelected(ThemeMode.AMOLED)
                                showMenu = false
                            }
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Focus Mode")
                        Switch(
                            checked = focusMode,
                            onCheckedChange = {
                                onFocusModeChanged(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) {
                androidx.compose.material3.MaterialTheme.colorScheme.primary
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun BottomNavBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        Icons.Filled.Article to "JM News",
        Icons.Filled.School to "Learning Social",
        Icons.Filled.MenuBook to "JM"
    )

    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    androidx.compose.material3.Surface(
        color = androidx.compose.material3.NavigationBarDefaults.containerColor,
        tonalElevation = androidx.compose.material3.NavigationBarDefaults.Elevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding)
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                IconButton(onClick = { onItemSelected(index) }) {
                    Icon(
                        imageVector = item.first,
                        contentDescription = item.second,
                        tint = if (selectedItem == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    SkyBaseTheme(themeMode = ThemeMode.LIGHT) {
        Scaffold(bottomBar = { BottomNavBar(selectedItem = 0, onItemSelected = {}) }) {}
    }
}
