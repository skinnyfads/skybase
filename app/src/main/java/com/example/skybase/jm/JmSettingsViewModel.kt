package com.example.skybase.jm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class JmSettingsUiState(
    val languages: List<JmLanguage> = emptyList(),
    val levels: List<JmDifficultyLevel> = emptyList(),
    val loadedLevelsLanguage: String? = null,
    val isLoadingLanguages: Boolean = false,
    val isLoadingLevels: Boolean = false,
    val errorMessage: String? = null
)

class JmSettingsViewModel : ViewModel() {
    private val repository = JmRepository(JmApiClient.service)
    private val _uiState = MutableStateFlow(JmSettingsUiState())
    val uiState: StateFlow<JmSettingsUiState> = _uiState.asStateFlow()

    private var lastLevelsLanguage: String? = null

    init {
        loadLanguages()
    }

    fun loadLanguages() {
        if (_uiState.value.isLoadingLanguages) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingLanguages = true,
                    errorMessage = null
                )
            }
            try {
                val languages = repository.fetchLanguages()
                    .filter { it.name.isNotBlank() }
                    .distinctBy { it.name }
                    .sortedBy { it.name.lowercase() }
                _uiState.update {
                    it.copy(
                        isLoadingLanguages = false,
                        languages = languages
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingLanguages = false,
                        errorMessage = "Failed to load settings options."
                    )
                }
            }
        }
    }

    fun loadLevels(language: String) {
        val normalizedLanguage = language.trim().takeIf { it.isNotEmpty() }
        if (normalizedLanguage == null) {
            clearLevels()
            return
        }
        if (_uiState.value.isLoadingLevels && lastLevelsLanguage == normalizedLanguage) return
        if (lastLevelsLanguage == normalizedLanguage && _uiState.value.levels.isNotEmpty()) return

        lastLevelsLanguage = normalizedLanguage
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingLevels = true,
                    errorMessage = null
                )
            }
            try {
                val levels = repository.fetchVocabLevels(language = normalizedLanguage)
                    .filter { it.name.isNotBlank() }
                    .sortedWith(
                        compareBy<JmDifficultyLevel> { it.rank ?: Int.MAX_VALUE }
                            .thenBy { it.name.lowercase() }
                    )
                _uiState.update {
                    it.copy(
                        isLoadingLevels = false,
                        levels = levels,
                        loadedLevelsLanguage = normalizedLanguage
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingLevels = false,
                        levels = emptyList(),
                        loadedLevelsLanguage = normalizedLanguage,
                        errorMessage = "Failed to load settings options."
                    )
                }
            }
        }
    }

    fun clearLevels() {
        lastLevelsLanguage = null
        _uiState.update {
            it.copy(
                levels = emptyList(),
                loadedLevelsLanguage = null,
                isLoadingLevels = false
            )
        }
    }
}
