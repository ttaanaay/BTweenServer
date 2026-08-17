package com.btweeu.server.data.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Reports : Table("reports") {
    val id = long("id").autoIncrement()
    val reporterId = long("reporter_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    // "QUOTE" or "USER"
    val targetType = varchar("target_type", 10)
    val targetId = long("target_id")
    val reason = varchar("reason", 40)
    val details = varchar("details", 500).nullable()
    // "PENDING" -> awaiting admin review, "RESOLVED" -> admin took action, "DISMISSED" -> no action needed.
    val status = varchar("status", 15).default("PENDING")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
