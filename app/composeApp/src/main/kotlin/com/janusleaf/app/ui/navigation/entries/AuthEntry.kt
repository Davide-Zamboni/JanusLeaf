package com.janusleaf.app.ui.navigation.entries

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.AuthNavKey
import com.janusleaf.app.ui.auth.AuthScreen
import com.janusleaf.app.presentation.viewmodel.AuthFormViewModel

fun EntryProviderScope<NavKey>.authEntry() {
    entry<AuthNavKey> {
        val viewModel: AuthFormViewModel = rememberKmpViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AuthScreen(
            uiState = uiState,
            isValidEmail = viewModel::isValidEmail,
            isValidPassword = viewModel::isValidPassword,
            isValidUsername = viewModel::isValidUsername,
            onLogin = viewModel::login,
            onRegister = viewModel::register,
            onClearError = viewModel::clearError
        )    }
}
