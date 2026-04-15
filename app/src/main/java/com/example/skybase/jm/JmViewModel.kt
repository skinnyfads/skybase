package com.example.skybase.jm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class JmUiState(
    val items: List<JmFeedArticleItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null,
    val selectedArticleId: String? = null,
    val articleDetail: JmArticleDetail? = null,
    val isLoadingArticle: Boolean = false,
    val articleErrorMessage: String? = null,
    val isAddingVocabulary: Boolean = false,
    val addingVocabularyKey: String? = null,
    val addedVocabularyKeys: Set<String> = emptySet(),
    val addVocabularyError: String? = null,
    val addVocabularySuccess: Boolean = false
)

class JmViewModel : ViewModel() {
    private val repository = JmRepository(JmApiClient.service)
    private val _uiState = MutableStateFlow(JmUiState())
    val uiState: StateFlow<JmUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var currentLanguageFilter: String? = null
    private var currentLevelFilter: String? = null

    init {
        loadNextPage()
    }

    fun applyFilters(language: String, level: String) {
        val normalizedLanguage = normalizeFilter(language)
        val normalizedLevel = normalizeFilter(level)
        val nextLevel = if (normalizedLanguage == null) null else normalizedLevel
        if (currentLanguageFilter == normalizedLanguage && currentLevelFilter == nextLevel) return

        currentLanguageFilter = normalizedLanguage
        currentLevelFilter = nextLevel
        reloadFeedForFilters()
    }

    fun openArticle(articleId: String) {
        if (articleId.isBlank()) return
        _uiState.update {
            it.copy(
                selectedArticleId = articleId,
                articleDetail = null,
                isLoadingArticle = true,
                articleErrorMessage = null
            )
        }
        loadArticle(articleId)
    }

    fun closeArticle() {
        _uiState.update {
            it.copy(
                selectedArticleId = null,
                articleDetail = null,
                isLoadingArticle = false,
                articleErrorMessage = null
            )
        }
    }

    fun retryLoadArticle() {
        val articleId = _uiState.value.selectedArticleId ?: return
        _uiState.update {
            it.copy(
                articleDetail = null,
                isLoadingArticle = true,
                articleErrorMessage = null
            )
        }
        loadArticle(articleId)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = repository.fetchFeed(
                    page = currentPage,
                    limit = PAGE_SIZE,
                    language = currentLanguageFilter,
                    level = currentLevelFilter
                )
                val pageValue = response.page.takeIf { it > 0 } ?: currentPage
                val limitValue = response.limit.takeIf { it > 0 } ?: PAGE_SIZE
                val hasNext = response.total > (pageValue * limitValue)

                _uiState.update {
                    it.copy(
                        items = it.items + response.items,
                        isLoading = false,
                        hasNextPage = hasNext,
                        errorMessage = null
                    )
                }
                currentPage = pageValue + 1
            } catch (exception: IOException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load feed. Check your connection and try again."
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load feed right now. Please try again."
                    )
                }
            }
        }
    }

    fun refreshFeed() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        currentPage = 1
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isRefreshing = true,
                    hasNextPage = true,
                    errorMessage = null
                )
            }
            try {
                val response = repository.fetchFeed(
                    page = currentPage,
                    limit = PAGE_SIZE,
                    language = currentLanguageFilter,
                    level = currentLevelFilter
                )
                val pageValue = response.page.takeIf { it > 0 } ?: currentPage
                val limitValue = response.limit.takeIf { it > 0 } ?: PAGE_SIZE
                val hasNext = response.total > (pageValue * limitValue)

                _uiState.update {
                    it.copy(
                        items = response.items,
                        isLoading = false,
                        isRefreshing = false,
                        hasNextPage = hasNext,
                        errorMessage = null
                    )
                }
                currentPage = pageValue + 1
            } catch (exception: IOException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Unable to load feed. Check your connection and try again."
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Unable to load feed right now. Please try again."
                    )
                }
            }
        }
    }

    private fun loadArticle(articleId: String) {
        viewModelScope.launch {
            try {
                val response = repository.fetchArticle(articleId)
                val language = response.language
                val existingAddedKeys = response.tokens.mapNotNull { token ->
                    val word = token.dictionaryForm?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    if (!token.addedToVocabulary || language.isNullOrBlank()) return@mapNotNull null
                    buildVocabularyKey(word = word, language = language)
                }.toSet()

                _uiState.update {
                    it.copy(
                        articleDetail = response,
                        isLoadingArticle = false,
                        articleErrorMessage = null,
                        addedVocabularyKeys = it.addedVocabularyKeys + existingAddedKeys
                    )
                }
            } catch (exception: IOException) {
                _uiState.update {
                    it.copy(
                        isLoadingArticle = false,
                        articleErrorMessage = "Unable to load article. Check your connection and try again."
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingArticle = false,
                        articleErrorMessage = "Unable to load article right now. Please try again."
                    )
                }
            }
        }
    }

    fun addVocabulary(word: String, language: String) {
        if (word.isBlank() || language.isBlank()) return
        val key = buildVocabularyKey(word = word, language = language)
        if (_uiState.value.addedVocabularyKeys.contains(key)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAddingVocabulary = true,
                    addingVocabularyKey = key,
                    addVocabularyError = null,
                    addVocabularySuccess = false
                )
            }
            try {
                repository.addVocabulary(word = word, language = language)
                _uiState.update {
                    it.copy(
                        isAddingVocabulary = false,
                        addingVocabularyKey = null,
                        addedVocabularyKeys = it.addedVocabularyKeys + key,
                        addVocabularySuccess = true
                    )
                }
            } catch (exception: HttpException) {
                val message = if (exception.code() == 400) {
                    "Cannot add vocabulary: invalid request."
                } else {
                    "Failed to add vocabulary. Please try again."
                }
                _uiState.update {
                    it.copy(
                        isAddingVocabulary = false,
                        addingVocabularyKey = null,
                        addVocabularyError = message
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isAddingVocabulary = false,
                        addingVocabularyKey = null,
                        addVocabularyError = "Failed to add vocabulary. Please try again."
                    )
                }
            }
        }
    }

    fun resetAddVocabularyState() {
        _uiState.update {
            it.copy(
                addVocabularySuccess = false,
                addVocabularyError = null
            )
        }
    }

    private fun buildVocabularyKey(word: String, language: String): String {
        return "${language.trim().lowercase()}::${word.trim()}"
    }

    private fun normalizeFilter(value: String): String? = value.trim().takeIf { it.isNotEmpty() }

    private fun reloadFeedForFilters() {
        if (_uiState.value.isLoading) return

        currentPage = 1
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    items = emptyList(),
                    selectedArticleId = null,
                    articleDetail = null,
                    isLoading = true,
                    isRefreshing = false,
                    hasNextPage = true,
                    errorMessage = null
                )
            }
            try {
                val response = repository.fetchFeed(
                    page = currentPage,
                    limit = PAGE_SIZE,
                    language = currentLanguageFilter,
                    level = currentLevelFilter
                )
                val pageValue = response.page.takeIf { it > 0 } ?: currentPage
                val limitValue = response.limit.takeIf { it > 0 } ?: PAGE_SIZE
                val hasNext = response.total > (pageValue * limitValue)

                _uiState.update {
                    it.copy(
                        items = response.items,
                        isLoading = false,
                        isRefreshing = false,
                        hasNextPage = hasNext,
                        errorMessage = null
                    )
                }
                currentPage = pageValue + 1
            } catch (exception: IOException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Unable to load feed. Check your connection and try again."
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Unable to load feed right now. Please try again."
                    )
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
