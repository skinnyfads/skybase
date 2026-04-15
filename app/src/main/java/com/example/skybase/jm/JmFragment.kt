package com.example.skybase.jm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class JmSubmenu(val label: String) {
    FEED("Feed"),
    FLASHCARDS("Flashcards")
}

@Composable
fun JmFragment(
    modifier: Modifier = Modifier,
    selectedSubmenu: JmSubmenu = JmSubmenu.FEED
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (selectedSubmenu) {
            JmSubmenu.FEED -> {
                JmFeedFragment(modifier = Modifier.fillMaxSize())
            }

            JmSubmenu.FLASHCARDS -> {
                JmFlashcardsFragment(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
