package com.btween.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.btween.app.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(quote: QuoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteEntity>)

    @Update
    suspend fun update(quote: QuoteEntity)

    @Delete
    suspend fun delete(quote: QuoteEntity)

    @Query("DELETE FROM quotes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM quotes")
    suspend fun deleteAll()

    @Query("SELECT * FROM quotes WHERE id = :id")
    fun observeQuoteById(id: Long): Flow<QuoteEntity?>

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getQuoteById(id: Long): QuoteEntity?

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    fun observeAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    suspend fun getAllQuotesOnce(): List<QuoteEntity>

    /**
     * The main Library query. All filter params are nullable/blank-safe "pass-through"
     * conditions - passing null/blank/false for a given filter disables it.
     *
     * sortOrder: 0 = Newest, 1 = Oldest, 2 = Alphabetical, 3 = Favorite first
     */
    @Query(
        """
        SELECT * FROM quotes
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:sourceType IS NULL OR sourceType = :sourceType)
        AND (:favoritesOnly = 0 OR isFavorite = 1)
        AND (
            :searchQuery = '' 
            OR text LIKE '%' || :searchQuery || '%'
            OR sourceTitle LIKE '%' || :searchQuery || '%'
            OR speaker LIKE '%' || :searchQuery || '%'
            OR author LIKE '%' || :searchQuery || '%'
            OR tags LIKE '%' || :searchQuery || '%'
            OR categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :searchQuery || '%')
        )
        ORDER BY
            CASE WHEN :sortOrder = 0 THEN createdAt END DESC,
            CASE WHEN :sortOrder = 1 THEN createdAt END ASC,
            CASE WHEN :sortOrder = 2 THEN text END ASC,
            CASE WHEN :sortOrder = 3 THEN isFavorite END DESC,
            createdAt DESC
        """
    )
    fun observeFilteredQuotes(
        categoryId: Long?,
        sourceType: String?,
        favoritesOnly: Boolean,
        searchQuery: String,
        sortOrder: Int
    ): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 10): Flow<List<QuoteEntity>>

    @Query(
        "SELECT * FROM quotes WHERE lastViewedAt IS NOT NULL ORDER BY lastViewedAt DESC LIMIT :limit"
    )
    fun observeRecentlyViewed(limit: Int = 10): Flow<List<QuoteEntity>>

    @Query("UPDATE quotes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE quotes SET lastViewedAt = :viewedAt WHERE id = :id")
    suspend fun markViewed(id: Long, viewedAt: Long)

    @Query("UPDATE quotes SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromQuotes(categoryId: Long)

    @Query("SELECT COUNT(*) FROM quotes")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM quotes WHERE isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT sourceTitle) FROM quotes")
    fun observeDistinctSourceCount(): Flow<Int>

    @Query(
        """
        SELECT sourceType, COUNT(*) as count FROM quotes
        GROUP BY sourceType ORDER BY count DESC
        """
    )
    fun observeSourceTypeBreakdown(): Flow<List<SourceTypeCount>>
}

data class SourceTypeCount(
    val sourceType: String,
    val count: Int
)
