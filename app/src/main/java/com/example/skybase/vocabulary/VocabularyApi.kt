package com.example.skybase.vocabulary

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class AddVocabularyRequest(
    val dictKey: String
)

data class AddVocabularyResponse(
    val id: String,
    val dictKey: String,
    val createdAt: String?,
    val surface: String? = null,
    val dictForm: String? = null,
    val reading: String? = null,
    val meanings: List<String>? = null,
    val pos: List<String>? = null,
    val reason: String? = null
)

interface VocabularyApi {
    @POST("vocabularies")
    suspend fun addVocabulary(
        @Body request: AddVocabularyRequest
    ): AddVocabularyResponse
}

object VocabularyApiClient {
    private val BASE_URL = com.example.skybase.BuildConfig.JM_NEWS_BASE_URL

    val service: VocabularyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VocabularyApi::class.java)
    }
}
