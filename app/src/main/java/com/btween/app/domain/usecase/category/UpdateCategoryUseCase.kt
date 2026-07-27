package com.btween.app.domain.usecase.category

import com.btween.app.domain.model.Category
import com.btween.app.domain.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        val trimmed = category.name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Category name can't be empty"))
        if (categoryRepository.isNameTaken(trimmed, excludeId = category.id)) {
            return Result.failure(IllegalArgumentException("A category named \"$trimmed\" already exists"))
        }
        return runCatching { categoryRepository.updateCategory(category.copy(name = trimmed)) }
    }
}
