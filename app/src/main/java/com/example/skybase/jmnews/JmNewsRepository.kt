package com.example.skybase.jmnews

data class FeedResponse(
    val items: List<FeedArticleItem> = emptyList(),
    val pagination: FeedPagination? = null
)

data class FeedArticleItem(
    val id: String? = null,
    val title: String? = null,
    val previewText: String? = null,
    val createdAt: String? = null
)

data class FeedPagination(
    val page: Int? = null,
    val limit: Int? = null,
    val hasNext: Boolean? = null,
    val totalPages: Int? = null
)

class JmNewsRepository(
    private val api: JmNewsApi
) {
    suspend fun fetchFeed(
        page: Int,
        limit: Int
    ): FeedResponse = api.getFeed(page = page, limit = limit)
}
