package com.btween.server.config

import com.btween.server.data.tables.AppSettings
import com.btween.server.data.tables.Comments
import com.btween.server.data.tables.Follows
import com.btween.server.data.tables.Likes
import com.btween.server.data.tables.Notifications
import com.btween.server.data.tables.PasswordResets
import com.btween.server.data.tables.Quotes
import com.btween.server.data.tables.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object DatabaseFactory {

    fun init(config: AppConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.dbUser
            password = config.dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Users, Quotes, Follows, Likes, AppSettings, Notifications, PasswordResets, Comments)

            if (AppSettings.selectAll().count() == 0L) {
                AppSettings.insert {
                    it[AppSettings.id] = 1
                    it[AppSettings.defaultAutoApprove] = false
                    it[AppSettings.legacyQuotesMigrated] = false
                }
            }

            // One-time backfill: quotes created before this feature existed default to
            // PENDING (the column's SQL default) purely because Postgres has to put
            // *something* in the new column. Without this, every quote anyone already
            // posted would instantly vanish from the feed the moment this migration runs.
            // Guarded by a flag so it only ever executes once, even across many redeploys.
            val settingsRow = AppSettings.selectAll().single()
            if (!settingsRow[AppSettings.legacyQuotesMigrated]) {
                Quotes.update({ Quotes.status eq "PENDING" }) { it[Quotes.status] = "APPROVED" }
                AppSettings.update({ AppSettings.id eq 1 }) { it[AppSettings.legacyQuotesMigrated] = true }
            }
        }
    }
}
