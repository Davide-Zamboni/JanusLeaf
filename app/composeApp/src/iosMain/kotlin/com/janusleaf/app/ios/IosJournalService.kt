package com.janusleaf.app.ios

import com.janusleaf.app.data.local.IosSecureTokenStorage
import com.janusleaf.app.data.remote.JournalApiService
import com.janusleaf.app.data.remote.createApiHttpClient
import com.janusleaf.app.data.remote.getPlatformBaseUrl
import com.janusleaf.app.data.repository.JournalRepositoryImpl
import com.janusleaf.app.domain.model.*
import com.janusleaf.app.domain.repository.JournalRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * iOS-friendly journal service.
 * This class provides a simple API for SwiftUI to interact with journal functionality.
 */
class IosJournalService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val baseUrl = getPlatformBaseUrl()
    private val tokenStorage = IosSecureTokenStorage()
    private val httpClient = createApiHttpClient()
    private val apiService = JournalApiService(httpClient, baseUrl)
    private val repository: JournalRepository = JournalRepositoryImpl(apiService, tokenStorage)
    
    // Observable state for SwiftUI
    private val _isLoading = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessageFlow: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _entries = MutableStateFlow<List<JournalPreview>>(emptyList())
    val entriesFlow: StateFlow<List<JournalPreview>> = _entries.asStateFlow()
    
    private val _currentEntry = MutableStateFlow<Journal?>(null)
    val currentEntryFlow: StateFlow<Journal?> = _currentEntry.asStateFlow()
    
    private val _hasMore = MutableStateFlow(true)
    val hasMoreFlow: StateFlow<Boolean> = _hasMore.asStateFlow()
    
    // Pagination state
    private var currentPage = 0
    private var isLoadingMore = false
    
    // Current values (for synchronous access)
    val isLoading: Boolean get() = _isLoading.value
    val errorMessage: String? get() = _errorMessage.value
    val entries: List<JournalPreview> get() = _entries.value
    val currentEntry: Journal? get() = _currentEntry.value
    val hasMore: Boolean get() = _hasMore.value
    
    init {
        Napier.i("IosJournalService initialized", tag = "JournalService")
    }
    
    /**
     * Load the first page of journal entries.
     */
    fun loadEntries(onComplete: () -> Unit = {}) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0
            
            when (val result = repository.listEntries(page = 0, size = 20)) {
                is JournalResult.Success -> {
                    _entries.value = result.data.entries
                    _hasMore.value = result.data.hasNext
                    currentPage = 0
                    Napier.d("Loaded ${result.data.entries.size} entries", tag = "JournalService")
                }
                is JournalResult.Error -> {
                    _errorMessage.value = result.error.toUserMessage()
                    Napier.e("Failed to load entries: ${result.error}", tag = "JournalService")
                }
                is JournalResult.Loading -> {}
            }
            
            _isLoading.value = false
            onComplete()
        }
    }
    
    /**
     * Load more entries (pagination).
     */
    fun loadMoreEntries(onComplete: () -> Unit = {}) {
        if (isLoadingMore || !_hasMore.value) {
            onComplete()
            return
        }
        
        scope.launch {
            isLoadingMore = true
            
            when (val result = repository.listEntries(page = currentPage + 1, size = 20)) {
                is JournalResult.Success -> {
                    val currentEntries = _entries.value.toMutableList()
                    currentEntries.addAll(result.data.entries)
                    _entries.value = currentEntries
                    _hasMore.value = result.data.hasNext
                    currentPage = result.data.page
                    Napier.d("Loaded ${result.data.entries.size} more entries (page $currentPage)", tag = "JournalService")
                }
                is JournalResult.Error -> {
                    _errorMessage.value = result.error.toUserMessage()
                }
                is JournalResult.Loading -> {}
            }
            
            isLoadingMore = false
            onComplete()
        }
    }
    
    /**
     * Create a new journal entry.
     */
    fun createEntry(
        title: String?,
        body: String?,
        entryDate: String?,
        onSuccess: (Journal) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val localDate = entryDate?.let { 
                try { LocalDate.parse(it) } catch (e: Exception) { null }
            }
            
            when (val result = repository.createEntry(title, body, localDate)) {
                is JournalResult.Success -> {
                    _currentEntry.value = result.data
                    // Add to the beginning of the list
                    val currentEntries = _entries.value.toMutableList()
                    currentEntries.add(0, JournalPreview(
                        id = result.data.id,
                        title = result.data.title,
                        bodyPreview = result.data.body.take(150),
                        moodScore = result.data.moodScore,
                        entryDate = result.data.entryDate,
                        updatedAt = result.data.updatedAt
                    ))
                    _entries.value = currentEntries
                    _isLoading.value = false
                    Napier.d("Created entry: ${result.data.id}", tag = "JournalService")
                    onSuccess(result.data)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _errorMessage.value = message
                    _isLoading.value = false
                    onError(message)
                }
                is JournalResult.Loading -> {}
            }
        }
    }
    
    /**
     * Get a journal entry by ID.
     */
    fun getEntry(
        id: String,
        onSuccess: (Journal) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = repository.getEntry(id)) {
                is JournalResult.Success -> {
                    _currentEntry.value = result.data
                    _isLoading.value = false
                    onSuccess(result.data)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _errorMessage.value = message
                    _isLoading.value = false
                    onError(message)
                }
                is JournalResult.Loading -> {}
            }
        }
    }
    
    /**
     * Update the body of a journal entry.
     * Called frequently for auto-save functionality.
     */
    fun updateBody(
        id: String,
        body: String,
        expectedVersion: Long?,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            when (val result = repository.updateBody(id, body, expectedVersion)) {
                is JournalResult.Success -> {
                    // Update current entry if it matches
                    _currentEntry.value?.let { current ->
                        if (current.id == id) {
                            _currentEntry.value = current.copy(
                                body = result.data.body,
                                version = result.data.version,
                                updatedAt = result.data.updatedAt
                            )
                        }
                    }
                    // Update preview in list
                    updateEntryInList(id, bodyPreview = body.take(150))
                    onSuccess(result.data.version)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _errorMessage.value = message
                    onError(message)
                }
                is JournalResult.Loading -> {}
            }
        }
    }
    
    /**
     * Update metadata (title, mood score) of a journal entry.
     */
    fun updateMetadata(
        id: String,
        title: String?,
        moodScore: Int?,
        onSuccess: (Journal) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _isLoading.value = true
            
            when (val result = repository.updateMetadata(id, title, moodScore)) {
                is JournalResult.Success -> {
                    _currentEntry.value = result.data
                    // Update in list
                    updateEntryInList(
                        id,
                        title = title,
                        moodScore = moodScore
                    )
                    _isLoading.value = false
                    onSuccess(result.data)
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _errorMessage.value = message
                    _isLoading.value = false
                    onError(message)
                }
                is JournalResult.Loading -> {}
            }
        }
    }
    
    /**
     * Delete a journal entry.
     */
    fun deleteEntry(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            _isLoading.value = true
            
            when (val result = repository.deleteEntry(id)) {
                is JournalResult.Success -> {
                    // Remove from list
                    _entries.value = _entries.value.filter { it.id != id }
                    // Clear current entry if it was deleted
                    if (_currentEntry.value?.id == id) {
                        _currentEntry.value = null
                    }
                    _isLoading.value = false
                    Napier.d("Deleted entry: $id", tag = "JournalService")
                    onSuccess()
                }
                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _errorMessage.value = message
                    _isLoading.value = false
                    onError(message)
                }
                is JournalResult.Loading -> {}
            }
        }
    }
    
    /**
     * Clear the current entry (when navigating away from editor).
     */
    fun clearCurrentEntry() {
        _currentEntry.value = null
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    // Helper to update an entry in the list
    private fun updateEntryInList(
        id: String,
        title: String? = null,
        bodyPreview: String? = null,
        moodScore: Int? = null
    ) {
        val currentEntries = _entries.value.toMutableList()
        val index = currentEntries.indexOfFirst { it.id == id }
        if (index >= 0) {
            val entry = currentEntries[index]
            currentEntries[index] = entry.copy(
                title = title ?: entry.title,
                bodyPreview = bodyPreview ?: entry.bodyPreview,
                moodScore = moodScore ?: entry.moodScore
            )
            _entries.value = currentEntries
        }
    }
    
    // ==================== Observers for Swift ====================
    
    fun observeLoading(callback: (Boolean) -> Unit): Cancellable {
        val job = scope.launch {
            isLoadingFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
    
    fun observeError(callback: (String?) -> Unit): Cancellable {
        val job = scope.launch {
            errorMessageFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
    
    fun observeEntries(callback: (List<JournalPreview>) -> Unit): Cancellable {
        val job = scope.launch {
            entriesFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
    
    fun observeCurrentEntry(callback: (Journal?) -> Unit): Cancellable {
        val job = scope.launch {
            currentEntryFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
    
    fun observeHasMore(callback: (Boolean) -> Unit): Cancellable {
        val job = scope.launch {
            hasMoreFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
}

/**
 * Factory function to create the journal service.
 */
fun createJournalService(): IosJournalService = IosJournalService()
