package com.example.skybase.jm

enum class JmFlashcardDirection(val value: String, val label: String) {
    WORD_FIRST("word_first", "Word First"),
    MEANING_FIRST("meaning_first", "Meaning First"),
    READING_FIRST("reading_first", "Reading First"),
    RANDOM("random", "Random");

    companion object {
        fun fromValue(value: String?): JmFlashcardDirection {
            return entries.firstOrNull { it.value == value } ?: WORD_FIRST
        }
    }
}
