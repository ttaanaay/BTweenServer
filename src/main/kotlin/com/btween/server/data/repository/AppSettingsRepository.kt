package com.btween.server.data.repository

import com.btween.server.data.tables.AppSettings
import com.btween.server.domain.AppSettingsData
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

data class DailyQuoteRecord(val quoteId: Long, val date: LocalDate)

class AppSettingsRepository {

    fun get(): AppSettingsData = transaction {
        AppSettings.selectAll()
            .map {
                AppSettingsData(
                    defaultAutoApprove = it[AppSettings.defaultAutoApprove],
                    maintenanceMode = it[AppSettings.maintenanceMode],
                    maintenanceMessage = it[AppSettings.maintenanceMessage]
                )
            }
            .single()
    }

    fun setDefaultAutoApprove(value: Boolean): AppSettingsData = transaction {
        AppSettings.update({ AppSettings.id eq 1 }) {
            it[AppSettings.defaultAutoApprove] = value
        }
        get()
    }

    fun setMaintenanceMode(enabled: Boolean, message: String?): AppSettingsData = transaction {
        AppSettings.update({ AppSettings.id eq 1 }) {
            it[AppSettings.maintenanceMode] = enabled
            it[AppSettings.maintenanceMessage] = message
        }
        get()
    }

    /** Null if no quote has ever been picked yet (e.g. cron hasn't run once since launch). */
    fun getDailyQuote(): DailyQuoteRecord? = transaction {
        AppSettings.selectAll().where { AppSettings.id eq 1 }
            .map { row ->
                val quoteId = row[AppSettings.dailyQuoteId]
                val date = row[AppSettings.dailyQuoteDate]
                if (quoteId != null && date != null) DailyQuoteRecord(quoteId, date) else null
            }
            .singleOrNull()
    }

    fun setDailyQuote(quoteId: Long, date: LocalDate) = transaction {
        AppSettings.update({ AppSettings.id eq 1 }) {
            it[AppSettings.dailyQuoteId] = quoteId
            it[AppSettings.dailyQuoteDate] = date
        }
    }
}
