package com.example.skybase.learning

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface LearningApi {
    @GET("api/posts")
    suspend fun getPosts(
        @Query("limit") limit: Int,
        @Query("cursor_score") cursorScore: Double? = null,
        @Query("cursor_id") cursorId: String? = null
    ): FeedPostsResponse
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
