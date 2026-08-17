package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.DeviceTokens
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class DeviceTokenRepository {

    /** Upserts by token: if this exact token is already registered (possibly to a different
     * user, e.g. after logout/login as someone else on the same device), it's just re-pointed
     * at the current user rather than creating a duplicate row. */
    fun register(userId: Long, fcmToken: String) = transaction {
        val existing = DeviceTokens.selectAll().where { DeviceTokens.fcmToken eq fcmToken }.singleOrNull()
        if (existing != null) {
            DeviceTokens.update({ DeviceTokens.fcmToken eq fcmToken }) {
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.updatedAt] = Instant.now()
            }
        } else {
            DeviceTokens.insert {
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.fcmToken] = fcmToken
                it[DeviceTokens.createdAt] = Instant.now()
                it[DeviceTokens.updatedAt] = Instant.now()
            }
        }
    }

    fun unregister(fcmToken: String) = transaction {
        DeviceTokens.deleteWhere { with(SqlExpressionBuilder) { DeviceTokens.fcmToken eq fcmToken } }
    }

    fun getAllTokens(): List<String> = transaction {
        DeviceTokens.selectAll().map { it[DeviceTokens.fcmToken] }
    }

    /** Removes a token that FCM reported as no-longer-valid (app uninstalled, etc). */
    fun removeInvalidToken(fcmToken: String) = unregister(fcmToken)
}
