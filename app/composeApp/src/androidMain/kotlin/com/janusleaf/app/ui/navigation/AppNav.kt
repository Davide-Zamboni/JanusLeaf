package com.janusleaf.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object AuthNavKey : NavKey

@Serializable
data object JournalListNavKey : NavKey

@Serializable
data object MoodInsightsNavKey : NavKey

@Serializable
data class JournalEditorNavKey(val entryId: String) : NavKey

@Serializable
data object ProfileNavKey : NavKey

@Serializable
data object WelcomeNavKey : NavKey
