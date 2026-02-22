package com.example.skybase.learning

import com.google.gson.annotations.SerializedName

data class FeedPostsResponse(
    val items: List<FeedPost> = emptyList(),
    @SerializedName("next_cursor")
    val nextCursor: PostsCursor? = null
)

data class FeedPost(
    val id: String? = null,
    val content: String? = null,
    @SerializedName("random_key")
    val randomKey: Double? = null,
    @SerializedName("biased_score")
    val biasedScore: Double? = null,
    @SerializedName("is_deleted")
    val isDeleted: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class PostsCursor(
    @SerializedName("biased_score")
    val biasedScore: Double? = null,
    val id: String? = null
)

class LearningRepository(
    private val api: LearningApi
) {
    suspend fun fetchPosts(
        limit: Int,
        cursorScore: Double? = null,
        cursorId: String? = null
    ): FeedPostsResponse = api.getPosts(
        limit = limit,
        cursorScore = cursorScore,
        cursorId = cursorId
    )
}
