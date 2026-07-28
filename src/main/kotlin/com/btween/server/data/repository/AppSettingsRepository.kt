package com.btween.server.data.repository

import com.btween.server.data.tables.AppSettings
import com.btween.server.domain.AppSettingsData
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AppSettingsRepository {

    fun get(): AppSettingsData = transaction {
        AppSettings.selectAll()
            .map { AppSettingsData(defaultAutoApprove = it[AppSettings.defaultAutoApprove]) }
            .single()
    }

    fun setDefaultAutoApprove(value: Boolean): AppSettingsData = transaction {
        AppSettings.update({ AppSettings.id eq 1 }) {
            it[AppSettings.defaultAutoApprove] = value
        }
        get()
    }
}
