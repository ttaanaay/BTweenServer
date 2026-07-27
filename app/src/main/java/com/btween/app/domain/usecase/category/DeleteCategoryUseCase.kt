package com.btween.app.domain.usecase.category

import com.btween.app.domain.model.Category
import com.btween.app.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) = categoryRepository.deleteCategory(category)
}
