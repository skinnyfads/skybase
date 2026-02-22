package com.example.skybase.jmnews

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun JmNewsFragment(
    modifier: Modifier = Modifier,
    viewModel: JmNewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
            ArticleFeedCard(article = article)
        }

        if (uiState.isLoading && uiState.items.isNotEmpty()) {
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

@Composable
private fun ArticleFeedCard(article: FeedArticleItem) {
    val previewText = article.previewText?.takeIf { it.isNotBlank() } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
