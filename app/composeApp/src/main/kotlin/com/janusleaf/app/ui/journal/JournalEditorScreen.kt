package com.janusleaf.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.janusleaf.app.presentation.state.JournalEditorUiState
import com.janusleaf.app.ui.preview.PreviewSamples
import com.janusleaf.app.ui.theme.JanusLeafTheme
import com.janusleaf.app.ui.util.stripMarkdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    entryId: String,
    uiState: JournalEditorUiState,
    bindEntry: (String) -> Unit,
    loadEntry: (String) -> Unit,
    updateTitle: (String, String) -> Unit,
    updateBody: (String, String) -> Unit,
    requestClose: (String, String) -> Unit,
    deleteEntry: (String) -> Unit,
    consumeNavigateBack: () -> Unit,
    onBack: () -> Unit,
    registerBackHandler: (handler: (() -> Unit)?) -> Unit = {}
) {
    val entry = uiState.entry
    val persistedTitle = entry?.title.orEmpty()

    var title by rememberSaveable(entryId) { mutableStateOf("") }
    var body by rememberSaveable(entryId) { mutableStateOf("") }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var previewMode by rememberSaveable { mutableStateOf(false) }
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(entryId) {
        if (!isPreview) {
            bindEntry(entryId)
            loadEntry(entryId)
        }
    }

    LaunchedEffect(entry?.id) {
        if (entry != null && entry.id == entryId) {
            title = entry.title
            body = entry.body
        }
    }

    LaunchedEffect(uiState.pendingNavigateBack) {
        if (uiState.pendingNavigateBack) {
            consumeNavigateBack()
            onBack()
        }
    }

    val handleBack = {
        requestClose(entryId, title)
    }

    SideEffect {
        registerBackHandler(handleBack)
    }

    DisposableEffect(Unit) {
        onDispose { registerBackHandler(null) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Entry",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { previewMode = !previewMode }) {
                        Icon(
                            imageVector = if (previewMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { padding ->
        JournalEditorContent(
            modifier = Modifier.padding(padding),
            title = title,
            onTitleChange = { title = it },
            onTitleBlur = {
                if (title.isNotBlank() && title != persistedTitle) {
                    updateTitle(entryId, title)
                }
            },
            body = body,
            onBodyChange = {
                body = it
                updateBody(entryId, it)
            },
            previewMode = previewMode,
            isSaving = uiState.isSaving,
            errorMessage = uiState.errorMessage,
            onDelete = { deleteEntry(entryId) },
            showDeleteConfirm = showDeleteConfirm,
            onShowDeleteConfirm = { showDeleteConfirm = it }
        )
    }
}

@Composable
fun JournalEditorContent(
    modifier: Modifier = Modifier,
    title: String,
    onTitleChange: (String) -> Unit,
    onTitleBlur: () -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    previewMode: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onDelete: () -> Unit,
    showDeleteConfirm: Boolean,
    onShowDeleteConfirm: (Boolean) -> Unit
) {
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onShowDeleteConfirm(false) },
            confirmButton = {
                TextButton(onClick = {
                    onShowDeleteConfirm(false)
                    onDelete()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDeleteConfirm(false) }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Entry") },
            text = { Text("This entry will be permanently deleted.") }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onTitleBlur()
                    }
                },
            decorationBox = { innerTextField ->
                if (title.isBlank()) {
                    Text(
                        text = "Enter title...",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                innerTextField()
            }
        )

        if (previewMode) {
            Text(
                text = stripMarkdown(body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            BasicTextField(
                value = body,
                onValueChange = onBodyChange,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (body.isBlank()) {
                        Text(
                            text = "Start writing...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        if (isSaving) {
            Text(
                text = "Saving...",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Preview
@Composable
private fun JournalEditorPreview() {
    val sample = PreviewSamples.journal()
    JanusLeafTheme {
        JournalEditorScreen(
            entryId = sample.id,
            uiState = JournalEditorUiState(entry = sample),
            bindEntry = {},
            loadEntry = {},
            updateTitle = { _, _ -> },
            updateBody = { _, _ -> },
            requestClose = { _, _ -> },
            deleteEntry = {},
            consumeNavigateBack = {},
            onBack = {}
        )
    }
}
