package com.btween.app.domain.usecase.quote

import com.btween.app.domain.model.Quote
import com.btween.app.domain.repository.QuoteRepository
import javax.inject.Inject

class UpdateQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(quote: Quote): Result<Unit> {
        val validationError = quote.validate()
        if (validationError != null) return Result.failure(IllegalArgumentException(validationError))
        return runCatching { quoteRepository.updateQuote(quote) }
    }
}
