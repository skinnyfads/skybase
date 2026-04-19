package com.example.skybase.jm

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.random.Random

@Composable
fun JmFlashcardsFragment(
    modifier: Modifier = Modifier,
    languageFilter: String = "",
    levelFilter: String = "",
    flashcardDirection: JmFlashcardDirection = JmFlashcardDirection.WORD_FIRST,
    hideExampleMeaning: Boolean = false,
    viewModel: JmFlashcardsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentCard = uiState.deck.getOrNull(uiState.currentIndex)
    var examplesVocabId by rememberSaveable { mutableStateOf<String?>(null) }
    var isDeckOverviewVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(languageFilter, levelFilter) {
        viewModel.applyFilters(
            language = languageFilter,
            level = levelFilter
        )
    }

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
                        flashcardDirection = flashcardDirection,
                        onReveal = viewModel::revealCurrentCard,
                        onRate = viewModel::rateCard,
                        onOpenExamples = {
                            isDeckOverviewVisible = false
                            currentCard.id?.let { id ->
                                examplesVocabId = id
                                viewModel.clearVocabularyActionError()
                            }
                        },
                        onOpenDeckOverview = {
                            examplesVocabId = null
                            viewModel.clearVocabularyActionError()
                            isDeckOverviewVisible = true
                        }
                    )
                }
            }
        }
        if (examplesVocabulary != null) {
            FlashcardExamplesPopover(
                vocabulary = examplesVocabulary,
                hideExampleMeaning = hideExampleMeaning,
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

        if (isDeckOverviewVisible && uiState.deck.isNotEmpty()) {
            FlashcardsDeckOverviewPopover(
                deck = uiState.deck,
                currentIndex = uiState.currentIndex,
                onDismiss = { isDeckOverviewVisible = false }
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
    flashcardDirection: JmFlashcardDirection,
    onReveal: () -> Unit,
    onRate: (JmFlashcardRating) -> Unit,
    onOpenExamples: () -> Unit,
    onOpenDeckOverview: () -> Unit
) {
    val cardsRemaining = (uiState.deck.size - uiState.currentIndex).coerceAtLeast(0)
    val randomDirectionToggle = remember { mutableMapOf<String, Boolean>() }
    val resolvedDirection = remember(card.id, uiState.currentIndex, flashcardDirection) {
        if (flashcardDirection == JmFlashcardDirection.RANDOM && card.id != null) {
            val lastWasWordFirst = randomDirectionToggle[card.id]
            val useWordFirst = if (lastWasWordFirst == null) {
                kotlin.random.Random.nextBoolean()
            } else {
                !lastWasWordFirst
            }
            randomDirectionToggle[card.id!!] = useWordFirst
            if (useWordFirst) JmFlashcardDirection.WORD_FIRST else JmFlashcardDirection.MEANING_FIRST
        } else {
            flashcardDirection
        }
    }

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
                direction = resolvedDirection,
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
                        .height(64.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Show Answer",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingButton(
                        label = "Again",
                        onClick = { onRate(JmFlashcardRating.AGAIN) },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                    RatingButton(
                        label = "Hard",
                        onClick = { onRate(JmFlashcardRating.HARD) },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    RatingButton(
                        label = "Good",
                        onClick = { onRate(JmFlashcardRating.GOOD) },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                    RatingButton(
                        label = "Easy",
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
            modifier = Modifier
                .align(Alignment.End)
                .clickable(onClick = onOpenDeckOverview)
        )
    }
}

@Composable
private fun FlashcardCard(
    card: JmVocabularyResponse,
    isFlipped: Boolean,
    direction: JmFlashcardDirection,
    onFlip: () -> Unit,
    onOpenExamples: () -> Unit
) {
    val word = card.word.orEmpty().ifBlank { "-" }
    val reading = card.readings.filter { it.isNotBlank() }.joinToString(", ")
    val meaning = card.meanings.filter { it.isNotBlank() }.joinToString("; ")
    val hasExamples = card.examples.isNotEmpty()
    val frontType = when (direction) {
        JmFlashcardDirection.MEANING_FIRST -> if (meaning.isNotBlank()) "meaning" else "word"
        JmFlashcardDirection.READING_FIRST -> if (reading.isNotBlank()) "reading" else "word"
        else -> "word"
    }
    val canOpenExamples = isFlipped && hasExamples
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
                when (frontType) {
                    "meaning" -> {
                        Text(
                            text = meaning.ifBlank { "-" },
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    "reading" -> {
                        Text(
                            text = reading.ifBlank { word },
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = if (canOpenExamples) {
                                Modifier.clickable(
                                    interactionSource = wordInteraction,
                                    indication = null,
                                    onClick = onOpenExamples
                                )
                            } else {
                                Modifier
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(backAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val showBackWord = frontType != "word"
                    val showBackReading = frontType != "reading" && reading.isNotBlank()
                    val showBackMeaning = frontType != "meaning" && meaning.isNotBlank()
                    val showDivider = showBackWord || showBackReading || showBackMeaning

                    if (showDivider) {
                        HorizontalDivider(
                            modifier = Modifier.width(64.dp),
                            thickness = 2.dp
                        )
                    }

                    if (showBackWord) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = if (canOpenExamples) {
                                Modifier.clickable(
                                    interactionSource = wordInteraction,
                                    indication = null,
                                    onClick = onOpenExamples
                                )
                            } else {
                                Modifier
                            }
                        )
                    }

                    if (showBackReading) {
                        Text(
                            text = reading,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (showBackMeaning) {
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
    onClick: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
private fun FlashcardsDeckOverviewPopover(
    deck: List<JmVocabularyResponse>,
    currentIndex: Int,
    onDismiss: () -> Unit
) {
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
                            text = "Current Flashcards",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${deck.size} cards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close flashcards list"
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
                    deck.forEachIndexed { index, vocab ->
                        val word = vocab.word.orEmpty().ifBlank { "-" }
                        val meaning = vocab.meanings.filter { it.isNotBlank() }.joinToString("; ").ifBlank { "-" }
                        val isCurrentCard = index == currentIndex

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. $word",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurrentCard) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isCurrentCard) {
                                    Text(
                                        text = "Current",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = meaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index != deck.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun FlashcardExamplesPopover(
    vocabulary: JmVocabularyResponse,
    hideExampleMeaning: Boolean,
    isMutating: Boolean,
    actionError: String?,
    onDismiss: () -> Unit,
    onToggleDeleteRestore: () -> Unit
) {
    val reading = vocabulary.readings.filter { it.isNotBlank() }.joinToString(", ")
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    var revealedMeaningIndex by remember(vocabulary.id, hideExampleMeaning) {
        mutableStateOf<Int?>(null)
    }

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
                        vocabulary.examples.forEachIndexed { index, example ->
                            val isMeaningVisible = !hideExampleMeaning || revealedMeaningIndex == index
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (hideExampleMeaning) {
                                            Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    revealedMeaningIndex = if (revealedMeaningIndex == index) null else index
                                                }
                                            )
                                        } else {
                                            Modifier
                                        }
                                    ),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = example.sentence.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                example.meaning?.takeIf { it.isNotBlank() && isMeaningVisible }?.let { meaning ->
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
