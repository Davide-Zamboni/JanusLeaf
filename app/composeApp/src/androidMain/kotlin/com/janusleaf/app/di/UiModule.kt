package com.janusleaf.app.di

import com.janusleaf.app.ui.viewmodel.AuthViewModel
import com.janusleaf.app.ui.viewmodel.InspirationViewModel
import com.janusleaf.app.ui.viewmodel.JournalViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { JournalViewModel(get()) }
    viewModel { InspirationViewModel(get(), get(), get()) }
}
