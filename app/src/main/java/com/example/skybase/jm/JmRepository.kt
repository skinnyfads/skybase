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
    val id: String? = null,
    val word: String? = null,
    val language: String? = null,
    val readings: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val status: String? = null,
    val deleted: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
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
    val status: String? = null,
    val deleted: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
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
    status = status,
    deleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)
