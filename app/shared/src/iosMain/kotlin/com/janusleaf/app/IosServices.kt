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

    fun createAuthService(): IosAuthService = IosAuthService(authRepository, tokenStorage)

    fun createJournalService(): IosJournalService = IosJournalService(journalRepository, tokenStorage)

    fun createInspirationService(): IosInspirationService = IosInspirationService(inspirationApi, tokenStorage)
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
    private val journalRepository: JournalRepository,
    private val tokenStorage: TokenStorage
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val entries = MutableStateFlow<List<JournalPreview>>(emptyList())
    private val currentEntry = MutableStateFlow<Journal?>(null)
    private val hasMore = MutableStateFlow(true)

    private var currentPage = 0
    private val pageSize = 20

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
                journalRepository.listEntries(currentPage, pageSize)
            }
            handlePageResult(result, replace = true)
            isLoading.value = false
            onComplete()
        }
    }

    fun loadMoreEntries(onComplete: () -> Unit) {
        if (!hasMore.value) {
            onComplete()
            return
        }
        scope.launch {
            isLoading.value = true
            val nextPage = currentPage + 1
            val result = withContext(Dispatchers.Default) {
                journalRepository.listEntries(nextPage, pageSize)
            }
            handlePageResult(result, replace = false, page = nextPage)
            isLoading.value = false
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
                journalRepository.createEntry(title, body, parsedDate)
            }
            when (result) {
                is JournalResult.Success -> {
                    currentEntry.value = result.data
                    entries.value = listOf(result.data.toPreview()) + entries.value
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
                journalRepository.getEntry(id)
            }
            when (result) {
                is JournalResult.Success -> {
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
            val result = withContext(Dispatchers.Default) {
                journalRepository.updateBody(id, body, expectedVersion)
            }
            when (result) {
                is JournalResult.Success -> {
                    val update = result.data
                    currentEntry.value = currentEntry.value?.copy(
                        body = update.body,
                        version = update.version,
                        updatedAt = update.updatedAt
                    )
                    errorMessage.value = null
                    onSuccess(update.version)
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
            val result = withContext(Dispatchers.Default) {
                journalRepository.updateMetadata(id, title, moodScore, expectedVersion = null)
            }
            when (result) {
                is JournalResult.Success -> {
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

    fun deleteEntry(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            isLoading.value = true
            val result = withContext(Dispatchers.Default) {
                journalRepository.deleteEntry(id)
            }
            when (result) {
                is JournalResult.Success -> {
                    entries.value = entries.value.filterNot { it.id == id }
                    if (currentEntry.value?.id == id) {
                        currentEntry.value = null
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
        currentEntry.value = null
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun handlePageResult(
        result: JournalResult<JournalPage>,
        replace: Boolean,
        page: Int = 0
    ) {
        when (result) {
            is JournalResult.Success -> {
                if (replace) {
                    entries.value = result.data.entries
                } else {
                    entries.value = entries.value + result.data.entries
                }
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

    private fun Journal.toPreview(): JournalPreview {
        val previewText = if (body.length <= 120) body else body.take(120) + "…"
        return JournalPreview(
            id = id,
            title = title,
            bodyPreview = previewText,
            moodScore = moodScore,
            entryDate = entryDate,
            updatedAt = updatedAt
        )
    }
}

class IosInspirationService(
    private val inspirationApiService: InspirationApiService,
    private val tokenStorage: TokenStorage
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val quote = MutableStateFlow<InspirationalQuote?>(null)
    private val notFound = MutableStateFlow(false)

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
            val accessToken = tokenStorage.getAccessToken()
            if (accessToken == null) {
                val message = InspirationError.Unauthorized.toUserMessage()
                errorMessage.value = message
                notFound.value = false
                isLoading.value = false
                onComplete()
                return@launch
            }
            val result = withContext(Dispatchers.Default) {
                inspirationApiService.getInspiration(accessToken)
            }
            when (result) {
                is InspirationResult.Success -> {
                    quote.value = result.data.toDomain()
                    notFound.value = false
                    errorMessage.value = null
                }
                is InspirationResult.Error -> {
                    if (result.error is InspirationError.NotFound) {
                        notFound.value = true
                    }
                    errorMessage.value = result.error.toUserMessage()
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
