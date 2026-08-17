package com.btweeu.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object EmailVerifications : Table("email_verifications") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val code = varchar("code", 10)
    val expiresAt = timestamp("expires_at")
    val used = bool("used").default(false)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
