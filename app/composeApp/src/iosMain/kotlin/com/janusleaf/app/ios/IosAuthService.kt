package com.janusleaf.app.ios

import com.janusleaf.app.data.local.IosSecureTokenStorage
import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.createApiHttpClient
import com.janusleaf.app.data.remote.getPlatformBaseUrl
import com.janusleaf.app.data.repository.AuthRepositoryImpl
import com.janusleaf.app.domain.model.AuthResult
import com.janusleaf.app.domain.model.User
import com.janusleaf.app.domain.repository.AuthRepository
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS-friendly authentication service.
 * This class provides a simple API for SwiftUI to interact with.
 */
class IosAuthService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val baseUrl = getPlatformBaseUrl()
    private val tokenStorage = IosSecureTokenStorage()
    private val httpClient = createApiHttpClient()
    private val apiService = AuthApiService(httpClient, baseUrl)
    private val repository: AuthRepository = AuthRepositoryImpl(apiService, tokenStorage)
    
    // Observable state for SwiftUI
    private val _isLoading = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticatedFlow: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUserFlow: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessageFlow: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Current values (for synchronous access)
    val isLoading: Boolean get() = _isLoading.value
    val isAuthenticated: Boolean get() = _isAuthenticated.value
    val currentUser: User? get() = _currentUser.value
    val errorMessage: String? get() = _errorMessage.value
    
    init {
        // Initialize Napier logging
        Napier.base(DebugAntilog())
        Napier.i("========================================", tag = "iOS")
        Napier.i("IosAuthService initialized", tag = "iOS")
        Napier.i("Base URL: $baseUrl", tag = "iOS")
        Napier.i("========================================", tag = "iOS")
        
        // Check initial auth state
        checkAuthState()
    }
    
    /**
     * Check if user is currently authenticated.
     * If authenticated, also fetches the user profile.
     */
    fun checkAuthState() {
        scope.launch {
            try {
                val authenticated = repository.isAuthenticated()
                _isAuthenticated.value = authenticated
                Napier.d("Auth state checked: $authenticated", tag = "iOS")
                
                // If authenticated, fetch user profile
                if (authenticated) {
                    when (val result = repository.getCurrentUser()) {
                        is AuthResult.Success -> {
                            _currentUser.value = result.data
                            Napier.d("Fetched user on auth check: ${result.data.username}", tag = "iOS")
                        }
                        is AuthResult.Error -> {
                            Napier.e("Failed to fetch user on auth check: ${result.error}", tag = "iOS")
                        }
                        is AuthResult.Loading -> {}
                    }
                }
            } catch (e: Exception) {
                Napier.e("Error checking auth state: ${e.message}", e, tag = "iOS")
                _isAuthenticated.value = false
            }
        }
    }
    
    /**
     * Login with email and password.
     * @param email User's email
     * @param password User's password
     * @param onSuccess Callback when login succeeds
     * @param onError Callback when login fails with error message
     */
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                when (val result = repository.login(email, password)) {
                    is AuthResult.Success -> {
                        _currentUser.value = result.data.user
                        _isAuthenticated.value = true
                        _isLoading.value = false
                        Napier.d("Login successful: ${result.data.user.email}", tag = "iOS")
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        val message = result.error.toUserMessage()
                        _errorMessage.value = message
                        _isLoading.value = false
                        Napier.e("Login failed: $message", tag = "iOS")
                        onError(message)
                    }
                    is AuthResult.Loading -> {
                        // Already loading
                    }
                }
            } catch (e: Exception) {
                val message = e.message ?: "An unexpected error occurred"
                _errorMessage.value = message
                _isLoading.value = false
                Napier.e("Login exception: $message", e, tag = "iOS")
                onError(message)
            }
        }
    }
    
    /**
     * Register a new user.
     * @param email User's email
     * @param username User's display name
     * @param password User's password
     * @param onSuccess Callback when registration succeeds
     * @param onError Callback when registration fails with error message
     */
    fun register(
        email: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                when (val result = repository.register(email, username, password)) {
                    is AuthResult.Success -> {
                        _currentUser.value = result.data.user
                        _isAuthenticated.value = true
                        _isLoading.value = false
                        Napier.d("Registration successful: ${result.data.user.email}", tag = "iOS")
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        val message = result.error.toUserMessage()
                        _errorMessage.value = message
                        _isLoading.value = false
                        Napier.e("Registration failed: $message", tag = "iOS")
                        onError(message)
                    }
                    is AuthResult.Loading -> {
                        // Already loading
                    }
                }
            } catch (e: Exception) {
                val message = e.message ?: "An unexpected error occurred"
                _errorMessage.value = message
                _isLoading.value = false
                Napier.e("Registration exception: $message", e, tag = "iOS")
                onError(message)
            }
        }
    }
    
    /**
     * Logout the current user.
     */
    fun logout(onComplete: () -> Unit) {
        scope.launch {
            try {
                val refreshToken = tokenStorage.getRefreshToken()
                if (refreshToken != null) {
                    repository.logout(refreshToken)
                } else {
                    repository.clearAuthData()
                }
                _currentUser.value = null
                _isAuthenticated.value = false
                Napier.d("Logout successful", tag = "iOS")
                onComplete()
            } catch (e: Exception) {
                // Still clear local data even on error
                repository.clearAuthData()
                _currentUser.value = null
                _isAuthenticated.value = false
                Napier.e("Logout error: ${e.message}", e, tag = "iOS")
                onComplete()
            }
        }
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Fetch the current user profile from the server.
     * Updates _currentUser on success.
     */
    fun fetchCurrentUser(
        onSuccess: (User) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                when (val result = repository.getCurrentUser()) {
                    is AuthResult.Success -> {
                        _currentUser.value = result.data
                        Napier.d("Fetched user: ${result.data.username}", tag = "iOS")
                        onSuccess(result.data)
                    }
                    is AuthResult.Error -> {
                        val message = result.error.toUserMessage()
                        Napier.e("Failed to fetch user: $message", tag = "iOS")
                        onError(message)
                    }
                    is AuthResult.Loading -> {}
                }
            } catch (e: Exception) {
                val message = e.message ?: "Failed to fetch user"
                Napier.e("Fetch user exception: $message", e, tag = "iOS")
                onError(message)
            }
        }
    }
    
    // ==================== Observers for Swift ====================
    
    /**
     * Observe loading state changes with a simple callback.
     * Returns a Cancellable that can be used to stop observing.
     */
    fun observeLoading(callback: (Boolean) -> Unit): Cancellable {
        val job = scope.launch {
            isLoadingFlow.collect { value ->
                callback(value)
            }
        }
        return Cancellable { job.cancel() }
    }
    
    /**
     * Observe authentication state changes with a simple callback.
     * Returns a Cancellable that can be used to stop observing.
     */
    fun observeAuthenticated(callback: (Boolean) -> Unit): Cancellable {
        val job = scope.launch {
            isAuthenticatedFlow.collect { value ->
                callback(value)
            }
        }
        return Cancellable { job.cancel() }
    }
    
    /**
     * Observe current user changes with a simple callback.
     * Returns a Cancellable that can be used to stop observing.
     */
    fun observeUser(callback: (User?) -> Unit): Cancellable {
        val job = scope.launch {
            currentUserFlow.collect { value ->
                callback(value)
            }
        }
        return Cancellable { job.cancel() }
    }
    
    /**
     * Observe error message changes with a simple callback.
     * Returns a Cancellable that can be used to stop observing.
     */
    fun observeError(callback: (String?) -> Unit): Cancellable {
        val job = scope.launch {
            errorMessageFlow.collect { value ->
                callback(value)
            }
        }
        return Cancellable { job.cancel() }
    }
    
    // ==================== Validation Helpers ====================
    
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }
    
    fun isValidUsername(username: String): Boolean {
        return username.length in 2..50
    }
}

/**
 * Factory function to create the auth service.
 * Call this from Swift to get an instance.
 */
fun createAuthService(): IosAuthService = IosAuthService()
