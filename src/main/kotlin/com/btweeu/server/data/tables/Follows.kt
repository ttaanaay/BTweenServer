package com.btweeu.server.data.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp

object Follows : Table("follows") {
    val followerId = long("follower_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val followingId = long("following_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(followerId, followingId)
}
