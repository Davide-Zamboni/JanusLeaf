package com.janusleaf.app.model.cache

import com.janusleaf.app.domain.model.InspirationalQuote
import kotlinx.coroutines.flow.Flow

interface InspirationCache {
    fun observeQuote(): Flow<InspirationalQuote?>

    suspend fun getQuote(): InspirationalQuote?

    suspend fun setQuote(quote: InspirationalQuote?)

    suspend fun clear()
}
