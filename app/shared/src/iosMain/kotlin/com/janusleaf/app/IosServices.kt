package com.janusleaf.app

import com.janusleaf.app.data.local.IosTokenStorage
import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.InspirationApiService
import com.janusleaf.app.data.remote.JournalApiService
import com.janusleaf.app.data.remote.createApiHttpClient
import com.janusleaf.app.data.remote.getPlatformBaseUrl
import com.janusleaf.app.data.repository.AuthRepositoryImpl
import com.janusleaf.app.data.repository.JournalRepositoryImpl
import com.janusleaf.app.domain.repository.AuthRepository
import com.janusleaf.app.domain.repository.JournalRepository
import com.janusleaf.app.domain.repository.TokenStorage
import com.janusleaf.app.model.cache.InMemoryInspirationCache
import com.janusleaf.app.model.cache.InMemoryJournalCache
import com.janusleaf.app.model.store.AuthStore
import com.janusleaf.app.model.store.InspirationStore
import com.janusleaf.app.model.store.JournalStore
import com.janusleaf.app.presentation.viewmodel.AuthFormViewModel
import com.janusleaf.app.presentation.viewmodel.JournalEditorViewModel
import com.janusleaf.app.presentation.viewmodel.JournalListViewModel
import com.janusleaf.app.presentation.viewmodel.MoodInsightsViewModel
import com.janusleaf.app.presentation.viewmodel.ObservableAuthFormViewModel
import com.janusleaf.app.presentation.viewmodel.ObservableJournalEditorViewModel
import com.janusleaf.app.presentation.viewmodel.ObservableJournalListViewModel
import com.janusleaf.app.presentation.viewmodel.ObservableMoodInsightsViewModel
import com.janusleaf.app.presentation.viewmodel.ObservableSessionViewModel
import com.janusleaf.app.presentation.viewmodel.WelcomeViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

object SharedModule {
    private val tokenStorage: TokenStorage by lazy { IosTokenStorage() }
    private val httpClient by lazy { createApiHttpClient() }
    private val authApi by lazy { AuthApiService(httpClient, getPlatformBaseUrl()) }
    private val journalApi by lazy { JournalApiService(httpClient, getPlatformBaseUrl()) }
    private val inspirationApi by lazy { InspirationApiService(httpClient, getPlatformBaseUrl()) }
    private val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authApi, tokenStorage) }
    private val journalRepository: JournalRepository by lazy { JournalRepositoryImpl(journalApi, tokenStorage, authApi) }
    private val authStore: AuthStore by lazy { AuthStore(authRepository, tokenStorage) }
    private val journalStore: JournalStore by lazy { JournalStore(journalRepository, InMemoryJournalCache()) }
    private val inspirationStore: InspirationStore by lazy {
        InspirationStore(inspirationApi, authApi, tokenStorage, InMemoryInspirationCache())
    }

    fun createAuthFormViewModel(): AuthFormViewModel = AuthFormViewModel(authStore)

    fun createObservableAuthFormViewModel(): ObservableAuthFormViewModel =
        ObservableAuthFormViewModel(createAuthFormViewModel())

    fun createJournalListViewModel(): JournalListViewModel =
        JournalListViewModel(authStore, journalStore, inspirationStore)

    fun createObservableJournalListViewModel(): ObservableJournalListViewModel =
        ObservableJournalListViewModel(createJournalListViewModel())

    fun createJournalEditorViewModel(): JournalEditorViewModel = JournalEditorViewModel(journalStore)

    fun createObservableJournalEditorViewModel(): ObservableJournalEditorViewModel =
        ObservableJournalEditorViewModel(createJournalEditorViewModel())

    fun createMoodInsightsViewModel(): MoodInsightsViewModel = MoodInsightsViewModel(journalStore, authStore)

    fun createObservableMoodInsightsViewModel(): ObservableMoodInsightsViewModel =
        ObservableMoodInsightsViewModel(createMoodInsightsViewModel())

    fun createWelcomeViewModel(): WelcomeViewModel = WelcomeViewModel(authStore)

    fun createObservableSessionViewModel(): ObservableSessionViewModel =
        ObservableSessionViewModel(createWelcomeViewModel())

    fun parseLocalDate(iso: String?): LocalDate? = try {
        iso?.let(LocalDate::parse)
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        Napier.e("Failed to parse local date", throwable, tag = "SharedModule")
        null
    }
}
