package com.janusleaf.app.ui.util

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

fun moodEmoji(score: Int?): String = when (score) {
    null -> "😶"
    in 1..2 -> "😢"
    in 3..4 -> "😔"
    in 5..6 -> "😐"
    in 7..8 -> "😊"
    in 9..10 -> "😄"
    else -> "😶"
}

fun moodColor(score: Int?, colorScheme: ColorScheme): Color {
    return when (score) {
        null -> colorScheme.onSurfaceVariant
        in 1..2 -> colorScheme.error
        in 3..4 -> colorScheme.errorContainer
        in 5..6 -> colorScheme.secondary
        in 7..8 -> colorScheme.primary
        in 9..10 -> colorScheme.tertiary
        else -> colorScheme.onSurfaceVariant
    }
}
