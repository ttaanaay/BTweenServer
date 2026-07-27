package com.btween.server.data.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp

object Quotes : Table("quotes") {
    val id = long("id").autoIncrement()
    val ownerId = long("owner_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val text = text("text")
    val sourceTitle = varchar("source_title", 200)
    val sourceType = varchar("source_type", 30)
    val speaker = varchar("speaker", 120)
    val author = varchar("author", 120).nullable()
    val category = varchar("category", 60).nullable()
    val tags = varchar("tags", 500).default("")
    val visibility = varchar("visibility", 10).default("PUBLIC")
    val likeCount = integer("like_count").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
