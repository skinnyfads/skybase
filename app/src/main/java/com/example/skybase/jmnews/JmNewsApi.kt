package com.example.skybase.jmnews

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JmNewsApi {
    @GET("articles/feed")
    suspend fun getFeed(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): FeedResponse

    @GET("articles/{id}")
    suspend fun getArticle(
        @Path("id") id: String
    ): ArticleDetailResponse
}

object JmNewsApiClient {
    private val BASE_URL = com.example.skybase.BuildConfig.JM_NEWS_BASE_URL

    val service: JmNewsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JmNewsApi::class.java)
    }
}
