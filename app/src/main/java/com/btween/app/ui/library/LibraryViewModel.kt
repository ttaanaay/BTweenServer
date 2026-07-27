package com.btween.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btween.app.domain.model.Category
import com.btween.app.domain.model.Quote
import com.btween.app.domain.model.QuoteFilter
import com.btween.app.domain.model.SortOrder
import com.btween.app.domain.model.SourceType
import com.btween.app.domain.usecase.category.GetCategoriesUseCase
import com.btween.app.domain.usecase.quote.GetFilteredQuotesUseCase
import com.btween.app.domain.usecase.quote.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val filter: QuoteFilter = QuoteFilter(),
    val quotes: List<Quote> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getFilteredQuotesUseCase: GetFilteredQuotesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val filterState = MutableStateFlow(QuoteFilter())

    val uiState: StateFlow<LibraryUiState> = combine(
        filterState.flatMapLatest { getFilteredQuotesUseCase(it) },
        filterState,
        getCategoriesUseCase()
    ) { quotes, filter, categories ->
        LibraryUiState(filter = filter, quotes = quotes, categories = categories, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        filterState.update { it.copy(searchQuery = query) }
    }

    fun onSortOrderSelected(order: SortOrder) {
        filterState.update { it.copy(sortOrder = order) }
    }

    fun onCategorySelected(categoryId: Long?) {
        filterState.update { it.copy(categoryId = if (it.categoryId == categoryId) null else categoryId) }
    }

    fun onSourceTypeSelected(sourceType: SourceType?) {
        filterState.update { it.copy(sourceType = if (it.sourceType == sourceType) null else sourceType) }
    }

    fun onFavoritesOnlyToggled() {
        filterState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    fun onToggleFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch { toggleFavoriteUseCase(id, isFavorite) }
    }

    fun onClearFilters() {
        filterState.update { QuoteFilter(searchQuery = it.searchQuery, sortOrder = it.sortOrder) }
    }
}
