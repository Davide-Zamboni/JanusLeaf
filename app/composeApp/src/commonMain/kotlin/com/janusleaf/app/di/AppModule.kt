package com.janusleaf.app.di

import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.createApiHttpClient
import com.janusleaf.app.data.repository.AuthRepositoryImpl
import com.janusleaf.app.domain.repository.AuthRepository
import com.janusleaf.app.presentation.auth.AuthViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Common Koin module with shared dependencies.
 */
val commonModule = module {
    // HTTP Client
    single { createApiHttpClient() }
    
    // API Services
    single { AuthApiService(get()) }
    
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    
    // ViewModels
    viewModelOf(::AuthViewModel)
}

/**
 * Platform-specific module placeholder.
 * Each platform will provide its own implementation.
 */
expect val platformModule: org.koin.core.module.Module
