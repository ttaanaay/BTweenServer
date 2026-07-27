package com.btween.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btween.app.domain.model.Quote
import com.btween.app.domain.model.QuoteFilter
import com.btween.app.domain.model.SortOrder
import com.btween.app.domain.usecase.quote.GetFilteredQuotesUseCase
import com.btween.app.domain.usecase.quote.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val quotes: List<Quote> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getFilteredQuotesUseCase: GetFilteredQuotesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> =
        getFilteredQuotesUseCase(QuoteFilter(favoritesOnly = true, sortOrder = SortOrder.NEWEST))
            .map { quotes -> FavoritesUiState(quotes = quotes, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FavoritesUiState()
            )

    fun onToggleFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch { toggleFavoriteUseCase(id, isFavorite) }
    }
}
