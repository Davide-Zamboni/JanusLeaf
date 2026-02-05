package com.janusleaf.app.di

import com.janusleaf.app.model.data.cache.InMemoryInspirationCache
import com.janusleaf.app.model.data.cache.InMemoryJournalCache
import com.janusleaf.app.model.data.cache.InspirationCache
import com.janusleaf.app.model.data.cache.JournalCache
import com.janusleaf.app.model.data.store.AuthStore
import com.janusleaf.app.model.data.store.InspirationStore
import com.janusleaf.app.model.data.store.JournalStore
import com.janusleaf.app.viewmodel.AuthScreenViewModel
import com.janusleaf.app.viewmodel.JournalEditorViewModel
import com.janusleaf.app.viewmodel.JournalListViewModel
import com.janusleaf.app.viewmodel.MoodInsightsViewModel
import com.janusleaf.app.viewmodel.ProfileViewModel
import com.janusleaf.app.viewmodel.WelcomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single { AuthStore(get(), get()) }
    single<JournalCache> { InMemoryJournalCache() }
    single<InspirationCache> { InMemoryInspirationCache() }
    single { JournalStore(get(), get()) }
    single { InspirationStore(get(), get(), get(), get()) }

    viewModel { AuthScreenViewModel(get()) }
    viewModel { JournalListViewModel(get(), get(), get()) }
    viewModel { JournalEditorViewModel(get()) }
    viewModel { MoodInsightsViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { WelcomeViewModel(get()) }
}
