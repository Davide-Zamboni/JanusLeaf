package com.janusleaf.app.ui.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.ProfileNavKey
import com.janusleaf.app.ui.profile.ProfileScreen
import com.janusleaf.app.presentation.viewmodel.ProfileViewModel

fun EntryProviderScope<NavKey>.profileEntry(
    onBack: () -> Unit
) {
    entry<ProfileNavKey> {
        val viewModel: ProfileViewModel = rememberKmpViewModel()
        ProfileScreen(
            viewModel = viewModel,
            onBack = onBack
        )
    }
}
