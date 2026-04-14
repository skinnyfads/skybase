package com.example.skybase.jm

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JmApi {
    @GET("articles/feed")
    suspend fun getFeed(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("language") language: String? = null,
        @Query("level") level: String? = null
    ): JmFeedResponseDto

    @GET("articles/{id}")
    suspend fun getArticle(
        @Path("id") id: String
    ): JmArticleDetailDto

    @POST("vocabs")
    suspend fun addVocabulary(
        @Body request: JmAddVocabularyRequest
    ): JmVocabularyResponseDto
}

object JmApiClient {
    private val BASE_URL = com.example.skybase.BuildConfig.JM_BASE_URL

    val service: JmApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JmApi::class.java)
    }
}
