package com.btween.app.util

import androidx.compose.ui.graphics.Color

/**
 * Parses a "#RRGGBB" hex string (as stored on [com.btween.app.domain.model.Category]) into
 * a Compose [Color]. Falls back to a neutral gray if the string is malformed.
 */
fun String.toColorOrDefault(): Color = runCatching {
    Color(android.graphics.Color.parseColor(this))
}.getOrDefault(Color(0xFF9E9E9E))
