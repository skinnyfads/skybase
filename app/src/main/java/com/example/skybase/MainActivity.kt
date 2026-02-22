package com.example.skybase

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.skybase.jmnews.JmNewsFragment
import com.example.skybase.ui.theme.SkyBaseTheme
import com.example.skybase.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeModeIndex = preferences
            .getInt(THEME_MODE_INDEX_KEY, ThemeMode.DARK.ordinal)
            .coerceIn(0, ThemeMode.entries.lastIndex)

        setContent {
            var selectedThemeModeIndex by rememberSaveable {
                mutableIntStateOf(savedThemeModeIndex)
            }
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            val themeMode = ThemeMode.entries[selectedThemeModeIndex]

            SkyBaseTheme(
                themeMode = themeMode,
                dynamicColor = themeMode != ThemeMode.AMOLED
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        ThemeMenuBar(
                            selectedMode = themeMode,
                            onModeSelected = { mode ->
                                val modeIndex = mode.ordinal
                                selectedThemeModeIndex = modeIndex
                                preferences.edit()
                                    .putInt(THEME_MODE_INDEX_KEY, modeIndex)
                                    .apply()
                            }
                        )
                    },
                    bottomBar = {
                        BottomNavBar(
                            selectedItem = selectedTab,
                            onItemSelected = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        when (selectedTab) {
                            0 -> JmNewsFragment(modifier = Modifier.fillMaxSize())
                            1 -> com.example.skybase.learning.LearningFragment(modifier = Modifier.fillMaxSize())
                            else -> Text(
                                text = "Learning Social",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val THEME_PREFS_NAME = "skybase_theme_prefs"
        const val THEME_MODE_INDEX_KEY = "theme_mode_index"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ThemeMenuBar(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = "SkyBase") },
        actions = {
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
            }
        }
    )
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
        Icons.Filled.School to "Learning Social"
    )

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.first,
                        contentDescription = item.second
                    )
                },
                label = {},
                alwaysShowLabel = false
            )
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
