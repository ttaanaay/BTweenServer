package com.btween.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object SourceTypes : Table("source_types") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 60).uniqueIndex()
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
