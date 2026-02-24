package com.example.skybase.vocabulary

class VocabularyRepository(
    private val api: VocabularyApi = VocabularyApiClient.service
) {
    suspend fun addVocabulary(
        dictKey: String
    ): AddVocabularyResponse {
        return api.addVocabulary(
            AddVocabularyRequest(
                dictKey = dictKey
            )
        )
    }
}
