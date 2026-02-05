package com.janusleaf.app.ui.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.JournalEditorNavKey
import com.janusleaf.app.ui.journal.JournalEditorScreen
import com.janusleaf.app.viewmodel.JournalEditorViewModel
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.journalEditorEntry(
    onBack: () -> Unit,
    registerBackHandler: (handler: (() -> Unit)?) -> Unit
) {
    entry<JournalEditorNavKey> { key ->
        val viewModel: JournalEditorViewModel = koinViewModel()
        JournalEditorScreen(
            entryId = key.entryId,
            viewModel = viewModel,
            onBack = onBack,
            registerBackHandler = registerBackHandler
        )
    }
}
