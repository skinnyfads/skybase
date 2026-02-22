package com.example.skybase.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class LearningUiState(
    val items: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null
)

class LearningViewModel : ViewModel() {
    private val repository = LearningRepository(LearningApiClient.service)
    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    private var nextCursorScore: Double? = null
    private var nextCursorId: String? = null

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = repository.fetchPosts(
                    limit = PAGE_SIZE,
                    cursorScore = nextCursorScore,
                    cursorId = nextCursorId
                )

                val cursor = response.nextCursor
                nextCursorScore = cursor?.biasedScore
                nextCursorId = cursor?.id

                _uiState.update {
                    it.copy(
                        items = it.items + response.items,
                        isLoading = false,
                        hasNextPage = cursor != null,
                        errorMessage = null
                    )
                }
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
