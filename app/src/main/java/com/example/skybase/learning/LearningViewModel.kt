package com.example.skybase.learning

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
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

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LearningRepository(LearningApiClient.service)
    private val prefs = application.getSharedPreferences("learning_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    private var syncToken: String? = prefs.getString("sync_token", null)

    private var nextCursorBucket: Int? = null
    private var nextCursorRandomKey: Double? = null
    private var nextCursorId: String? = null

    private val seenPostsQueue = mutableSetOf<String>()
    private var isSyncingSeen = false

    init {
        viewModelScope.launch {
            if (syncToken == null) {
                try {
                    val token = repository.initSyncToken()
                    syncToken = token
                    prefs.edit().putString("sync_token", token).apply()
                } catch (e: Exception) {
                    // Ignore, will fetch posts without token
                }
            }
            loadNextPage()
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = repository.fetchPosts(
                    limit = PAGE_SIZE,
                    syncToken = syncToken,
                    cursorBucket = nextCursorBucket,
                    cursorRandomKey = nextCursorRandomKey,
                    cursorId = nextCursorId
                )

                val cursor = response.nextCursor
                nextCursorBucket = cursor?.bucket
                nextCursorRandomKey = cursor?.randomKey
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

    fun markPostAsSeen(postId: String) {
        val currentToken = syncToken ?: return
        seenPostsQueue.add(postId)
        if (!isSyncingSeen) {
            isSyncingSeen = true
            viewModelScope.launch {
                delay(3000) // Batch for 3 seconds
                val idsToSync = seenPostsQueue.toList()
                seenPostsQueue.clear()
                isSyncingSeen = false

                if (idsToSync.isNotEmpty()) {
                    try {
                        repository.markPostsAsSeen(currentToken, idsToSync)
                    } catch (e: Exception) {
                        seenPostsQueue.addAll(idsToSync)
                    }
                }
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
