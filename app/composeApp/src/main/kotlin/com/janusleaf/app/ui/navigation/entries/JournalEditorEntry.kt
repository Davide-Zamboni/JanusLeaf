package com.janusleaf.app.ui.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.JournalEditorNavKey
import com.janusleaf.app.ui.journal.JournalEditorScreen
import com.janusleaf.app.presentation.viewmodel.JournalEditorViewModel

fun EntryProviderScope<NavKey>.journalEditorEntry(
    onBack: () -> Unit,
    registerBackHandler: (handler: (() -> Unit)?) -> Unit
) {
    entry<JournalEditorNavKey> { key ->
        val viewModel: JournalEditorViewModel = rememberKmpViewModel()
        JournalEditorScreen(
            entryId = key.entryId,
            viewModel = viewModel,
            onBack = onBack,
            registerBackHandler = registerBackHandler
        )
    }
}
