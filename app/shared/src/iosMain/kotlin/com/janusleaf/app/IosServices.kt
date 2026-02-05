package com.janusleaf.app

import com.janusleaf.app.data.local.IosTokenStorage
import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.InspirationApiService
import com.janusleaf.app.data.remote.JournalApiService
import com.janusleaf.app.data.remote.createApiHttpClient
import com.janusleaf.app.data.remote.getPlatformBaseUrl
import com.janusleaf.app.data.repository.AuthRepositoryImpl
import com.janusleaf.app.data.repository.JournalRepositoryImpl
import com.janusleaf.app.domain.model.*
import com.janusleaf.app.domain.repository.AuthRepository
import com.janusleaf.app.domain.repository.JournalRepository
import com.janusleaf.app.domain.repository.TokenStorage
import com.janusleaf.app.model.data.cache.InMemoryInspirationCache
import com.janusleaf.app.model.data.cache.InMemoryJournalCache
import com.janusleaf.app.model.data.store.InspirationStore
import com.janusleaf.app.model.data.store.JournalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

interface Cancellable {
    fun cancel()
}

private class FlowCancellable(private val job: Job) : Cancellable {
    override fun cancel() {
        job.cancel()
    }
}

private fun <T> Flow<T>.collectIn(
    scope: CoroutineScope,
    callback: (T) -> Unit
): Cancellable {
    val job = scope.launch {
        collect { value ->
            callback(value)
        }
    }
    return FlowCancellable(job)
}

object SharedModule {
    private val tokenStorage: TokenStorage by lazy { IosTokenStorage() }
    private val httpClient by lazy { createApiHttpClient() }
    private val authApi by lazy { AuthApiService(httpClient, getPlatformBaseUrl()) }
    private val journalApi by lazy { JournalApiService(httpClient, getPlatformBaseUrl()) }
    private val inspirationApi by lazy { InspirationApiService(httpClient, getPlatformBaseUrl()) }
    private val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authApi, tokenStorage) }
    private val journalRepository: JournalRepository by lazy { JournalRepositoryImpl(journalApi, tokenStorage, authApi) }
    private val journalStore: JournalStore by lazy { JournalStore(journalRepository, InMemoryJournalCache()) }
    private val inspirationStore: InspirationStore by lazy {
        InspirationStore(inspirationApi, authApi, tokenStorage, InMemoryInspirationCache())
    }

    fun createAuthService(): IosAuthService = IosAuthService(authRepository, tokenStorage)

    fun createJournalService(): IosJournalService = IosJournalService(journalStore)

    fun createInspirationService(): IosInspirationService = IosInspirationService(inspirationStore)
}

class IosAuthService(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isLoading = MutableStateFlow(false)
    private val isAuthenticated = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val currentUser = MutableStateFlow<User?>(null)

    init {
        scope.launch {
            isAuthenticated.value = tokenStorage.hasTokens()
        }
    }

    fun observeLoading(callback: (Boolean) -> Unit): Cancellable =
        isLoading.asStateFlow().collectIn(scope, callback)

    fun observeAuthenticated(callback: (Boolean) -> Unit): Cancellable =
        isAuthenticated.asStateFlow().collectIn(scope, callback)

    fun observeError(callback: (String?) -> Unit): Cancellable =
        errorMessage.asStateFlow().collectIn(scope, callback)

    fun observeUser(callback: (User?) -> Unit): Cancellable =
        currentUser.asStateFlow().collectIn(scope, callback)

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            val result = withContext(Dispatchers.Default) {
                authRepository.login(email, password)
            }
            handleAuthResult(result, onSuccess, onError)
        }
    }

    fun register(
        email: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            val result = withContext(Dispatchers.Default) {
                authRepository.register(email, username, password)
            }
            handleAuthResult(result, onSuccess, onError)
        }
    }

    fun logout(onSuccess: () -> Unit) {
        scope.launch {
            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken != null) {
                withContext(Dispatchers.Default) {
                    authRepository.logout(refreshToken)
                }
            } else {
                tokenStorage.clearTokens()
            }
            currentUser.value = null
            isAuthenticated.value = false
            onSuccess()
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length >= 5
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun isValidUsername(username: String): Boolean {
        return username.length >= 3
    }

    private fun handleAuthResult(
        result: AuthResult<AuthenticatedUser>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        isLoading.value = false
        when (result) {
            is AuthResult.Success -> {
                currentUser.value = result.data.user
                isAuthenticated.value = true
                errorMessage.value = null
                onSuccess()
            }
            is AuthResult.Error -> {
                val message = result.error.toUserMessage()
                errorMessage.value = message
                isAuthenticated.value = false
                onError(message)
            }
            is AuthResult.Loading -> {
                isLoading.value = true
            }
        }
    }
}

class IosJournalService(
    private val journalStore: JournalStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val entries = MutableStateFlow<List<JournalPreview>>(emptyList())
    private val currentEntry = MutableStateFlow<Journal?>(null)
    private val hasMore = MutableStateFlow(true)

    private var currentPage = 0
    private val pageSize = 20
    private var isLoadingMore = false
    private var currentEntryId: String? = null
    private var currentEntryJob: Job? = null

    init {
        scope.launch {
            journalStore.observeEntries().collect { entries.value = it }
        }
    }

    fun observeLoading(callback: (Boolean) -> Unit): Cancellable =
        isLoading.asStateFlow().collectIn(scope, callback)

    fun observeError(callback: (String?) -> Unit): Cancellable =
        errorMessage.asStateFlow().collectIn(scope, callback)

    fun observeEntries(callback: (List<JournalPreview>) -> Unit): Cancellable =
        entries.asStateFlow().collectIn(scope, callback)

    fun observeCurrentEntry(callback: (Journal?) -> Unit): Cancellable =
        currentEntry.asStateFlow().collectIn(scope, callback)

    fun observeHasMore(callback: (Boolean) -> Unit): Cancellable =
        hasMore.asStateFlow().collectIn(scope, callback)

    fun loadEntries(onComplete: () -> Unit) {
        scope.launch {
            isLoading.value = true
            currentPage = 0
            val result = withContext(Dispatchers.Default) {
                journalStore.loadEntries(page = currentPage, size = pageSize)
            }
            handlePageResult(result, page = currentPage)
            isLoading.value = false
            onComplete()
        }
    }

    fun loadMoreEntries(onComplete: () -> Unit) {
        if (!hasMore.value || isLoadingMore) {
            onComplete()
            return
        }
        scope.launch {
            isLoadingMore = true
            isLoading.value = true
            val nextPage = currentPage + 1
            val result = withContext(Dispatchers.Default) {
                journalStore.loadEntries(page = nextPage, size = pageSize)
            }
            handlePageResult(result, page = nextPage)
            isLoading.value = false
            isLoadingMore = false
            onComplete()
        }
    }

    fun createEntry(
        title: String?,
        body: String?,
        entryDate: String?,
        onSuccess: (Journal) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            val parsedDate = entryDate?.let { LocalDate.parse(it) }
            val result = withContext(Dispatchers.Default) {
                journalStore.createEntry(title, body, parsedDate)
            }
            when (result) {
                is JournalResult.Success -> {
                    bindCurrentEntry(result.data.id)
                    currentEntry.value = result.data
                    errorMessage.value = null
                    onSuccess(result.data)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    errorMessage.value = message
                    onError(message)
                }
                is JournalResult.Loading -> Unit
            }
            isLoading.value = false
        }
    }

    fun getEntry(
        id: String,
        onSuccess: (Journal) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            val result = withContext(Dispatchers.Default) {
                journalStore.getEntry(id)
            }
            when (result) {
                is JournalResult.Success -> {
                    bindCurrentEntry(id)
                    currentEntry.value = result.data
                    errorMessage.value = null
                    onSuccess(result.data)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    errorMessage.value = message
                    onError(message)
                }
                is JournalResult.Loading -> Unit
            }
            isLoading.value = false
        }
    }

    fun updateBody(
        id: String,
        body: String,
        expectedVersion: Long?,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            bindCurrentEntry(id)
            val result = withContext(Dispatchers.Default) {
                journalStore.updateBody(id, body, expectedVersion)
            }
            when (result) {
                is JournalResult.Success -> {
                    errorMessage.value = null
                    onSuccess(result.data.version)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    errorMessage.value = message
                    onError(message)
                }
                is JournalResult.Loading -> Unit
            }
            isLoading.value = false
        }
    }

    fun updateMetadata(
        id: String,
        title: String?,
        moodScore: Int?,
        onSuccess: (Journal) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            bindCurrentEntry(id)
            val result = withContext(Dispatchers.Default) {
                journalStore.updateMetadata(id, title, moodScore, expectedVersion = null)
            }
            when (result) {
                is JournalResult.Success -> {
                    errorMessage.value = null
                    onSuccess(result.data)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    errorMessage.value = message
                    onError(message)
                }
                is JournalResult.Loading -> Unit
            }
            isLoading.value = false
        }
    }

    fun deleteEntry(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            val result = withContext(Dispatchers.Default) {
                journalStore.deleteEntry(id)
            }
            when (result) {
                is JournalResult.Success -> {
                    if (currentEntryId == id) {
                        clearCurrentEntry()
                    }
                    errorMessage.value = null
                    onSuccess()
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    errorMessage.value = message
                    onError(message)
                }
                is JournalResult.Loading -> Unit
            }
            isLoading.value = false
        }
    }

    fun clearCurrentEntry() {
        currentEntryId = null
        currentEntryJob?.cancel()
        currentEntry.value = null
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun handlePageResult(result: JournalResult<JournalPage>, page: Int) {
        when (result) {
            is JournalResult.Success -> {
                currentPage = page
                hasMore.value = result.data.hasNext
                errorMessage.value = null
            }
            is JournalResult.Error -> {
                errorMessage.value = result.error.toUserMessage()
            }
            is JournalResult.Loading -> Unit
        }
    }

    private fun bindCurrentEntry(entryId: String) {
        if (currentEntryId == entryId) return
        currentEntryId = entryId
        currentEntryJob?.cancel()
        currentEntryJob = scope.launch {
            journalStore.observeEntry(entryId).collect { entry ->
                currentEntry.value = entry
            }
        }
    }
}

class IosInspirationService(
    private val inspirationStore: InspirationStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val quote = MutableStateFlow<InspirationalQuote?>(null)
    private val notFound = MutableStateFlow(false)

    init {
        scope.launch {
            inspirationStore.observeQuote().collect { quote.value = it }
        }
    }

    fun observeLoading(callback: (Boolean) -> Unit): Cancellable =
        isLoading.asStateFlow().collectIn(scope, callback)

    fun observeError(callback: (String?) -> Unit): Cancellable =
        errorMessage.asStateFlow().collectIn(scope, callback)

    fun observeQuote(callback: (InspirationalQuote?) -> Unit): Cancellable =
        quote.asStateFlow().collectIn(scope, callback)

    fun observeNotFound(callback: (Boolean) -> Unit): Cancellable =
        notFound.asStateFlow().collectIn(scope, callback)

    fun fetchQuote(onComplete: () -> Unit) {
        scope.launch {
            isLoading.value = true
            val result = withContext(Dispatchers.Default) {
                inspirationStore.fetchQuote()
            }
            when (result) {
                is InspirationResult.Success -> {
                    notFound.value = false
                    errorMessage.value = null
                }
                is InspirationResult.Error -> {
                    if (result.error is InspirationError.NotFound) {
                        notFound.value = true
                        errorMessage.value = null
                    } else {
                        notFound.value = false
                        errorMessage.value = result.error.toUserMessage()
                    }
                }
                is InspirationResult.Loading -> Unit
            }
            isLoading.value = false
            onComplete()
        }
    }

    fun refresh(onComplete: () -> Unit) {
        fetchQuote(onComplete)
    }

    fun clearError() {
        errorMessage.value = null
    }
}
