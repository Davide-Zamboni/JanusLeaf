package com.janusleaf.app.data.repository

import com.janusleaf.app.data.remote.JournalApiService
import com.janusleaf.app.domain.model.*
import com.janusleaf.app.domain.repository.JournalRepository
import com.janusleaf.app.domain.repository.TokenStorage
import io.github.aakira.napier.Napier
import kotlinx.datetime.LocalDate

/**
 * Implementation of JournalRepository.
 * Coordinates between the API service and token storage for authenticated requests.
 */
class JournalRepositoryImpl(
    private val apiService: JournalApiService,
    private val tokenStorage: TokenStorage
) : JournalRepository {
    
    private suspend fun getAccessToken(): String? = tokenStorage.getAccessToken()
    
    override suspend fun createEntry(
        title: String?,
        body: String?,
        entryDate: LocalDate?
    ): JournalResult<Journal> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiService.createEntry(accessToken, title, body, entryDate)) {
            is JournalResult.Success -> {
                val journal = result.data.toDomain()
                Napier.d("Journal entry created: ${journal.id}", tag = "JournalRepository")
                JournalResult.Success(journal)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun getEntry(id: String): JournalResult<Journal> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiService.getEntry(accessToken, id)) {
            is JournalResult.Success -> {
                JournalResult.Success(result.data.toDomain())
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun listEntries(page: Int, size: Int): JournalResult<JournalPage> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiService.listEntries(accessToken, page, size)) {
            is JournalResult.Success -> {
                val journalPage = JournalPage(
                    entries = result.data.entries.map { it.toDomain() },
                    page = result.data.page,
                    size = result.data.size,
                    totalElements = result.data.totalElements,
                    totalPages = result.data.totalPages,
                    hasNext = result.data.hasNext,
                    hasPrevious = result.data.hasPrevious
                )
                JournalResult.Success(journalPage)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun getEntriesByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): JournalResult<List<JournalPreview>> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiService.getEntriesByDateRange(accessToken, startDate, endDate)) {
            is JournalResult.Success -> {
                JournalResult.Success(result.data.map { it.toDomain() })
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun updateBody(
        id: String,
        body: String,
        expectedVersion: Long?
    ): JournalResult<JournalBodyUpdate> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiService.updateBody(accessToken, id, body, expectedVersion)) {
            is JournalResult.Success -> {
                val update = JournalBodyUpdate(
                    id = result.data.id,
                    body = result.data.body,
                    version = result.data.version,
                    updatedAt = result.data.updatedAt
                )
                Napier.d("Journal body updated: $id (v${update.version})", tag = "JournalRepository")
                JournalResult.Success(update)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun updateMetadata(
        id: String,
        title: String?,
        moodScore: Int?
    ): JournalResult<Journal> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiService.updateMetadata(accessToken, id, title, moodScore)) {
            is JournalResult.Success -> {
                val journal = result.data.toDomain()
                Napier.d("Journal metadata updated: $id", tag = "JournalRepository")
                JournalResult.Success(journal)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun deleteEntry(id: String): JournalResult<Unit> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiService.deleteEntry(accessToken, id)) {
            is JournalResult.Success -> {
                Napier.d("Journal entry deleted: $id", tag = "JournalRepository")
                JournalResult.Success(Unit)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
}
