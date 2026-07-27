package com.btween.app.domain.repository

import com.btween.app.domain.model.Quote
import com.btween.app.domain.model.QuoteFilter
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {

    fun observeFilteredQuotes(filter: QuoteFilter): Flow<List<Quote>>

    fun observeQuoteById(id: Long): Flow<Quote?>

    suspend fun getQuoteById(id: Long): Quote?

    fun observeFavorites(): Flow<List<Quote>>

    fun observeRecentlyAdded(limit: Int = 10): Flow<List<Quote>>

    fun observeRecentlyViewed(limit: Int = 10): Flow<List<Quote>>

    fun observeTotalCount(): Flow<Int>

    fun observeFavoriteCount(): Flow<Int>

    fun observeDistinctSourceCount(): Flow<Int>

    suspend fun addQuote(quote: Quote): Long

    suspend fun updateQuote(quote: Quote)

    suspend fun deleteQuote(quote: Quote)

    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    suspend fun markViewed(id: Long)

    suspend fun getAllQuotesOnce(): List<Quote>

    suspend fun clearAllQuotes()

    suspend fun restoreQuotes(quotes: List<Quote>)
}
