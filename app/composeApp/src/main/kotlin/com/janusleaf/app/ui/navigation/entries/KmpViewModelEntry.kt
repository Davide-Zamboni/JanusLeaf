package com.janusleaf.app.ui.navigation.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.janusleaf.app.presentation.viewmodel.ClearableViewModel
import org.koin.compose.getKoin

@Composable
inline fun <reified VM : ClearableViewModel> rememberKmpViewModel(): VM {
    val koin = getKoin()
    val viewModel = remember { koin.get<VM>() }
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.clear()
        }
    }
    return viewModel
}
