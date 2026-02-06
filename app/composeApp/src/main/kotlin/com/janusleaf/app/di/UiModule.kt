package com.janusleaf.app.di

import com.janusleaf.app.model.cache.InMemoryInspirationCache
import com.janusleaf.app.model.cache.InMemoryJournalCache
import com.janusleaf.app.model.cache.InspirationCache
import com.janusleaf.app.model.cache.JournalCache
import com.janusleaf.app.model.store.AuthStore
import com.janusleaf.app.model.store.InspirationStore
import com.janusleaf.app.model.store.JournalStore
import com.janusleaf.app.presentation.viewmodel.AuthFormViewModel
import com.janusleaf.app.presentation.viewmodel.JournalEditorViewModel
import com.janusleaf.app.presentation.viewmodel.JournalListViewModel
import com.janusleaf.app.presentation.viewmodel.MoodInsightsViewModel
import com.janusleaf.app.presentation.viewmodel.ProfileViewModel
import com.janusleaf.app.presentation.viewmodel.WelcomeViewModel
import org.koin.dsl.module

val uiModule = module {
    single { AuthStore(get(), get()) }
    single<JournalCache> { InMemoryJournalCache() }
    single<InspirationCache> { InMemoryInspirationCache() }
    single { JournalStore(get(), get()) }
    single { InspirationStore(get(), get(), get(), get()) }

    factory { AuthFormViewModel(get()) }
    factory { JournalListViewModel(get(), get(), get()) }
    factory { JournalEditorViewModel(get()) }
    factory { MoodInsightsViewModel(get(), get()) }
    factory { ProfileViewModel(get(), get()) }
    factory { WelcomeViewModel(get()) }
}
