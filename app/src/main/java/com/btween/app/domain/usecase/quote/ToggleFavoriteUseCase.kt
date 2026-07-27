package com.btween.app.domain.usecase.quote

import com.btween.app.domain.repository.QuoteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(id: Long, isFavorite: Boolean) =
        quoteRepository.setFavorite(id, isFavorite)
}
