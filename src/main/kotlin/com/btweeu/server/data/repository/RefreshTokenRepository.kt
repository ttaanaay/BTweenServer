package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.RefreshTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.Instant

enum class RefreshTokenStatus { VALID, EXPIRED, REVOKED, UNKNOWN }

class RefreshTokenRepository {

    private fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun store(userId: Long, rawToken: String, expiresAt: Instant) = transaction {
        RefreshTokens.insert {
            it[RefreshTokens.userId] = userId
            it[tokenHash] = hash(rawToken)
            it[RefreshTokens.expiresAt] = expiresAt
            it[createdAt] = Instant.now()
        }
    }

    fun checkStatus(rawToken: String): RefreshTokenStatus = transaction {
        val row = RefreshTokens.selectAll().where { RefreshTokens.tokenHash eq hash(rawToken) }.singleOrNull()
            ?: return@transaction RefreshTokenStatus.UNKNOWN
        when {
            row[RefreshTokens.revokedAt] != null -> RefreshTokenStatus.REVOKED
            row[RefreshTokens.expiresAt].isBefore(Instant.now()) -> RefreshTokenStatus.EXPIRED
            else -> RefreshTokenStatus.VALID
        }
    }

    fun revoke(rawToken: String) = transaction {
        RefreshTokens.update({ RefreshTokens.tokenHash eq hash(rawToken) }) {
            it[revokedAt] = Instant.now()
        }
    }

    /** Revokes every active session for this user - used as a defensive response when a
     * refresh token that's already been rotated away gets presented again, since that's a
     * strong signal the token was stolen and used by someone other than its rightful owner. */
    fun revokeAllForUser(userId: Long) = transaction {
        RefreshTokens.update({
            with(SqlExpressionBuilder) { (RefreshTokens.userId eq userId) and (RefreshTokens.revokedAt.isNull()) }
        }) {
            it[revokedAt] = Instant.now()
        }
    }
}
