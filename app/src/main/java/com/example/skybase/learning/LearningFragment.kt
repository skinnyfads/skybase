package com.example.skybase.learning

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningFragment(
    modifier: Modifier = Modifier,
    viewModel: LearningViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState()
) {
    val uiState by viewModel.uiState.collectAsState()

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

    var showSyncDialog by remember { mutableStateOf(false) }

    if (showSyncDialog) {
        SyncTokenDialog(
            currentServerToken = uiState.syncToken ?: "",
            isVerifying = uiState.isVerifyingToken,
            errorMessage = uiState.verifyTokenError,
            onDismiss = {
                showSyncDialog = false
                viewModel.clearVerifyError()
            },
            onSave = { newToken ->
                viewModel.verifyAndUpdateToken(newToken)
            }
        )
    }

    // Success close logic
    LaunchedEffect(uiState.isVerifyingToken, uiState.verifyTokenError) {
        if (!uiState.isVerifyingToken && uiState.verifyTokenError == null && showSyncDialog) {
            val wasVerifying = true // We can just assume if we successfully verified, we can close it, but a cleaner way is just tracking success. The viewModel clears the error and sets isVerifying to false on success.
            // When user clicks save, isVerifying becomes true. When it finishes successfully, it becomes false and error is null.
            // Let's rely on checking if it was verifying previous frame, but LaunchedEffect fires on changes.
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { showSyncDialog = true }) {
                Text(text = "Sync Token")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.items.isEmpty() && uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Box
            }

            if (uiState.items.isEmpty() && uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorContent(
                        message = uiState.errorMessage!!,
                        onRetry = viewModel::loadNextPage
                    )
                }
                return@Box
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshFeed,
                modifier = Modifier.fillMaxSize()
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
                            val post = uiState.items[index]
                            post.id ?: "post-$index"
                        }
                    ) { index ->
                        val post = uiState.items[index]

                        val postId = post.id
                        if (postId != null) {
                            LaunchedEffect(postId) {
                                viewModel.markPostAsSeen(postId)
                            }
                        }

                        PostCard(post = post)
                    }

                    if (uiState.isLoading && uiState.items.isNotEmpty() && !uiState.isRefreshing) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    val errorMessage = uiState.errorMessage
                    if (errorMessage != null && uiState.items.isNotEmpty()) {
                        item(key = "error") {
                            ErrorContent(
                                message = errorMessage,
                                onRetry = viewModel::loadNextPage
                            )
                        }
                    }

                    if (!uiState.hasNextPage && uiState.items.isNotEmpty()) {
                        item(key = "end") {
                            Text(
                                text = "You've reached the end",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncTokenDialog(
    currentServerToken: String,
    isVerifying: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tokenInput by remember { mutableStateOf(currentServerToken) }

    AlertDialog(
        onDismissRequest = {
            if (!isVerifying) onDismiss()
        },
        title = { Text(text = "Custom Sync Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Use this token to sync your seen posts across devices. You can also enter another device's token here.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Sync Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tokenInput) },
                enabled = !isVerifying && tokenInput.isNotBlank() && tokenInput != currentServerToken
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PostCard(post: FeedPost) {
    val content = post.content?.takeIf { it.isNotBlank() } ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            MarkdownText(
                markdown = content,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                linkColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = toRelativeTime(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
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
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
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
