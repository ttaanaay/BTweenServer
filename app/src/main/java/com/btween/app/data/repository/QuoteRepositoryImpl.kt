package com.btween.app.data.repository

import com.btween.app.data.local.dao.CategoryDao
import com.btween.app.data.local.dao.QuoteDao
import com.btween.app.data.local.mapper.toDomain
import com.btween.app.data.local.mapper.toEntity
import com.btween.app.domain.model.Category
import com.btween.app.domain.model.Quote
import com.btween.app.domain.model.QuoteFilter
import com.btween.app.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao,
    private val categoryDao: CategoryDao
) : QuoteRepository {

    private fun categoryMapFlow(): Flow<Map<Long, Category>> =
        categoryDao.observeCategories().map { entities ->
            entities.associate { it.id to it.toDomain() }
        }

    override fun observeFilteredQuotes(filter: QuoteFilter): Flow<List<Quote>> =
        combine(
            quoteDao.observeFilteredQuotes(
                categoryId = filter.categoryId,
                sourceType = filter.sourceType?.name,
                favoritesOnly = filter.favoritesOnly,
                searchQuery = filter.searchQuery.trim(),
                sortOrder = filter.sortOrder.ordinal
            ),
            categoryMapFlow()
        ) { entities, categories ->
            entities.map { it.toDomain(it.categoryId?.let(categories::get)) }
        }

    override fun observeQuoteById(id: Long): Flow<Quote?> =
        combine(quoteDao.observeQuoteById(id), categoryMapFlow()) { entity, categories ->
            entity?.let { it.toDomain(it.categoryId?.let(categories::get)) }
        }

    override suspend fun getQuoteById(id: Long): Quote? {
        val entity = quoteDao.getQuoteById(id) ?: return null
        val categories = categoryMapFlow().first()
        return entity.toDomain(entity.categoryId?.let(categories::get))
    }

    override fun observeFavorites(): Flow<List<Quote>> =
        combine(quoteDao.observeFavorites(), categoryMapFlow()) { entities, categories ->
            entities.map { it.toDomain(it.categoryId?.let(categories::get)) }
        }

    override fun observeRecentlyAdded(limit: Int): Flow<List<Quote>> =
        combine(quoteDao.observeRecentlyAdded(limit), categoryMapFlow()) { entities, categories ->
            entities.map { it.toDomain(it.categoryId?.let(categories::get)) }
        }

    override fun observeRecentlyViewed(limit: Int): Flow<List<Quote>> =
        combine(quoteDao.observeRecentlyViewed(limit), categoryMapFlow()) { entities, categories ->
            entities.map { it.toDomain(it.categoryId?.let(categories::get)) }
        }

    override fun observeTotalCount(): Flow<Int> = quoteDao.observeTotalCount()

    override fun observeFavoriteCount(): Flow<Int> = quoteDao.observeFavoriteCount()

    override fun observeDistinctSourceCount(): Flow<Int> = quoteDao.observeDistinctSourceCount()

    override suspend fun addQuote(quote: Quote): Long {
        val now = System.currentTimeMillis()
        return quoteDao.insert(quote.copy(createdAt = now, updatedAt = now).toEntity())
    }

    override suspend fun updateQuote(quote: Quote) {
        quoteDao.update(quote.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun deleteQuote(quote: Quote) {
        quoteDao.delete(quote.toEntity())
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        quoteDao.setFavorite(id, isFavorite, System.currentTimeMillis())
    }

    override suspend fun markViewed(id: Long) {
        quoteDao.markViewed(id, System.currentTimeMillis())
    }

    override suspend fun getAllQuotesOnce(): List<Quote> {
        val categories = categoryMapFlow().first()
        return quoteDao.getAllQuotesOnce().map { it.toDomain(it.categoryId?.let(categories::get)) }
    }

    override suspend fun clearAllQuotes() {
        quoteDao.deleteAll()
    }

    override suspend fun restoreQuotes(quotes: List<Quote>) {
        quoteDao.insertAll(quotes.map { it.toEntity() })
    }
}
