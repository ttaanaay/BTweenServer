package com.btween.app.domain.usecase.quote

import com.btween.app.domain.repository.QuoteRepository
import javax.inject.Inject

class MarkQuoteViewedUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(id: Long) = quoteRepository.markViewed(id)
}
