package com.btween.app.domain.model

data class HomeData(
    val recentlyAdded: List<Quote> = emptyList(),
    val favorites: List<Quote> = emptyList(),
    val recentlyViewed: List<Quote> = emptyList(),
    val totalQuotes: Int = 0,
    val totalFavorites: Int = 0,
    val totalSources: Int = 0
)
