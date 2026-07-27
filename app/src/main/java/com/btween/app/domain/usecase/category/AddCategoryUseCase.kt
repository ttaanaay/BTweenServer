package com.btween.app.domain.usecase.category

import com.btween.app.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String, colorHex: String): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Category name can't be empty"))
        if (categoryRepository.isNameTaken(trimmed)) {
            return Result.failure(IllegalArgumentException("A category named \"$trimmed\" already exists"))
        }
        return runCatching { categoryRepository.addCategory(trimmed, colorHex) }
    }
}
