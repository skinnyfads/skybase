package com.example.skybase.jm

import com.google.gson.annotations.SerializedName

data class JmFeedResponseDto(
    val items: List<JmFeedArticleItemDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class JmFeedArticleItemDto(
    val id: String? = null,
    val previewText: String? = null,
    val language: String? = null,
    val languageId: String? = null,
    val level: Int? = null,
    @SerializedName("level_label")
    val levelLabel: String? = null,
    val createdAt: String? = null
)

data class JmArticleDetailDto(
    @SerializedName(value = "ID", alternate = ["id"])
    val id: String? = null,
    @SerializedName(value = "PreviewText", alternate = ["previewText"])
    val previewText: String? = null,
    @SerializedName(value = "CreatedAt", alternate = ["createdAt"])
    val createdAt: String? = null,
    @SerializedName(value = "Language", alternate = ["language"])
    val language: String? = null,
    @SerializedName(value = "Topic", alternate = ["topic"])
    val topic: String? = null,
    @SerializedName(value = "Text", alternate = ["text"])
    val text: String? = null,
    @SerializedName(value = "Level", alternate = ["level"])
    val level: Int? = null,
    @SerializedName(value = "LevelLabel", alternate = ["levelLabel", "level_label"])
    val levelLabel: String? = null,
    @SerializedName(value = "Tokens", alternate = ["tokens"])
    val tokens: List<JmArticleTokenDto> = emptyList()
)

data class JmArticleTokenDto(
    @SerializedName(value = "sentence", alternate = ["Sentence"])
    val sentence: Int? = null,
    @SerializedName(value = "token", alternate = ["Token"])
    val token: String? = null,
    @SerializedName(value = "dictionaryForm", alternate = ["DictionaryForm"])
    val dictionaryForm: String? = null,
    @SerializedName(value = "reading", alternate = ["Reading"])
    val reading: String? = null,
    @SerializedName(value = "meaning", alternate = ["Meaning"])
    val meaning: String? = null,
    @SerializedName(value = "addedToVocabulary", alternate = ["AddedToVocabulary"])
    val addedToVocabulary: Boolean? = null
)

data class JmAddVocabularyRequest(
    val word: String,
    val language: String
)

data class JmVocabularyResponseDto(
    @SerializedName(value = "id", alternate = ["ID"])
    val id: String? = null,
    @SerializedName(value = "word", alternate = ["Word"])
    val word: String? = null,
    @SerializedName(value = "language", alternate = ["Language"])
    val language: String? = null,
    @SerializedName(value = "readings", alternate = ["Readings"])
    val readings: List<String> = emptyList(),
    @SerializedName(value = "meanings", alternate = ["Meanings"])
    val meanings: List<String> = emptyList(),
    @SerializedName(value = "level", alternate = ["Level", "level_label", "levelLabel"])
    val level: String? = null,
    @SerializedName(value = "status", alternate = ["Status"])
    val status: String? = null,
    @SerializedName(value = "deleted", alternate = ["Deleted"])
    val deleted: Boolean? = null,
    @SerializedName(value = "createdAt", alternate = ["CreatedAt"])
    val createdAt: String? = null,
    @SerializedName(value = "updatedAt", alternate = ["UpdatedAt"])
    val updatedAt: String? = null,
    @SerializedName(value = "examples", alternate = ["Examples"])
    val examples: List<JmVocabularyExampleDto> = emptyList()
)

data class JmVocabularyExampleDto(
    @SerializedName(value = "id", alternate = ["ID"])
    val id: String? = null,
    @SerializedName(value = "vocabId", alternate = ["vocab_id", "VocabID", "VocabId"])
    val vocabId: String? = null,
    @SerializedName(value = "sentence", alternate = ["Sentence"])
    val sentence: String? = null,
    @SerializedName(value = "meaning", alternate = ["Meaning"])
    val meaning: String? = null,
    @SerializedName(value = "level", alternate = ["Level"])
    val level: String? = null
)

data class JmVocabularyListResponseDto(
    val items: List<JmVocabularyResponseDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class JmRandomVocabularyResponseDto(
    val items: List<JmVocabularyResponseDto> = emptyList()
)

data class JmDeleteVocabularyResponseDto(
    val success: Boolean = false
)

data class JmFeedResponse(
    val items: List<JmFeedArticleItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class JmFeedArticleItem(
    val id: String? = null,
    val previewText: String? = null,
    val language: String? = null,
    val languageId: String? = null,
    val level: Int? = null,
    val levelLabel: String? = null,
    val createdAt: String? = null
)

data class JmArticleDetail(
    val id: String? = null,
    val previewText: String? = null,
    val createdAt: String? = null,
    val language: String? = null,
    val topic: String? = null,
    val text: String? = null,
    val level: Int? = null,
    val levelLabel: String? = null,
    val tokens: List<JmArticleToken> = emptyList()
)

data class JmArticleToken(
    val sentence: Int? = null,
    val token: String? = null,
    val dictionaryForm: String? = null,
    val reading: String? = null,
    val meaning: String? = null,
    val addedToVocabulary: Boolean = false
)

data class JmVocabularyResponse(
    val id: String? = null,
    val word: String? = null,
    val language: String? = null,
    val readings: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val level: String? = null,
    val status: String? = null,
    val deleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val examples: List<JmVocabularyExample> = emptyList()
)

data class JmVocabularyExample(
    val id: String? = null,
    val vocabId: String? = null,
    val sentence: String? = null,
    val meaning: String? = null,
    val level: String? = null
)

data class JmVocabularyListResponse(
    val items: List<JmVocabularyResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

class JmRepository(
    private val api: JmApi
) {
    suspend fun fetchFeed(
        page: Int,
        limit: Int,
        language: String? = null,
        level: String? = null
    ): JmFeedResponse = api.getFeed(
        page = page,
        limit = limit,
        language = language,
        level = level
    ).toModel()

    suspend fun fetchArticle(id: String): JmArticleDetail = api.getArticle(id = id).toModel()

    suspend fun addVocabulary(word: String, language: String): JmVocabularyResponse = api.addVocabulary(
        JmAddVocabularyRequest(
            word = word,
            language = language
        )
    ).toModel()

    suspend fun fetchVocabularies(
        page: Int,
        limit: Int,
        language: String? = null,
        level: String? = null,
        withExamples: Boolean = false
    ): JmVocabularyListResponse = api.listVocabularies(
        page = page,
        limit = limit,
        language = language,
        level = level,
        withExamples = withExamples
    ).toModel()

    suspend fun fetchRandomVocabularies(
        limit: Int,
        language: String? = null,
        level: String? = null,
        withExamples: Boolean = false
    ): List<JmVocabularyResponse> = api.randomVocabularies(
        limit = limit,
        language = language,
        level = level,
        withExamples = withExamples
    ).items.map { it.toModel() }

    suspend fun deleteVocabulary(id: String): Boolean = api.deleteVocabulary(id = id).success
}

private fun JmFeedResponseDto.toModel(): JmFeedResponse = JmFeedResponse(
    items = items.map { it.toModel() },
    total = total,
    page = page,
    limit = limit
)

private fun JmFeedArticleItemDto.toModel(): JmFeedArticleItem = JmFeedArticleItem(
    id = id,
    previewText = previewText,
    language = language,
    languageId = languageId,
    level = level,
    levelLabel = levelLabel,
    createdAt = createdAt
)

private fun JmArticleDetailDto.toModel(): JmArticleDetail = JmArticleDetail(
    id = id,
    previewText = previewText,
    createdAt = createdAt,
    language = language,
    topic = topic,
    text = text,
    level = level,
    levelLabel = levelLabel,
    tokens = tokens.map { it.toModel() }
)

private fun JmArticleTokenDto.toModel(): JmArticleToken = JmArticleToken(
    sentence = sentence,
    token = token,
    dictionaryForm = dictionaryForm,
    reading = reading,
    meaning = meaning,
    addedToVocabulary = addedToVocabulary == true
)

private fun JmVocabularyResponseDto.toModel(): JmVocabularyResponse = JmVocabularyResponse(
    id = id,
    word = word,
    language = language,
    readings = readings,
    meanings = meanings,
    level = level,
    status = status,
    deleted = deleted == true,
    createdAt = createdAt,
    updatedAt = updatedAt,
    examples = examples.map { it.toModel() }
)

private fun JmVocabularyExampleDto.toModel(): JmVocabularyExample = JmVocabularyExample(
    id = id,
    vocabId = vocabId,
    sentence = sentence,
    meaning = meaning,
    level = level
)

private fun JmVocabularyListResponseDto.toModel(): JmVocabularyListResponse = JmVocabularyListResponse(
    items = items.map { it.toModel() },
    total = total,
    page = page,
    limit = limit
)
