package com.example.skybase.jm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun JmSettingsFragment(
    modifier: Modifier = Modifier,
    selectedLanguage: String,
    selectedLevel: String,
    selectedFlashcardDirection: JmFlashcardDirection,
    hideExampleMeaning: Boolean,
    onLanguageChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onFlashcardDirectionChange: (JmFlashcardDirection) -> Unit,
    onHideExampleMeaningChange: (Boolean) -> Unit,
    viewModel: JmSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(selectedLanguage) {
        if (selectedLanguage.isBlank()) {
            viewModel.clearLevels()
            if (selectedLevel.isNotBlank()) {
                onLevelChange("")
            }
        } else {
            viewModel.loadLevels(selectedLanguage)
        }
    }

    LaunchedEffect(
        selectedLanguage,
        selectedLevel,
        uiState.levels,
        uiState.loadedLevelsLanguage,
        uiState.isLoadingLevels
    ) {
        if (selectedLanguage.isBlank() || selectedLevel.isBlank()) return@LaunchedEffect
        if (uiState.isLoadingLevels) return@LaunchedEffect
        if (uiState.loadedLevelsLanguage != selectedLanguage) return@LaunchedEffect
        if (uiState.levels.none { it.name == selectedLevel }) {
            onLevelChange("")
        }
    }

    val languageOptions = buildList {
        add("" to "All Languages")
        uiState.languages.forEach { language ->
            add(language.name to language.name)
        }
    }
    val levelOptions = buildList {
        add("" to "All Levels")
        uiState.levels.forEach { level ->
            add(level.name to level.name)
        }
    }
    val directionOptions = JmFlashcardDirection.entries.map { it.value to it.label }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "JM Settings",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Configure feed and flashcard preferences.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsSelector(
            label = "Language",
            value = selectedLanguage,
            options = languageOptions,
            enabled = !uiState.isLoadingLanguages,
            onSelected = { language ->
                onLanguageChange(language)
                if (language.isBlank()) {
                    onLevelChange("")
                    viewModel.clearLevels()
                } else {
                    viewModel.loadLevels(language)
                }
            }
        )

        SettingsSelector(
            label = "Level",
            value = selectedLevel,
            options = levelOptions,
            enabled = selectedLanguage.isNotBlank() && !uiState.isLoadingLevels,
            onSelected = onLevelChange
        )

        SettingsSelector(
            label = "Flashcard direction",
            value = selectedFlashcardDirection.value,
            options = directionOptions,
            enabled = true,
            onSelected = { selected ->
                onFlashcardDirectionChange(JmFlashcardDirection.fromValue(selected))
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Hide example meaning",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Hide translations by default in flashcard examples.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = hideExampleMeaning,
                onCheckedChange = onHideExampleMeaningChange
            )
        }

        if (uiState.isLoadingLanguages || uiState.isLoadingLevels) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 2.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Loading options...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.errorMessage != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        viewModel.loadLanguages()
                        if (selectedLanguage.isNotBlank()) {
                            viewModel.loadLevels(selectedLanguage)
                        }
                    }
                ) {
                    Text(text = "Retry")
                }
            }
        }
    }
}

@Composable
private fun SettingsSelector(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == value }?.second ?: options.firstOrNull()?.second.orEmpty()

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedLabel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            onSelected(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
