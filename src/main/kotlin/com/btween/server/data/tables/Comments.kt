package com.btween.server.data.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Comments : Table("comments") {
    val id = long("id").autoIncrement()
    val quoteId = long("quote_id").references(Quotes.id, onDelete = ReferenceOption.CASCADE)
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val text = varchar("text", 1000)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
