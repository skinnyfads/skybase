package com.example.skybase.learning

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LearningApi {
    @GET("api/posts")
    suspend fun getPosts(
        @Query("limit") limit: Int,
        @Query("sync_token") syncToken: String? = null,
        @Query("cursor_bucket") cursorBucket: Int? = null,
        @Query("cursor_random_key") cursorRandomKey: Double? = null,
        @Query("cursor_id") cursorId: String? = null
    ): FeedPostsResponse

    @POST("api/sync/init")
    suspend fun initSyncToken(): SyncInitResponse

    @POST("api/sync/seen")
    suspend fun markPostsAsSeen(
        @Body request: SyncSeenRequest
    )

    @POST("api/sync/verify")
    suspend fun verifySyncToken(
        @Body request: SyncVerifyRequest
    ): SyncVerifyResponse
}

object LearningApiClient {
    private val BASE_URL = com.example.skybase.BuildConfig.LEARNING_BASE_URL

    val service: LearningApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LearningApi::class.java)
    }
}
