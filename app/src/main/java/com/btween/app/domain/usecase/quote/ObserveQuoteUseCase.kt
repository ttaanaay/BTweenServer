package com.btween.app.domain.usecase.quote

import com.btween.app.domain.model.Quote
import com.btween.app.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    operator fun invoke(id: Long): Flow<Quote?> = quoteRepository.observeQuoteById(id)
}
