package com.btween.server.data.repository

import com.btween.server.data.tables.EmailVerifications
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

class EmailVerificationRepository {

    fun createCode(userId: Long): String = transaction {
        val code = (100000..999999).random().toString()
        EmailVerifications.insert {
            it[EmailVerifications.userId] = userId
            it[EmailVerifications.code] = code
            it[EmailVerifications.expiresAt] = Instant.now().plus(15, ChronoUnit.MINUTES)
            it[EmailVerifications.createdAt] = Instant.now()
        }
        code
    }

    /** Marks the code used and returns true if it was valid, unused, and not expired. */
    fun verifyAndConsumeCode(userId: Long, code: String): Boolean = transaction {
        val row = EmailVerifications.selectAll()
            .where {
                (EmailVerifications.userId eq userId) and
                    (EmailVerifications.code eq code) and
                    (EmailVerifications.used eq false)
            }
            .orderBy(EmailVerifications.createdAt, SortOrder.DESC)
            .firstOrNull() ?: return@transaction false

        if (row[EmailVerifications.expiresAt].isBefore(Instant.now())) return@transaction false

        EmailVerifications.update({ EmailVerifications.id eq row[EmailVerifications.id] }) {
            it[EmailVerifications.used] = true
        }
        true
    }
}
