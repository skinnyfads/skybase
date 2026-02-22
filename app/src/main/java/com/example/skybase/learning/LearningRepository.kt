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
    val bucket: Int? = null,
    @SerializedName("is_deleted")
    val isDeleted: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class PostsCursor(
    val bucket: Int? = null,
    @SerializedName("random_key")
    val randomKey: Double? = null,
    val id: String? = null
)

data class SyncInitResponse(
    val token: String
)

data class SyncSeenRequest(
    val token: String,
    @SerializedName("post_ids")
    val postIds: List<String>
)

data class SyncVerifyRequest(
    val token: String
)

data class SyncVerifyResponse(
    val valid: Boolean
)

class LearningRepository(
    private val api: LearningApi
) {
    suspend fun fetchPosts(
        limit: Int,
        syncToken: String? = null,
        cursorBucket: Int? = null,
        cursorRandomKey: Double? = null,
        cursorId: String? = null
    ): FeedPostsResponse = api.getPosts(
        limit = limit,
        syncToken = syncToken,
        cursorBucket = cursorBucket,
        cursorRandomKey = cursorRandomKey,
        cursorId = cursorId
    )

    suspend fun initSyncToken(): String {
        return api.initSyncToken().token
    }

    suspend fun markPostsAsSeen(token: String, postIds: List<String>) {
        api.markPostsAsSeen(SyncSeenRequest(token, postIds))
    }

    suspend fun verifySyncToken(token: String): Boolean {
        return try {
            val response = api.verifySyncToken(SyncVerifyRequest(token))
            response.valid
        } catch (e: Exception) {
            false
        }
    }
}
