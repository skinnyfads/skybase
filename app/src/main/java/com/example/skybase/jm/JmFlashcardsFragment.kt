package com.example.skybase.jm

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun JmFlashcardsFragment(
    modifier: Modifier = Modifier,
    viewModel: JmFlashcardsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentCard = uiState.deck.getOrNull(uiState.currentIndex)
    var examplesVocabId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentCard?.id) {
        if (examplesVocabId != null && examplesVocabId != currentCard?.id) {
            examplesVocabId = null
            viewModel.clearVocabularyActionError()
        }
    }

    val examplesVocabulary = examplesVocabId?.let { targetId ->
        uiState.deck.firstOrNull { card -> card.id == targetId }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoadingDeck -> {
                    FlashcardsLoadingState()
                }

                uiState.errorMessage != null && uiState.deck.isEmpty() -> {
                    FlashcardsErrorState(
                        message = uiState.errorMessage,
                        onRetry = viewModel::fetchNewBatch
                    )
                }

                uiState.deck.isEmpty() -> {
                    FlashcardsEmptyState()
                }

                uiState.deckFinished -> {
                    FlashcardsFinishedState(
                        onReshuffle = viewModel::reshuffleExisting,
                        onFetchNewBatch = viewModel::fetchNewBatch
                    )
                }

                currentCard != null -> {
                    FlashcardsDeckContent(
                        card = currentCard,
                        uiState = uiState,
                        onReveal = viewModel::revealCurrentCard,
                        onRate = viewModel::rateCard,
                        onOpenExamples = {
                            currentCard.id?.let { id ->
                                examplesVocabId = id
                                viewModel.clearVocabularyActionError()
                            }
                        }
                    )
                }
            }
        }

        if (examplesVocabulary != null) {
            FlashcardExamplesPopover(
                vocabulary = examplesVocabulary,
                isMutating = uiState.isMutatingVocabulary && uiState.mutatingVocabularyId == examplesVocabulary.id,
                actionError = uiState.vocabularyActionError,
                onDismiss = {
                    examplesVocabId = null
                    viewModel.clearVocabularyActionError()
                },
                onToggleDeleteRestore = {
                    val vocabId = examplesVocabulary.id ?: return@FlashcardExamplesPopover
                    viewModel.toggleVocabularyDeleted(vocabId)
                }
            )
        }
    }
}

@Composable
private fun FlashcardsLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading deck...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FlashcardsErrorState(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = message ?: "Failed to fetch flashcards.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun FlashcardsEmptyState() {
    Text(
        text = "No vocabulary found for this language.",
        modifier = Modifier.padding(top = 80.dp),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun FlashcardsDeckContent(
    card: JmVocabularyResponse,
    uiState: JmFlashcardsUiState,
    onReveal: () -> Unit,
    onRate: (JmFlashcardRating) -> Unit,
    onOpenExamples: () -> Unit
) {
    val cardsRemaining = (uiState.deck.size - uiState.currentIndex).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 512.dp)
            .padding(top = 32.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        key(card.id ?: "card-${uiState.currentIndex}") {
            FlashcardCard(
                card = card,
                isFlipped = uiState.isFlipped,
                onFlip = onReveal,
                onOpenExamples = onOpenExamples
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!uiState.isFlipped) {
                Button(
                    onClick = onReveal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "Show Answer")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingButton(
                        label = "Again",
                        shortcutLabel = "1",
                        onClick = { onRate(JmFlashcardRating.AGAIN) },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                    RatingButton(
                        label = "Hard",
                        shortcutLabel = "2",
                        onClick = { onRate(JmFlashcardRating.HARD) },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    RatingButton(
                        label = "Good",
                        shortcutLabel = "3",
                        onClick = { onRate(JmFlashcardRating.GOOD) },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                    RatingButton(
                        label = "Easy",
                        shortcutLabel = "4",
                        onClick = { onRate(JmFlashcardRating.EASY) },
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0x2232CD32),
                        contentColor = Color(0xFF2E8B57)
                    )
                }
            }
        }

        Text(
            text = "Cards remaining: $cardsRemaining",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun FlashcardCard(
    card: JmVocabularyResponse,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onOpenExamples: () -> Unit
) {
    val word = card.word.orEmpty().ifBlank { "-" }
    val reading = card.readings.filter { it.isNotBlank() }.joinToString(", ")
    val meaning = card.meanings.filter { it.isNotBlank() }.joinToString("; ")
    val hasExamples = card.examples.isNotEmpty()
    val backAlpha by animateFloatAsState(
        targetValue = if (isFlipped) 1f else 0f,
        label = "flashcard-back-alpha"
    )
    val cardInteraction = remember { MutableInteractionSource() }
    val wordInteraction = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 384.dp)
            .heightIn(min = 300.dp)
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = onFlip
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = if (isFlipped && hasExamples) {
                        Modifier.clickable(
                            interactionSource = wordInteraction,
                            indication = null,
                            onClick = onOpenExamples
                        )
                    } else {
                        Modifier
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(backAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (reading.isNotBlank() || meaning.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.width(64.dp),
                            thickness = 2.dp
                        )
                    }

                    if (reading.isNotBlank()) {
                        Text(
                            text = reading,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (meaning.isNotBlank()) {
                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (!isFlipped) {
                Text(
                    text = "Tap to reveal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    shortcutLabel: String,
    onClick: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = shortcutLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FlashcardsFinishedState(
    onReshuffle: () -> Unit,
    onFetchNewBatch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Deck Finished!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "You've reached the end of this random vocabulary batch. What would you like to do next?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReshuffle,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Reshuffle Existing")
            }
            Button(
                onClick = onFetchNewBatch,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Fetch New Batch")
            }
        }
    }
}

@Composable
private fun FlashcardExamplesPopover(
    vocabulary: JmVocabularyResponse,
    isMutating: Boolean,
    actionError: String?,
    onDismiss: () -> Unit,
    onToggleDeleteRestore: () -> Unit
) {
    val reading = vocabulary.readings.filter { it.isNotBlank() }.joinToString(", ")
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 448.dp)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = vocabulary.word.orEmpty().ifBlank { "-" },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (reading.isNotBlank()) {
                            Text(
                                text = reading,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, enabled = !isMutating) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close examples"
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (vocabulary.examples.isEmpty()) {
                        Text(
                            text = "No examples available.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        vocabulary.examples.forEach { example ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = example.sentence.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                example.meaning?.takeIf { it.isNotBlank() }?.let { meaning ->
                                    Text(
                                        text = meaning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (actionError != null) {
                        Text(
                            text = actionError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onToggleDeleteRestore,
                        enabled = !isMutating && vocabulary.id != null
                    ) {
                        if (isMutating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = if (vocabulary.deleted) "Restore Vocabulary" else "Delete Vocabulary")
                        }
                    }
                }
            }
        }
    }
}
