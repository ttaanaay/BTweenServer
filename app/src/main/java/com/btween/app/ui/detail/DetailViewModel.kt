package com.btween.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btween.app.domain.model.Quote
import com.btween.app.domain.usecase.quote.DeleteQuoteUseCase
import com.btween.app.domain.usecase.quote.MarkQuoteViewedUseCase
import com.btween.app.domain.usecase.quote.ObserveQuoteUseCase
import com.btween.app.domain.usecase.quote.ToggleFavoriteUseCase
import com.btween.app.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = true,
    val didDelete: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeQuoteUseCase: ObserveQuoteUseCase,
    private val markQuoteViewedUseCase: MarkQuoteViewedUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteQuoteUseCase: DeleteQuoteUseCase
) : ViewModel() {

    private val quoteId: Long = checkNotNull(savedStateHandle[Destination.QuoteDetail.ARG_QUOTE_ID])

    private val didDelete = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(
        observeQuoteUseCase(quoteId),
        didDelete
    ) { quote, deleted ->
        DetailUiState(quote = quote, isLoading = false, didDelete = deleted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState()
    )

    init {
        viewModelScope.launch { markQuoteViewedUseCase(quoteId) }
    }

    fun onToggleFavorite() {
        val current = uiState.value.quote ?: return
        viewModelScope.launch { toggleFavoriteUseCase(current.id, !current.isFavorite) }
    }

    fun onDelete() {
        val current = uiState.value.quote ?: return
        viewModelScope.launch {
            deleteQuoteUseCase(current)
            didDelete.value = true
        }
    }
}
