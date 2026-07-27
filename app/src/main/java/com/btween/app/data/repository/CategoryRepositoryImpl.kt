package com.btween.app.data.repository

import com.btween.app.data.local.dao.CategoryDao
import com.btween.app.data.local.mapper.toDomain
import com.btween.app.data.local.mapper.toEntity
import com.btween.app.domain.model.Category
import com.btween.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategoriesWithCounts().map { list -> list.map { it.toDomain() } }

    override suspend fun getCategories(): List<Category> =
        categoryDao.getCategories().map { it.toDomain() }

    override suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    override suspend fun addCategory(name: String, colorHex: String): Long =
        categoryDao.insert(Category(name = name, colorHex = colorHex).toEntity())

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity())
    }

    override suspend fun isNameTaken(name: String, excludeId: Long): Boolean =
        categoryDao.existsByName(name.trim(), excludeId)

    override suspend fun restoreCategories(categories: List<Category>) {
        categoryDao.insertAll(categories.map { it.toEntity() })
    }
}
