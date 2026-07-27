package com.btween.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.btween.app.domain.model.Quote

fun buildShareText(quote: Quote): String = buildString {
    append("\u201C${quote.text}\u201D")
    append("\n\u2014 ${quote.speaker}")
    if (quote.sourceTitle.isNotBlank()) append(", ${quote.sourceTitle}")
}

fun copyQuoteToClipboard(context: Context, quote: Quote) {
    val clipboardManager = ContextCompat.getSystemService(context, ClipboardManager::class.java)
    val clip = ClipData.newPlainText("Quote", buildShareText(quote))
    clipboardManager?.setPrimaryClip(clip)
}

fun shareQuoteAsText(context: Context, quote: Quote) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, buildShareText(quote))
    }
    context.startActivity(Intent.createChooser(intent, "Share quote"))
}
