package com.example.skybase.jm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val addVocabularySuccess: Boolean = false,
    val currentArticleIndex: Int = -1,
    val feedTotal: Int = 0
)

class JmViewModel : ViewModel() {
    private val repository = JmRepository(JmApiClient.service)
    private val _uiState = MutableStateFlow(JmUiState())
    val uiState: StateFlow<JmUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var currentLanguageFilter: String? = null
    private var currentLevelFilter: String? = null
    private var feedJob: Job? = null
    private var pendingOpenNextAfterPageLoad = false

    private val articleCache = object : LinkedHashMap<String, JmArticleDetail>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, JmArticleDetail>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    private val preloadJobs = mutableMapOf<String, Job>()

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

        val currentIndex = _uiState.value.items.indexOfFirst { it.id == articleId }
        val cached = articleCache[articleId]

        if (cached != null) {
            val existingAddedKeys = extractAddedVocabularyKeys(cached)
            _uiState.update {
                it.copy(
                    selectedArticleId = articleId,
                    articleDetail = cached,
                    isLoadingArticle = false,
                    articleErrorMessage = null,
                    currentArticleIndex = currentIndex,
                    addedVocabularyKeys = it.addedVocabularyKeys + existingAddedKeys
                )
            }
            preloadSurroundingArticles(currentIndex)
            prefetchFeedPageIfNeeded(currentIndex)
            Log.d(TAG, "Article cache HIT: $articleId (index $currentIndex)")
        } else {
            _uiState.update {
                it.copy(
                    selectedArticleId = articleId,
                    articleDetail = null,
                    isLoadingArticle = true,
                    articleErrorMessage = null,
                    currentArticleIndex = currentIndex
                )
            }
            loadArticle(articleId)
            Log.d(TAG, "Article cache MISS: $articleId (index $currentIndex)")
        }
    }

    fun closeArticle() {
        _uiState.update {
            it.copy(
                selectedArticleId = null,
                articleDetail = null,
                isLoadingArticle = false,
                articleErrorMessage = null,
                currentArticleIndex = -1
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

    fun openNextArticle() {
        openAdjacentArticle(offset = 1)
    }

    fun openPreviousArticle() {
        openAdjacentArticle(offset = -1)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        feedJob = viewModelScope.launch {
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

                val isFirstPage = currentPage == 1
                _uiState.update {
                    it.copy(
                        items = it.items + response.items,
                        isLoading = false,
                        hasNextPage = hasNext,
                        feedTotal = response.total,
                        errorMessage = null
                    )
                }
                currentPage = pageValue + 1
                if (pendingOpenNextAfterPageLoad) {
                    pendingOpenNextAfterPageLoad = false
                    openAdjacentArticle(offset = 1)
                }
                if (isFirstPage) {
                    preloadTopArticles(count = TOP_PRELOAD_COUNT)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                pendingOpenNextAfterPageLoad = false
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load feed. Check your connection and try again."
                    )
                }
            } catch (exception: Exception) {
                pendingOpenNextAfterPageLoad = false
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

        pendingOpenNextAfterPageLoad = false
        currentPage = 1
        feedJob = viewModelScope.launch {
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
                        feedTotal = response.total,
                        errorMessage = null
                    )
                }
                currentPage = pageValue + 1
                articleCache.clear()
                cancelAllPreloads()
                preloadTopArticles(count = TOP_PRELOAD_COUNT)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                pendingOpenNextAfterPageLoad = false
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Unable to load feed. Check your connection and try again."
                    )
                }
            } catch (exception: Exception) {
                pendingOpenNextAfterPageLoad = false
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
                articleCache[articleId] = response

                val existingAddedKeys = extractAddedVocabularyKeys(response)
                val currentIndex = _uiState.value.items.indexOfFirst { it.id == articleId }

                _uiState.update {
                    it.copy(
                        articleDetail = response,
                        isLoadingArticle = false,
                        articleErrorMessage = null,
                        currentArticleIndex = currentIndex,
                        addedVocabularyKeys = it.addedVocabularyKeys + existingAddedKeys
                    )
                }

                preloadSurroundingArticles(currentIndex)
                prefetchFeedPageIfNeeded(currentIndex)
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

    private fun extractAddedVocabularyKeys(article: JmArticleDetail): Set<String> {
        val language = article.language ?: return emptySet()
        if (language.isBlank()) return emptySet()
        return article.tokens.mapNotNull { token ->
            val word = token.dictionaryForm?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!token.addedToVocabulary) return@mapNotNull null
            buildVocabularyKey(word = word, language = language)
        }.toSet()
    }

    private fun preloadSurroundingArticles(currentIndex: Int) {
        if (currentIndex < 0) return
        val state = _uiState.value
        val indicesToPreload = listOf(
            currentIndex + 1,
            currentIndex + 2,
            currentIndex - 1
        )
        for (index in indicesToPreload) {
            val articleId = state.items.getOrNull(index)?.id
            if (articleId.isNullOrBlank()) continue
            if (articleCache.containsKey(articleId)) continue
            preloadArticle(articleId)
        }
    }

    private fun preloadTopArticles(count: Int) {
        val state = _uiState.value
        val toPreload = state.items.take(count)
        for (article in toPreload) {
            val articleId = article.id
            if (articleId.isNullOrBlank()) continue
            if (articleCache.containsKey(articleId)) continue
            preloadArticle(articleId)
        }
    }

    private fun preloadArticle(articleId: String) {
        if (preloadJobs[articleId]?.isActive == true) return

        preloadJobs[articleId] = viewModelScope.launch {
            try {
                val response = repository.fetchArticle(articleId)
                articleCache[articleId] = response
                Log.d(TAG, "Preloaded article: $articleId")
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Exception) {
                Log.d(TAG, "Preload failed for article: $articleId")
            } finally {
                preloadJobs.remove(articleId)
            }
        }
    }

    private fun prefetchFeedPageIfNeeded(currentIndex: Int) {
        if (currentIndex < 0) return
        val state = _uiState.value
        if (currentIndex >= state.items.size - FEED_PREFETCH_THRESHOLD
            && state.hasNextPage
            && !state.isLoading
        ) {
            Log.d(TAG, "Prefetching next feed page (reading article at index $currentIndex, ${state.items.size} loaded)")
            loadNextPage()
        }
    }

    private fun cancelAllPreloads() {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
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

    private fun openAdjacentArticle(offset: Int) {
        val state = _uiState.value
        if (state.isLoadingArticle) return
        val selectedId = state.selectedArticleId ?: return
        val currentIndex = state.items.indexOfFirst { it.id == selectedId }
        if (currentIndex < 0) return

        val targetIndex = currentIndex + offset
        if (targetIndex !in state.items.indices) {
            if (offset > 0 && state.hasNextPage && !state.isLoading) {
                pendingOpenNextAfterPageLoad = true
                loadNextPage()
            } else {
                pendingOpenNextAfterPageLoad = false
            }
            return
        }

        val targetId = state.items[targetIndex].id?.takeIf { it.isNotBlank() } ?: return
        openArticle(targetId)
    }

    private fun normalizeFilter(value: String): String? = value.trim().takeIf { it.isNotEmpty() }

    private fun reloadFeedForFilters() {
        feedJob?.cancel()
        pendingOpenNextAfterPageLoad = false
        articleCache.clear()
        cancelAllPreloads()

        currentPage = 1
        feedJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    items = emptyList(),
                    selectedArticleId = null,
                    articleDetail = null,
                    isLoading = true,
                    isRefreshing = false,
                    hasNextPage = true,
                    errorMessage = null,
                    currentArticleIndex = -1,
                    feedTotal = 0
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
                        feedTotal = response.total,
                        errorMessage = null
                    )
                }
                currentPage = pageValue + 1
                if (pendingOpenNextAfterPageLoad) {
                    pendingOpenNextAfterPageLoad = false
                    openAdjacentArticle(offset = 1)
                }
                preloadTopArticles(count = TOP_PRELOAD_COUNT)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                pendingOpenNextAfterPageLoad = false
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Unable to load feed. Check your connection and try again."
                    )
                }
            } catch (exception: Exception) {
                pendingOpenNextAfterPageLoad = false
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
        const val MAX_CACHE_SIZE = 20
        const val TOP_PRELOAD_COUNT = 3
        const val FEED_PREFETCH_THRESHOLD = 4
        const val TAG = "JmViewModel"
    }
}
