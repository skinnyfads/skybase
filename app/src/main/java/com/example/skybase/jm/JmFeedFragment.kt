package com.example.skybase.jm

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JmFeedFragment(
    modifier: Modifier = Modifier,
    languageFilter: String = "",
    levelFilter: String = "",
    viewModel: JmViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(languageFilter, levelFilter) {
        viewModel.applyFilters(
            language = languageFilter,
            level = levelFilter
        )
    }

    LaunchedEffect(uiState.addVocabularySuccess, uiState.addVocabularyError) {
        if (uiState.addVocabularySuccess) {
            Toast.makeText(context, "Added to vocabulary", Toast.LENGTH_SHORT).show()
            viewModel.resetAddVocabularyState()
        }
        uiState.addVocabularyError?.let { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            viewModel.resetAddVocabularyState()
        }
    }

    val selectedArticleId = uiState.selectedArticleId
    if (selectedArticleId != null) {
        JmArticleDetail(
            modifier = modifier,
            uiState = uiState,
            onBack = viewModel::closeArticle,
            onRetry = viewModel::retryLoadArticle,
            onAddVocabulary = viewModel::addVocabulary,
            onOpenPreviousArticle = viewModel::openPreviousArticle,
            onOpenNextArticle = viewModel::openNextArticle
        )
        return
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalCount = listState.layoutInfo.totalItemsCount
            if (totalCount == 0) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalCount - 4
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.isLoading, uiState.hasNextPage) {
        if (shouldLoadMore && !uiState.isLoading && uiState.hasNextPage) {
            viewModel.loadNextPage()
        }
    }

    if (uiState.items.isEmpty() && uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshFeed,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = uiState.items.size,
                key = { index ->
                    val id = uiState.items[index].id
                    if (id.isNullOrBlank()) "article-$index" else "$id-$index"
                }
            ) { index ->
                val article = uiState.items[index]
                ArticleFeedCard(
                    article = article,
                    onClick = {
                        article.id?.takeIf { it.isNotBlank() }?.let(viewModel::openArticle)
                    }
                )
            }

            if (uiState.isLoading && uiState.items.isNotEmpty() && !uiState.isRefreshing) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                item(key = "error") {
                    ErrorCard(
                        message = errorMessage,
                        onRetry = viewModel::loadNextPage
                    )
                }
            }
        }
    }
}

@Composable
private fun JmArticleDetail(
    modifier: Modifier,
    uiState: JmUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddVocabulary: (String, String) -> Unit,
    onOpenPreviousArticle: () -> Unit,
    onOpenNextArticle: () -> Unit
) {
    val article = uiState.articleDetail
    val articleErrorMessage = uiState.articleErrorMessage
    var dragOffsetY by remember(uiState.selectedArticleId) { mutableFloatStateOf(0f) }
    val currentIndex = uiState.items.indexOfFirst { it.id == uiState.selectedArticleId }
    val canOpenPrevious = currentIndex > 0
    val canOpenNext = currentIndex >= 0 && (currentIndex < uiState.items.lastIndex || uiState.hasNextPage)
    val swipeDirection = when {
        dragOffsetY >= SWIPE_INDICATOR_MIN_OFFSET && canOpenPrevious -> SwipeArticleDirection.PREVIOUS
        dragOffsetY <= -SWIPE_INDICATOR_MIN_OFFSET && canOpenNext -> SwipeArticleDirection.NEXT
        else -> SwipeArticleDirection.NONE
    }
    val canReleaseToSwitch = abs(dragOffsetY) >= SWIPE_ARTICLE_THRESHOLD

    BackHandler(onBack = onBack)

    LaunchedEffect(uiState.selectedArticleId) {
        dragOffsetY = 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.selectedArticleId, uiState.isLoadingArticle, canOpenNext, canOpenPrevious) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (!uiState.isLoadingArticle) {
                            when {
                                dragOffsetY <= -SWIPE_ARTICLE_THRESHOLD && canOpenNext -> {
                                    onOpenNextArticle()
                                }
                                dragOffsetY >= SWIPE_ARTICLE_THRESHOLD && canOpenPrevious -> {
                                    onOpenPreviousArticle()
                                }
                            }
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                    }
                ) { change, dragAmount ->
                    val nextOffset = (dragOffsetY + dragAmount).coerceIn(-SWIPE_MAX_DRAG, SWIPE_MAX_DRAG)
                    dragOffsetY = when {
                        nextOffset > 0f && !canOpenPrevious -> 0f
                        nextOffset < 0f && !canOpenNext -> 0f
                        else -> nextOffset
                    }
                    change.consume()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dragOffsetY.roundToInt()) }
        ) {
            when {
                uiState.isLoadingArticle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                article != null -> {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ArticleTokenContent(
                            tokens = article.tokens,
                            language = article.language,
                            isAddingVocabulary = uiState.isAddingVocabulary,
                            addingVocabularyKey = uiState.addingVocabularyKey,
                            addedVocabularyKeys = uiState.addedVocabularyKeys,
                            onAddVocabulary = onAddVocabulary
                        )
                    }
                }

                articleErrorMessage != null -> {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ErrorCard(
                            message = articleErrorMessage,
                            onRetry = onRetry
                        )
                    }
                }
            }
        }

        if (swipeDirection == SwipeArticleDirection.NONE && uiState.currentArticleIndex >= 0) {
            val displayIndex = uiState.currentArticleIndex + 1
            val total = if (uiState.feedTotal > 0) uiState.feedTotal else uiState.items.size
            Text(
                text = "$displayIndex / $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp, end = 4.dp)
            )
        }

        if (swipeDirection == SwipeArticleDirection.PREVIOUS) {
            SwipePullIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
                message = if (canReleaseToSwitch) {
                    "Release to go to previous article"
                } else {
                    "Pull down for previous article"
                }
            )
        }
        if (swipeDirection == SwipeArticleDirection.NEXT) {
            SwipePullIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
                message = if (canReleaseToSwitch) {
                    "Release to go to next article"
                } else {
                    "Pull up for next article"
                }
            )
        }
    }
}

@Composable
private fun SwipePullIndicator(
    modifier: Modifier = Modifier,
    message: String
) {
    Card(modifier = modifier) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ArticleTokenContent(
    tokens: List<JmArticleToken>,
    language: String?,
    isAddingVocabulary: Boolean,
    addingVocabularyKey: String?,
    addedVocabularyKeys: Set<String>,
    onAddVocabulary: (String, String) -> Unit
) {
    var selectedTokenIndex by remember(tokens) { mutableIntStateOf(-1) }

    if (tokens.isEmpty()) {
        Text(
            text = "No token data available.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tokens.forEachIndexed { tokenIndex, token ->
                val tokenText = tokenDisplayText(tokens, tokenIndex)
                if (tokenText.isBlank()) return@forEachIndexed

                Box {
                    Text(
                        text = tokenText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedTokenIndex == tokenIndex) {
                            if (isSystemInDarkTheme()) Color(0xFFFFAB91) else Color(0xFFD84315)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.clickable {
                            selectedTokenIndex = if (selectedTokenIndex == tokenIndex) -1 else tokenIndex
                        }
                    )

                    DropdownMenu(
                        expanded = selectedTokenIndex == tokenIndex,
                        onDismissRequest = { selectedTokenIndex = -1 },
                        modifier = Modifier.widthIn(max = 260.dp)
                    ) {
                        TokenInfoMenu(
                            token = token,
                            language = language,
                            isAddingVocabulary = isAddingVocabulary,
                            addingVocabularyKey = addingVocabularyKey,
                            addedVocabularyKeys = addedVocabularyKeys,
                            onAddVocabulary = onAddVocabulary
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TokenInfoMenu(
    token: JmArticleToken,
    language: String?,
    isAddingVocabulary: Boolean,
    addingVocabularyKey: String?,
    addedVocabularyKeys: Set<String>,
    onAddVocabulary: (String, String) -> Unit
) {
    val reading = token.reading?.takeIf { it.isNotBlank() }
    val dictForm = token.dictionaryForm?.takeIf { it.isNotBlank() }
    val tokenText = token.token?.takeIf { it.isNotBlank() }
    val word = dictForm ?: tokenText
    val normalizedLanguage = language?.takeIf { it.isNotBlank() }
    val vocabularyKey = if (word != null && normalizedLanguage != null) {
        buildVocabularyKey(word = word, language = normalizedLanguage)
    } else {
        null
    }
    val isAdded = token.addedToVocabulary || (vocabularyKey != null && addedVocabularyKeys.contains(vocabularyKey))
    val isAddingThisKey = vocabularyKey != null && isAddingVocabulary && addingVocabularyKey == vocabularyKey
    val buttonEnabled = vocabularyKey != null && !isAdded && !isAddingThisKey
    val buttonText = when {
        vocabularyKey == null -> "Unavailable"
        isAdded -> "Vocabulary already added"
        isAddingThisKey -> "Adding..."
        else -> "Add to Vocabulary"
    }
    val meaning = token.meaning?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = reading ?: dictForm ?: tokenText ?: "-",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (dictForm != null && reading != null) {
                    Text(
                        text = dictForm,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            if (meaning != null) {
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "No meaning available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }

        Button(
            onClick = {
                if (word != null && normalizedLanguage != null) {
                    onAddVocabulary(word, normalizedLanguage)
                }
            },
            enabled = buttonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text(text = buttonText)
        }
    }
}

@Composable
private fun ArticleFeedCard(
    article: JmFeedArticleItem,
    onClick: () -> Unit
) {
    val previewText = article.previewText?.takeIf { it.isNotBlank() } ?: ""
    val languageText = article.language?.takeIf { it.isNotBlank() }
    val levelText = article.levelLabel?.takeIf { it.isNotBlank() }
        ?: article.level?.toString()?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (languageText != null || levelText != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languageText?.let { label ->
                        ArticleMetaBadge(
                            text = label,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    levelText?.let { label ->
                        ArticleMetaBadge(
                            text = label,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = toRelativeTime(article.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArticleMetaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = Modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}

private fun tokenDisplayText(
    tokens: List<JmArticleToken>,
    index: Int
): String {
    return tokens[index].token?.trim().orEmpty()
}

private fun toRelativeTime(rawCreatedAt: String?): String {
    val timestamp = parseCreatedAtMillis(rawCreatedAt) ?: return "Unknown time"
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

private fun parseCreatedAtMillis(rawCreatedAt: String?): Long? {
    if (rawCreatedAt.isNullOrBlank()) return null

    rawCreatedAt.toLongOrNull()?.let { return it }

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    formats.forEach { pattern ->
        runCatching {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            parser.parse(rawCreatedAt)?.time
        }.getOrNull()?.let { return it }
    }
    return null
}

private const val SWIPE_ARTICLE_THRESHOLD = 72f
private const val SWIPE_INDICATOR_MIN_OFFSET = 8f
private const val SWIPE_MAX_DRAG = 240f

private enum class SwipeArticleDirection {
    NONE,
    NEXT,
    PREVIOUS
}

private fun buildVocabularyKey(word: String, language: String): String {
    return "${language.trim().lowercase()}::${word.trim()}"
}
