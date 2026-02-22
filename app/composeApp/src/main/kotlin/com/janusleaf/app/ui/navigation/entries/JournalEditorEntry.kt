package com.janusleaf.app.ui.navigation.entries

import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janusleaf.app.ui.navigation.JournalEditorNavKey
import com.janusleaf.app.ui.journal.JournalEditorScreen
import com.janusleaf.app.presentation.viewmodel.JournalEditorViewModel

fun EntryProviderScope<NavKey>.journalEditorEntry(
    onBack: () -> Unit,
    registerBackHandler: (handler: (() -> Unit)?) -> Unit
) {
    entry<JournalEditorNavKey> { key ->
        val viewModel: JournalEditorViewModel = rememberKmpViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        JournalEditorScreen(
            entryId = key.entryId,
            uiState = uiState,
            bindEntry = viewModel::bindEntry,
            loadEntry = viewModel::loadEntry,
            updateTitle = viewModel::updateTitle,
            updateBody = viewModel::updateBody,
            requestClose = viewModel::requestClose,
            deleteEntry = viewModel::deleteEntry,
            consumeNavigateBack = viewModel::consumeNavigateBack,
            onBack = onBack,
            registerBackHandler = registerBackHandler
        )
    }
}
