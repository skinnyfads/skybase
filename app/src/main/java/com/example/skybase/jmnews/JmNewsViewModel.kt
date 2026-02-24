package com.example.skybase.jmnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import com.example.skybase.vocabulary.VocabularyRepository
import retrofit2.HttpException

data class JmNewsUiState(
    val items: List<FeedArticleItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null,
    val selectedArticleId: String? = null,
    val articleDetail: ArticleDetailResponse? = null,
    val isLoadingArticle: Boolean = false,
    val articleErrorMessage: String? = null,
    val isAddingVocabulary: Boolean = false,
    val addingVocabularyKey: String? = null,
    val addedVocabularyKeys: Set<String> = emptySet(),
    val addVocabularyError: String? = null,
    val addVocabularySuccess: Boolean = false
)

class JmNewsViewModel : ViewModel() {
    private val repository = JmNewsRepository(JmNewsApiClient.service)
    private val vocabularyRepository = VocabularyRepository()
    private val _uiState = MutableStateFlow(JmNewsUiState())
    val uiState: StateFlow<JmNewsUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init {
        loadNextPage()
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
                    limit = PAGE_SIZE
                )
                val hasNext = response.pagination?.hasNext ?: (response.items.size >= PAGE_SIZE)
                _uiState.update {
                    it.copy(
                        items = it.items + response.items,
                        isLoading = false,
                        hasNextPage = hasNext,
                        errorMessage = null
                    )
                }
                currentPage += 1
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

    private fun loadArticle(articleId: String) {
        viewModelScope.launch {
            try {
                val response = repository.fetchArticle(articleId)
                _uiState.update {
                    it.copy(
                        articleDetail = response,
                        isLoadingArticle = false,
                        articleErrorMessage = null
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

    fun addVocabulary(dictKey: String) {
        if (dictKey.isBlank()) return
        if (_uiState.value.addedVocabularyKeys.contains(dictKey)) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAddingVocabulary = true,
                    addingVocabularyKey = dictKey,
                    addVocabularyError = null,
                    addVocabularySuccess = false
                )
            }
            try {
                vocabularyRepository.addVocabulary(dictKey = dictKey)
                _uiState.update {
                    it.copy(
                        isAddingVocabulary = false,
                        addingVocabularyKey = null,
                        addedVocabularyKeys = it.addedVocabularyKeys + dictKey,
                        addVocabularySuccess = true
                    )
                }
            } catch (exception: HttpException) {
                val message = if (exception.code() == 400) {
                    "Cannot add vocabulary: dictKey must match a JMDict headword or reading."
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
        _uiState.update { it.copy(addVocabularySuccess = false, addVocabularyError = null) }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
