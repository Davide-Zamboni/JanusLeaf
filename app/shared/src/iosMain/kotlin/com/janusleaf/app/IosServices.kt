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
import com.janusleaf.app.presentation.viewmodel.AuthViewModel
import com.janusleaf.app.presentation.viewmodel.JournalEditorViewModel
import com.janusleaf.app.presentation.viewmodel.JournalListViewModel
import com.janusleaf.app.presentation.viewmodel.MoodInsightsViewModel
import com.janusleaf.app.presentation.viewmodel.ProfileViewModel
import com.janusleaf.app.presentation.viewmodel.WelcomeViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

interface Cancellable {
    fun cancel()
}

private class FlowCancellable(private val job: Job) : Cancellable {
    override fun cancel() {
        job.cancel()
    }
}

class FlowObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun <T> observe(flow: Flow<T>, callback: (T) -> Unit): Cancellable {
        val job = scope.launch {
            try {
                flow.collect { value -> callback(value) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Flow collection failed", throwable, tag = "FlowObserver")
            }
        }
        return FlowCancellable(job)
    }

    fun cancel() {
        scope.cancel()
    }
}

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

    fun createAuthViewModel(): AuthViewModel = AuthViewModel(authStore)

    fun createJournalListViewModel(): JournalListViewModel =
        JournalListViewModel(authStore, journalStore, inspirationStore)

    fun createJournalEditorViewModel(): JournalEditorViewModel = JournalEditorViewModel(journalStore)

    fun createMoodInsightsViewModel(): MoodInsightsViewModel = MoodInsightsViewModel(journalStore)

    fun createProfileViewModel(): ProfileViewModel = ProfileViewModel(authStore, journalStore)

    fun createWelcomeViewModel(): WelcomeViewModel = WelcomeViewModel(authStore)

    fun parseLocalDate(iso: String?): LocalDate? = try {
        iso?.let(LocalDate::parse)
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        Napier.e("Failed to parse local date", throwable, tag = "SharedModule")
        null
    }
}
