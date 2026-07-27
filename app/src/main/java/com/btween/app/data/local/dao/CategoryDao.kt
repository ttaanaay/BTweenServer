package com.btween.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.btween.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query(
        """
        SELECT categories.*, (
            SELECT COUNT(*) FROM quotes WHERE quotes.categoryId = categories.id
        ) AS quoteCount
        FROM categories
        ORDER BY name ASC
        """
    )
    fun observeCategoriesWithCounts(): Flow<List<CategoryWithCount>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :name AND id != :excludeId)")
    suspend fun existsByName(name: String, excludeId: Long = -1L): Boolean
}

data class CategoryWithCount(
    val id: Long,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val quoteCount: Int
)
