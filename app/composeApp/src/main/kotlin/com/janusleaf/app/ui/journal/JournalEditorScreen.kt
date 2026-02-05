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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janusleaf.app.ui.preview.PreviewSamples
import com.janusleaf.app.ui.theme.JanusLeafTheme
import com.janusleaf.app.ui.util.stripMarkdown
import com.janusleaf.app.presentation.viewmodel.JournalEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    entryId: String,
    viewModel: JournalEditorViewModel,
    onBack: () -> Unit,
    registerBackHandler: (handler: (() -> Unit)?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entry = uiState.entry

    var title by rememberSaveable(entryId) { mutableStateOf("") }
    var body by rememberSaveable(entryId) { mutableStateOf("") }
    var originalTitle by rememberSaveable(entryId) { mutableStateOf("") }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var previewMode by rememberSaveable { mutableStateOf(false) }
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(entryId) {
        if (!isPreview) {
            viewModel.bindEntry(entryId)
            viewModel.loadEntry(entryId)
        }
    }

    LaunchedEffect(entry?.id) {
        if (entry != null && entry.id == entryId) {
            title = entry.title
            originalTitle = entry.title
            body = entry.body
        }
    }

    val handleBack = {
        val hasTitleChanges = title.isNotBlank() && title != originalTitle
        if (hasTitleChanges) {
            viewModel.updateTitle(entryId, title) { onBack() }
        } else {
            viewModel.forceSave(entryId) { onBack() }
        }
    }

    SideEffect {
        registerBackHandler(handleBack)
    }

    DisposableEffect(Unit) {
        onDispose { registerBackHandler(null) }
    }

    JournalEditorContent(
        title = title,
        onTitleChange = { title = it },
        onTitleBlur = {
            if (title.isNotBlank() && title != originalTitle) {
                viewModel.updateTitle(entryId, title) { success ->
                    if (success) originalTitle = title
                }
            }
        },
        body = body,
        onBodyChange = {
            body = it
            viewModel.updateBody(entryId, it)
        },
        previewMode = previewMode,
        onTogglePreview = { previewMode = !previewMode },
        isSaving = uiState.isSaving,
        errorMessage = uiState.errorMessage,
        onBack = handleBack,
        onDelete = {
            viewModel.deleteEntry(entryId) { success ->
                if (success) onBack()
            }
        },
        showDeleteConfirm = showDeleteConfirm,
        onShowDeleteConfirm = { showDeleteConfirm = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorContent(
    title: String,
    onTitleChange: (String) -> Unit,
    onTitleBlur: () -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    previewMode: Boolean,
    onTogglePreview: () -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePreview) {
                        Icon(
                            imageVector = if (previewMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { onShowDeleteConfirm(true) }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
}

@Preview
@Composable
private fun JournalEditorPreview() {
    val sample = PreviewSamples.journal()
    JanusLeafTheme {
        JournalEditorContent(
            title = sample.title,
            onTitleChange = {},
            onTitleBlur = {},
            body = sample.body,
            onBodyChange = {},
            previewMode = false,
            onTogglePreview = {},
            isSaving = false,
            errorMessage = null,
            onBack = {},
            onDelete = {},
            showDeleteConfirm = false,
            onShowDeleteConfirm = {}
        )
    }
}
