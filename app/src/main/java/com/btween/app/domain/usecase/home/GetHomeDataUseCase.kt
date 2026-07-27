package com.btween.app.domain.usecase.home

import com.btween.app.domain.model.HomeData
import com.btween.app.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

private const val HOME_SECTION_LIMIT = 8

class GetHomeDataUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    operator fun invoke(): Flow<HomeData> {
        val quotesFlow = combine(
            quoteRepository.observeRecentlyAdded(HOME_SECTION_LIMIT),
            quoteRepository.observeFavorites(),
            quoteRepository.observeRecentlyViewed(HOME_SECTION_LIMIT)
        ) { recentlyAdded, favorites, recentlyViewed ->
            Triple(recentlyAdded, favorites.take(HOME_SECTION_LIMIT), recentlyViewed)
        }

        val statsFlow = combine(
            quoteRepository.observeTotalCount(),
            quoteRepository.observeFavoriteCount(),
            quoteRepository.observeDistinctSourceCount()
        ) { total, favoriteCount, sourceCount ->
            Triple(total, favoriteCount, sourceCount)
        }

        return combine(quotesFlow, statsFlow) { quotes, stats ->
            HomeData(
                recentlyAdded = quotes.first,
                favorites = quotes.second,
                recentlyViewed = quotes.third,
                totalQuotes = stats.first,
                totalFavorites = stats.second,
                totalSources = stats.third
            )
        }
    }
}
