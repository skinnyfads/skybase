package com.example.skybase.vocabulary

class VocabularyRepository(
    private val api: VocabularyApi = VocabularyApiClient.service
) {
    suspend fun fetchAllVocabularyDictKeys(limit: Int = 20): Set<String> {
        val pageSize = if (limit > 0) limit else 20
        val dictKeys = linkedSetOf<String>()
        var page = 1

        while (true) {
            val response = api.listVocabularies(page = page, limit = pageSize)
            response.items.forEach { item ->
                if (item.dictKey.isNotBlank()) dictKeys.add(item.dictKey)
            }

            val reachedTotal = page * response.limit >= response.total
            val reachedLastPage = response.items.size < response.limit
            if (response.items.isEmpty() || reachedTotal || reachedLastPage) {
                break
            }
            page += 1
        }

        return dictKeys
    }

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
