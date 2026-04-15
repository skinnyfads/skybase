package com.example.skybase.jm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max

enum class JmFlashcardRating {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

data class JmFlashcardsUiState(
    val isLoadingDeck: Boolean = false,
    val errorMessage: String? = null,
    val deck: List<JmVocabularyResponse> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val deckFinished: Boolean = false,
    val isMutatingVocabulary: Boolean = false,
    val mutatingVocabularyId: String? = null,
    val vocabularyActionError: String? = null
)

class JmFlashcardsViewModel : ViewModel() {
    private val repository = JmRepository(JmApiClient.service)
    private val _uiState = MutableStateFlow(JmFlashcardsUiState())
    val uiState: StateFlow<JmFlashcardsUiState> = _uiState.asStateFlow()

    init {
        fetchNewBatch()
    }

    fun fetchNewBatch() {
        if (_uiState.value.isLoadingDeck) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDeck = true,
                    errorMessage = null,
                    vocabularyActionError = null
                )
            }
            try {
                val randomDeck = repository.fetchRandomVocabularies(
                    limit = RANDOM_DECK_LIMIT,
                    withExamples = true
                ).shuffled()
                _uiState.update {
                    it.copy(
                        isLoadingDeck = false,
                        deck = randomDeck,
                        currentIndex = 0,
                        isFlipped = false,
                        deckFinished = false,
                        isMutatingVocabulary = false,
                        errorMessage = null,
                        mutatingVocabularyId = null,
                        vocabularyActionError = null
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDeck = false,
                        isMutatingVocabulary = false,
                        mutatingVocabularyId = null,
                        errorMessage = "Failed to fetch flashcards."
                    )
                }
            }
        }
    }

    fun reshuffleExisting() {
        val state = _uiState.value
        if (state.deck.isEmpty()) {
            fetchNewBatch()
            return
        }
        _uiState.update {
            it.copy(
                deck = it.deck.shuffled(),
                currentIndex = 0,
                isFlipped = false,
                deckFinished = false,
                mutatingVocabularyId = null,
                vocabularyActionError = null
            )
        }
    }

    fun revealCurrentCard() {
        val state = _uiState.value
        if (state.deckFinished || state.isFlipped) return
        if (state.deck.getOrNull(state.currentIndex) == null) return
        _uiState.update { it.copy(isFlipped = true) }
    }

    fun rateCard(rating: JmFlashcardRating) {
        val state = _uiState.value
        if (!state.isFlipped || state.deckFinished) return

        if (state.currentIndex >= state.deck.lastIndex && rating != JmFlashcardRating.AGAIN) {
            _uiState.update {
                it.copy(
                    deckFinished = true,
                    isFlipped = false,
                    vocabularyActionError = null
                )
            }
            return
        }

        val currentCard = state.deck.getOrNull(state.currentIndex) ?: return
        val updatedDeck = state.deck.toMutableList()
        var nextIndex = state.currentIndex

        when (rating) {
            JmFlashcardRating.EASY -> {
                nextIndex += 1
            }

            JmFlashcardRating.AGAIN,
            JmFlashcardRating.HARD,
            JmFlashcardRating.GOOD -> {
                updatedDeck.removeAt(state.currentIndex)
                val remaining = (updatedDeck.size - state.currentIndex).coerceAtLeast(0)
                val offset = when (rating) {
                    JmFlashcardRating.AGAIN -> 2
                    JmFlashcardRating.HARD -> max(3, floor(remaining * 0.2).toInt())
                    JmFlashcardRating.GOOD -> max(5, floor(remaining * 0.6).toInt())
                    JmFlashcardRating.EASY -> 0
                }
                val insertIndex = (state.currentIndex + offset).coerceAtMost(updatedDeck.size)
                updatedDeck.add(insertIndex, currentCard)
            }
        }

        val finished = updatedDeck.isEmpty() || nextIndex > updatedDeck.lastIndex
        val safeIndex = if (updatedDeck.isEmpty()) 0 else nextIndex.coerceIn(0, updatedDeck.lastIndex)
        _uiState.update {
            it.copy(
                deck = updatedDeck,
                currentIndex = safeIndex,
                isFlipped = false,
                deckFinished = finished,
                vocabularyActionError = null
            )
        }
    }

    fun clearVocabularyActionError() {
        _uiState.update { it.copy(vocabularyActionError = null) }
    }

    fun toggleVocabularyDeleted(vocabId: String) {
        val state = _uiState.value
        if (state.isMutatingVocabulary) return
        val vocab = state.deck.firstOrNull { it.id == vocabId } ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isMutatingVocabulary = true,
                    mutatingVocabularyId = vocabId,
                    vocabularyActionError = null
                )
            }

            val isDeleted = vocab.deleted
            try {
                if (isDeleted) {
                    val word = vocab.word?.takeIf { value -> value.isNotBlank() }
                    val language = vocab.language?.takeIf { value -> value.isNotBlank() }
                    if (word == null || language == null) {
                        _uiState.update {
                            it.copy(
                                isMutatingVocabulary = false,
                                mutatingVocabularyId = null,
                                vocabularyActionError = "Cannot restore this vocabulary."
                            )
                        }
                        return@launch
                    }
                    repository.addVocabulary(word = word, language = language)
                    _uiState.update {
                        it.copy(
                            isMutatingVocabulary = false,
                            mutatingVocabularyId = null,
                            deck = it.deck.map { item ->
                                if (item.id == vocabId) item.copy(deleted = false) else item
                            },
                            vocabularyActionError = null
                        )
                    }
                } else {
                    val success = repository.deleteVocabulary(vocabId)
                    if (success) {
                        _uiState.update {
                            it.copy(
                                isMutatingVocabulary = false,
                                mutatingVocabularyId = null,
                                deck = it.deck.map { item ->
                                    if (item.id == vocabId) item.copy(deleted = true) else item
                                },
                                vocabularyActionError = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isMutatingVocabulary = false,
                                mutatingVocabularyId = null,
                                vocabularyActionError = "Failed to delete vocabulary."
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isMutatingVocabulary = false,
                        mutatingVocabularyId = null,
                        vocabularyActionError = "Failed to update vocabulary."
                    )
                }
            }
        }
    }

    private companion object {
        const val RANDOM_DECK_LIMIT = 10
    }
}
