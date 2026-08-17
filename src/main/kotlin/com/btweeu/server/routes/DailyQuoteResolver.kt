package com.btweeu.server.routes

import com.btweeu.server.data.repository.AppSettingsRepository
import com.btweeu.server.data.repository.QuoteRepository
import com.btweeu.server.domain.Quote
import java.time.LocalDate

/**
 * Returns the quote already picked for today if one was picked, otherwise picks a fresh
 * random one and remembers it - so the first caller each day (whichever of the Home screen
 * or the push-notification cron happens to ask first) decides it for everyone else that day.
 */
fun resolveDailyQuote(quoteRepository: QuoteRepository, appSettingsRepository: AppSettingsRepository): Quote? {
    val today = LocalDate.now()
    val existing = appSettingsRepository.getDailyQuote()

    if (existing != null && existing.date == today) {
        val quote = quoteRepository.findById(existing.quoteId)
        // Fall through to pick a new one only if that quote no longer exists/qualifies
        // (e.g. deleted or unpublished since being picked) - otherwise keep today's pick.
        if (quote != null && quote.visibility == "PUBLIC" && quote.status == "APPROVED") {
            return quote
        }
    }

    val fresh = quoteRepository.getRandomPublicQuote() ?: return null
    appSettingsRepository.setDailyQuote(fresh.id, today)
    return fresh
}
