package com.btween.server.data.repository

import com.btween.server.data.tables.Follows
import com.btween.server.data.tables.Users
import com.btween.server.domain.User
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class UserRepository {

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        username = this[Users.username],
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        displayName = this[Users.displayName],
        avatarUrl = this[Users.avatarUrl],
        bio = this[Users.bio],
        createdAt = this[Users.createdAt]
    )

    fun create(username: String, email: String, passwordHash: String, displayName: String): User = transaction {
        val id = Users.insert {
            it[Users.username] = username
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.displayName] = displayName
            it[Users.createdAt] = Instant.now()
        } get Users.id
        findById(id)!!
    }

    fun findById(id: Long): User? = transaction {
        Users.selectAll().where { Users.id eq id }.map { it.toUser() }.singleOrNull()
    }

    fun findByEmail(email: String): User? = transaction {
        Users.selectAll().where { Users.email eq email }.map { it.toUser() }.singleOrNull()
    }

    fun findByUsername(username: String): User? = transaction {
        Users.selectAll().where { Users.username eq username }.map { it.toUser() }.singleOrNull()
    }

    fun usernameTaken(username: String): Boolean = transaction {
        Users.selectAll().where { Users.username eq username }.count() > 0
    }

    fun emailTaken(email: String): Boolean = transaction {
        Users.selectAll().where { Users.email eq email }.count() > 0
    }

    fun updateProfile(id: Long, displayName: String?, avatarUrl: String?, bio: String?): User? = transaction {
        Users.update({ Users.id eq id }) { statement ->
            displayName?.let { statement[Users.displayName] = it }
            avatarUrl?.let { statement[Users.avatarUrl] = it }
            bio?.let { statement[Users.bio] = it }
        }
        findById(id)
    }

    fun follow(followerId: Long, followingId: Long) = transaction {
        val alreadyFollowing = Follows.selectAll()
            .where { (Follows.followerId eq followerId) and (Follows.followingId eq followingId) }
            .count() > 0
        if (!alreadyFollowing && followerId != followingId) {
            Follows.insert {
                it[Follows.followerId] = followerId
                it[Follows.followingId] = followingId
                it[Follows.createdAt] = Instant.now()
            }
        }
    }

    fun unfollow(followerId: Long, followingId: Long) = transaction {
        Follows.deleteWhere { (Follows.followerId eq followerId) and (Follows.followingId eq followingId) }
    }

    fun isFollowing(followerId: Long, followingId: Long): Boolean = transaction {
        Follows.selectAll()
            .where { (Follows.followerId eq followerId) and (Follows.followingId eq followingId) }
            .count() > 0
    }

    fun followerCount(userId: Long): Int = transaction {
        Follows.selectAll().where { Follows.followingId eq userId }.count().toInt()
    }

    fun followingCount(userId: Long): Int = transaction {
        Follows.selectAll().where { Follows.followerId eq userId }.count().toInt()
    }
}
