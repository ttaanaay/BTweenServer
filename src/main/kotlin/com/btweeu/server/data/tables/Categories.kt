package com.btweeu.server.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Categories : Table("categories") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 60).uniqueIndex()
    // A key from the fixed set defined in IconCatalog (both client apps map these to the
    // same Material Icons the app always used) - not a free-form emoji, so all platforms
    // render an identical, consistent icon set.
    val icon = varchar("icon", 32).default("label")
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
