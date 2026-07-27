package com.btween.app.domain.usecase.quote

import com.btween.app.domain.model.Quote
import com.btween.app.domain.repository.QuoteRepository
import javax.inject.Inject

class AddQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(quote: Quote): Result<Long> {
        val validationError = quote.validate()
        if (validationError != null) return Result.failure(IllegalArgumentException(validationError))
        return runCatching { quoteRepository.addQuote(quote) }
    }
}

/**
 * Returns a human-readable validation message, or null if the quote is valid.
 * Shared by [AddQuoteUseCase] and [UpdateQuoteUseCase] so both enforce the same rules.
 */
internal fun Quote.validate(): String? = when {
    text.isBlank() -> "Quote text can't be empty"
    sourceTitle.isBlank() -> "Source title can't be empty"
    speaker.isBlank() -> "Speaker / character can't be empty"
    else -> null
}
