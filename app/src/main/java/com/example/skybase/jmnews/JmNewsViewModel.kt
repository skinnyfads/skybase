package com.example.skybase.jmnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class JmNewsUiState(
    val items: List<FeedArticleItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null
)

class JmNewsViewModel : ViewModel() {
    private val repository = JmNewsRepository(JmNewsApiClient.service)
    private val _uiState = MutableStateFlow(JmNewsUiState())
    val uiState: StateFlow<JmNewsUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init {
        loadNextPage()
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

    companion object {
        private const val PAGE_SIZE = 20
    }
}
