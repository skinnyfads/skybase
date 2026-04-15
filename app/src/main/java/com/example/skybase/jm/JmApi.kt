package com.example.skybase.jm

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.DELETE

interface JmApi {
    @GET("languages")
    suspend fun getLanguages(): JmLanguagesResponseDto

    @GET("vocabs/levels")
    suspend fun getVocabularyLevels(
        @Query("language") language: String? = null
    ): JmDifficultyLevelsResponseDto

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

    @GET("vocabs")
    suspend fun listVocabularies(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("language") language: String? = null,
        @Query("level") level: String? = null,
        @Query("with_examples") withExamples: Boolean = false
    ): JmVocabularyListResponseDto

    @GET("vocabs/random")
    suspend fun randomVocabularies(
        @Query("limit") limit: Int,
        @Query("language") language: String? = null,
        @Query("level") level: String? = null,
        @Query("with_examples") withExamples: Boolean = false
    ): JmRandomVocabularyResponseDto

    @DELETE("vocabs/{id}")
    suspend fun deleteVocabulary(
        @Path("id") id: String
    ): JmDeleteVocabularyResponseDto
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
