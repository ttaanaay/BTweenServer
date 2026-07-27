package com.btween.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.btween.app.R
import com.btween.app.domain.model.SortOrder
import com.btween.app.domain.model.SourceType

/**
 * Localized display label for a [SourceType]. Kept in the UI layer (rather than as a
 * property on the domain enum itself) so the domain model has no Android/resource
 * dependency, while the label still updates immediately when the app's language changes.
 */
@Composable
fun SourceType.localizedLabel(): String = when (this) {
    SourceType.MOVIE -> stringResource(R.string.source_type_movie)
    SourceType.TV_SERIES -> stringResource(R.string.source_type_tv_series)
    SourceType.BOOK -> stringResource(R.string.source_type_book)
    SourceType.ANIME -> stringResource(R.string.source_type_anime)
    SourceType.GAME -> stringResource(R.string.source_type_game)
    SourceType.PODCAST -> stringResource(R.string.source_type_podcast)
    SourceType.SPEECH -> stringResource(R.string.source_type_speech)
    SourceType.OTHER -> stringResource(R.string.source_type_other)
}

@Composable
fun SortOrder.localizedLabel(): String = when (this) {
    SortOrder.NEWEST -> stringResource(R.string.sort_newest)
    SortOrder.OLDEST -> stringResource(R.string.sort_oldest)
    SortOrder.ALPHABETICAL -> stringResource(R.string.sort_alphabetical)
    SortOrder.FAVORITE -> stringResource(R.string.sort_favorite)
}
