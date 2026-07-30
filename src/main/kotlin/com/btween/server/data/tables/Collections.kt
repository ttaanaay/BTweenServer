package com.btween.server.data.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Collections : Table("collections") {
    val id = long("id").autoIncrement()
    val ownerId = long("owner_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object CollectionItems : Table("collection_items") {
    val id = long("id").autoIncrement()
    val collectionId = long("collection_id").references(Collections.id, onDelete = ReferenceOption.CASCADE)
    val quoteId = long("quote_id").references(Quotes.id, onDelete = ReferenceOption.CASCADE)
    val addedAt = timestamp("added_at")

    override val primaryKey = PrimaryKey(id)
}
