package com.btween.server.data.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object LoginEvents : Table("login_events") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val occurredAt = timestamp("occurred_at")

    override val primaryKey = PrimaryKey(id)
}
