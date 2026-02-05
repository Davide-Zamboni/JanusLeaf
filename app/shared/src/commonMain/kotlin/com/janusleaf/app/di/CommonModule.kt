package com.janusleaf.app.di

import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.InspirationApiService
import com.janusleaf.app.data.remote.JournalApiService
import com.janusleaf.app.data.remote.createApiHttpClient
import com.janusleaf.app.data.remote.getPlatformBaseUrl
import com.janusleaf.app.data.repository.AuthRepositoryImpl
import com.janusleaf.app.data.repository.JournalRepositoryImpl
import com.janusleaf.app.domain.repository.AuthRepository
import com.janusleaf.app.domain.repository.JournalRepository
import io.ktor.client.HttpClient
import org.koin.dsl.module

val commonModule = module {
    single<HttpClient> { createApiHttpClient() }

    single { AuthApiService(get(), getPlatformBaseUrl()) }
    single { JournalApiService(get(), getPlatformBaseUrl()) }
    single { InspirationApiService(get(), getPlatformBaseUrl()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<JournalRepository> { JournalRepositoryImpl(get(), get(), get()) }
}
