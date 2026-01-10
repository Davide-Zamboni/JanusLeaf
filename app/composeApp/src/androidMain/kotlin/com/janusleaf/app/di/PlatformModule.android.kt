package com.janusleaf.app.di

import com.janusleaf.app.data.local.AndroidSecureTokenStorage
import com.janusleaf.app.domain.repository.TokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<TokenStorage> { AndroidSecureTokenStorage(androidContext()) }
}
