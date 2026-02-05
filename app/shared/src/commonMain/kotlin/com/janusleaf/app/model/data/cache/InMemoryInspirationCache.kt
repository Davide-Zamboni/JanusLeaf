package com.janusleaf.app.model.data.cache

import com.janusleaf.app.domain.model.InspirationalQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryInspirationCache : InspirationCache {
    private val quoteState = MutableStateFlow<InspirationalQuote?>(null)

    override fun observeQuote(): Flow<InspirationalQuote?> = quoteState.asStateFlow()

    override suspend fun getQuote(): InspirationalQuote? = quoteState.value

    override suspend fun setQuote(quote: InspirationalQuote?) {
        quoteState.value = quote
    }

    override suspend fun clear() {
        quoteState.value = null
    }
}
