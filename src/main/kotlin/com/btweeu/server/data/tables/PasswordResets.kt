package com.btweeu.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PasswordResets : Table("password_resets") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    // 6-digit numeric code, not a long opaque token - simple to type/read from a log or
    // (once real email is wired up) an email body.
    val code = varchar("code", 10)
    val expiresAt = timestamp("expires_at")
    val used = bool("used").default(false)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
