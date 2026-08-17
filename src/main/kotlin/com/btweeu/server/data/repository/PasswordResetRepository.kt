package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.PasswordResets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

class PasswordResetRepository {

    fun createCode(userId: Long): String = transaction {
        val code = (100000..999999).random().toString()
        PasswordResets.insert {
            it[PasswordResets.userId] = userId
            it[PasswordResets.code] = code
            it[PasswordResets.expiresAt] = Instant.now().plus(15, ChronoUnit.MINUTES)
            it[PasswordResets.createdAt] = Instant.now()
        }
        code
    }

    /** Marks the code used and returns true if it was valid, unused, and not expired. */
    fun verifyAndConsumeCode(userId: Long, code: String): Boolean = transaction {
        val row = PasswordResets.selectAll()
            .where {
                (PasswordResets.userId eq userId) and
                    (PasswordResets.code eq code) and
                    (PasswordResets.used eq false)
            }
            .orderBy(PasswordResets.createdAt, SortOrder.DESC)
            .firstOrNull() ?: return@transaction false

        if (row[PasswordResets.expiresAt].isBefore(Instant.now())) return@transaction false

        PasswordResets.update({ PasswordResets.id eq row[PasswordResets.id] }) {
            it[PasswordResets.used] = true
        }
        true
    }
}
