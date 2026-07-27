package com.btween.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btween.app.domain.model.Quote
import com.btween.app.domain.model.QuoteFilter
import com.btween.app.domain.usecase.quote.GetFilteredQuotesUseCase
import com.btween.app.domain.usecase.quote.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Quote> = emptyList(),
    val hasSearched: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getFilteredQuotesUseCase: GetFilteredQuotesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val queryState = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = combine(
        queryState,
        queryState.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                getFilteredQuotesUseCase(QuoteFilter(searchQuery = query))
            }
        }
    ) { query, results ->
        SearchUiState(query = query, results = results, hasSearched = query.isNotBlank())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    fun onQueryChanged(query: String) {
        queryState.value = query
    }

    fun onToggleFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch { toggleFavoriteUseCase(id, isFavorite) }
    }
}
