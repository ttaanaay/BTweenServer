package com.btween.server.data.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 30).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 60)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val bio = varchar("bio", 280).nullable()
    val isAdmin = bool("is_admin").default(false)
    val isBanned = bool("is_banned").default(false)
    // null = "use the global default setting"; true/false = explicit per-user override set by an admin.
    val autoApprove = bool("auto_approve").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
