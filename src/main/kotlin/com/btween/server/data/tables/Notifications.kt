package com.btween.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Notifications : Table("notifications") {
    val id = long("id").autoIncrement()
    val recipientUserId = long("recipient_user_id")
    val actorUserId = long("actor_user_id")
    // "FOLLOW" or "LIKE"
    val type = varchar("type", 20)
    // Only set for LIKE notifications - which quote was liked.
    val quoteId = long("quote_id").nullable()
    val isRead = bool("is_read").default(false)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
