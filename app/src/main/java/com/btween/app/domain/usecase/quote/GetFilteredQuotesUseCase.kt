package com.btween.app.domain.usecase.quote

import com.btween.app.domain.model.Quote
import com.btween.app.domain.model.QuoteFilter
import com.btween.app.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFilteredQuotesUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    operator fun invoke(filter: QuoteFilter): Flow<List<Quote>> =
        quoteRepository.observeFilteredQuotes(filter)
}
