package com.btween.app.domain.usecase.quote

import com.btween.app.domain.model.Quote
import com.btween.app.domain.repository.QuoteRepository
import javax.inject.Inject

class DeleteQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(quote: Quote) = quoteRepository.deleteQuote(quote)
}
