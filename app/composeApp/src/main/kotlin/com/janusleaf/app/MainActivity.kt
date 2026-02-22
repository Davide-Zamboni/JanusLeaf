package com.janusleaf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.janusleaf.app.ui.navigation.AuthNavKey
import com.janusleaf.app.ui.navigation.JournalEditorNavKey
import com.janusleaf.app.ui.navigation.JournalListNavKey
import com.janusleaf.app.ui.navigation.MoodInsightsNavKey
import com.janusleaf.app.ui.navigation.ProfileNavKey
import com.janusleaf.app.ui.navigation.entries.authEntry
import com.janusleaf.app.ui.navigation.entries.journalEditorEntry
import com.janusleaf.app.ui.navigation.entries.journalListEntry
import com.janusleaf.app.ui.navigation.entries.moodInsightsEntry
import com.janusleaf.app.ui.navigation.entries.profileEntry
import com.janusleaf.app.ui.navigation.entries.welcomeEntry
import com.janusleaf.app.ui.theme.JanusLeafTheme
import com.janusleaf.app.domain.repository.AuthRepository
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            JanusLeafTheme {
                MainComposable()
            }
        }
    }
}

@Composable
private fun MainComposable() {
    val authRepository: AuthRepository = koinInject()

    val isAuthenticated by authRepository.observeAuthState()
        .collectAsStateWithLifecycle(initialValue = false)
    val startDestination: NavKey = if (isAuthenticated) JournalListNavKey else AuthNavKey
    val backStack = rememberNavBackStack(startDestination)
    val editorBackHandler = remember { mutableStateOf<(() -> Unit)?>(null) }

    ResetBackStack(isAuthenticated, backStack)

    val entryDecorators: List<NavEntryDecorator<NavKey>> = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    )

    NavDisplay(
        backStack = backStack,
        modifier = Modifier,
        entryDecorators = entryDecorators,
        onBack = {
            if (backStack.size <= 1) return@NavDisplay
            val active = backStack.lastOrNull()
            if (active is JournalEditorNavKey) {
                editorBackHandler.value?.invoke() ?: backStack.removeLastOrNull()
            } else {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            authEntry()
            journalListEntry(
                onEntryClick = { backStack.add(JournalEditorNavKey(it)) },
                onProfileClick = { backStack.add(ProfileNavKey) },
                onNavigateToJournal = { replaceTop(backStack, JournalListNavKey) },
                onNavigateToInsights = { replaceTop(backStack, MoodInsightsNavKey) }
            )
            moodInsightsEntry(
                onProfileClick = { backStack.add(ProfileNavKey) },
                onNavigateToJournal = { replaceTop(backStack, JournalListNavKey) },
                onNavigateToInsights = { replaceTop(backStack, MoodInsightsNavKey) }
            )
            profileEntry(
                onBack = { backStack.removeLastOrNull() }
            )
            journalEditorEntry(
                onBack = {
                    backStack.removeLastOrNull()
                },
                registerBackHandler = { handler -> editorBackHandler.value = handler }
            )
            welcomeEntry()
        }
    )
}

@Composable
private fun ResetBackStack(
    isAuthenticated: Boolean,
    backStack: NavBackStack<NavKey>
) {
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            backStack.reset(AuthNavKey)
        } else {
            backStack.reset(JournalListNavKey)
        }
    }
}

private fun MutableList<NavKey>.reset(destination: NavKey) {
    if (isEmpty()) {
        add(destination)
    } else {
        this[0] = destination
        if (size > 1) {
            subList(1, size).clear()
        }
    }
}

private fun replaceTop(backStack: MutableList<NavKey>, destination: NavKey) {
    if (backStack.isEmpty()) {
        backStack.add(destination)
    } else {
        backStack[backStack.lastIndex] = destination
    }
}
