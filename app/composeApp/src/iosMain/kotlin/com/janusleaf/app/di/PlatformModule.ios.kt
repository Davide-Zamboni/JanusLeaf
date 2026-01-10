package com.janusleaf.app.di

import com.janusleaf.app.data.local.IosSecureTokenStorage
import com.janusleaf.app.domain.repository.TokenStorage
import org.koin.dsl.module

actual val platformModule = module {
    single<TokenStorage> { IosSecureTokenStorage() }
}
