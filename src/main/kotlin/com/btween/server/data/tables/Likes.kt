package com.btween.server.data.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp

object Likes : Table("likes") {
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val quoteId = long("quote_id").references(Quotes.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(userId, quoteId)
}
