package com.btween.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btween.app.R
import com.btween.app.domain.model.Quote
import com.btween.app.ui.components.EmptyState
import com.btween.app.ui.components.QuoteListCard
import com.btween.app.ui.components.SectionHeader
import com.btween.app.ui.components.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddQuote: () -> Unit,
    onSearch: () -> Unit,
    onQuoteClick: (Long) -> Unit,
    onSeeAllFavorites: () -> Unit,
    onSeeAllLibrary: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddQuote,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_quote)) }
            )
        }
    ) { padding ->
        if (uiState.isEmpty) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                icon = Icons.Outlined.AutoStories,
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                actionLabel = stringResource(R.string.home_empty_action),
                onAction = onAddQuote
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                StatsRow(
                    totalQuotes = uiState.homeData.totalQuotes,
                    totalFavorites = uiState.homeData.totalFavorites,
                    totalSources = uiState.homeData.totalSources
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (uiState.homeData.recentlyAdded.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.home_section_recently_added),
                        actionLabel = stringResource(R.string.action_see_all),
                        onActionClick = onSeeAllLibrary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    QuoteRow(
                        quotes = uiState.homeData.recentlyAdded,
                        onQuoteClick = onQuoteClick,
                        onToggleFavorite = viewModel::onToggleFavorite
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (uiState.homeData.favorites.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.home_section_favorites),
                        actionLabel = stringResource(R.string.action_see_all),
                        onActionClick = onSeeAllFavorites
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    QuoteRow(
                        quotes = uiState.homeData.favorites,
                        onQuoteClick = onQuoteClick,
                        onToggleFavorite = viewModel::onToggleFavorite
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (uiState.homeData.recentlyViewed.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.home_section_recently_viewed))
                    Spacer(modifier = Modifier.height(8.dp))
                    QuoteRow(
                        quotes = uiState.homeData.recentlyViewed,
                        onQuoteClick = onQuoteClick,
                        onToggleFavorite = viewModel::onToggleFavorite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun StatsRow(totalQuotes: Int, totalFavorites: Int, totalSources: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(value = totalQuotes.toString(), label = stringResource(R.string.home_stat_quotes))
        StatCard(value = totalFavorites.toString(), label = stringResource(R.string.home_stat_favorites))
        StatCard(value = totalSources.toString(), label = stringResource(R.string.home_stat_sources))
    }
}

@Composable
private fun QuoteRow(
    quotes: List<Quote>,
    onQuoteClick: (Long) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quotes, key = { it.id }) { quote ->
            QuoteListCard(
                quote = quote,
                onClick = { onQuoteClick(quote.id) },
                onToggleFavorite = { onToggleFavorite(quote.id, !quote.isFavorite) },
                modifier = Modifier.width(280.dp)
            )
        }
    }
}
