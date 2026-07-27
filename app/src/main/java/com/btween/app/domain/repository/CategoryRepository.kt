package com.btween.app.domain.repository

import com.btween.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>

    suspend fun getCategories(): List<Category>

    suspend fun getCategoryById(id: Long): Category?

    suspend fun addCategory(name: String, colorHex: String): Long

    suspend fun updateCategory(category: Category)

    suspend fun deleteCategory(category: Category)

    suspend fun isNameTaken(name: String, excludeId: Long = -1L): Boolean

    suspend fun restoreCategories(categories: List<Category>)
}
