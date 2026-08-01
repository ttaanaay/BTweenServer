package com.btween.server.data.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 30).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    // Null for accounts created via Google/Facebook/Microsoft sign-in - they never set a
    // local password, so there's nothing to hash.
    val passwordHash = varchar("password_hash", 255).nullable()
    val displayName = varchar("display_name", 60)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val bio = varchar("bio", 280).nullable()
    val isAdmin = bool("is_admin").default(false)
    val isBanned = bool("is_banned").default(false)
    // OAuth providers already verify email ownership themselves, so accounts created via
    // Google/Facebook/Microsoft start out true; local email+password accounts start false
    // and flip to true once the user enters their emailed verification code.
    val emailVerified = bool("email_verified").default(false)
    // Brute-force protection: incremented on each failed login, reset to 0 on success.
    // Once it hits the threshold (see AuthRoutes.kt), lockedUntil is set and login is
    // refused - even with correct credentials - until that time passes.
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lockedUntil = timestamp("locked_until").nullable()
    // null = "use the global default setting"; true/false = explicit per-user override set by an admin.
    val autoApprove = bool("auto_approve").nullable()
    // "GOOGLE" / "FACEBOOK" / "MICROSOFT", or null for a local email+password account.
    val authProvider = varchar("auth_provider", 20).nullable()
    val providerUserId = varchar("provider_user_id", 255).nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_auth_provider_identity", isUnique = true, authProvider, providerUserId)
    }
}
