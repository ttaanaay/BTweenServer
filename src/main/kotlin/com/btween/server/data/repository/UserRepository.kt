package com.btween.server.data.repository

import com.btween.server.data.tables.Follows
import com.btween.server.data.tables.Quotes
import com.btween.server.data.tables.Users
import com.btween.server.domain.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
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
        isAdmin = this[Users.isAdmin],
        isBanned = this[Users.isBanned],
        autoApprove = this[Users.autoApprove],
        authProvider = this[Users.authProvider],
        providerUserId = this[Users.providerUserId],
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

    /** Creates an account for a user who signed in via Google/Facebook/Microsoft - no password. */
    fun createOAuthUser(
        provider: String,
        providerUserId: String,
        username: String,
        email: String,
        displayName: String
    ): User = transaction {
        val id = Users.insert {
            it[Users.username] = username
            it[Users.email] = email
            it[Users.passwordHash] = null
            it[Users.displayName] = displayName
            it[Users.authProvider] = provider
            it[Users.providerUserId] = providerUserId
            it[Users.createdAt] = Instant.now()
        } get Users.id
        findById(id)!!
    }

    fun findByProvider(provider: String, providerUserId: String): User? = transaction {
        Users.selectAll()
            .where { (Users.authProvider eq provider) and (Users.providerUserId eq providerUserId) }
            .map { it.toUser() }
            .singleOrNull()
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
        Follows.deleteWhere { with(SqlExpressionBuilder) { (Follows.followerId eq followerId) and (Follows.followingId eq followingId) } }
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

    fun getAllUsers(limit: Int, offset: Long): List<User> = transaction {
        Users.selectAll()
            .orderBy(Users.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toUser() }
    }

    fun setBanned(id: Long, banned: Boolean): User? = transaction {
        Users.update({ Users.id eq id }) { it[Users.isBanned] = banned }
        findById(id)
    }

    fun setAutoApprove(id: Long, autoApprove: Boolean?): User? = transaction {
        Users.update({ Users.id eq id }) { it[Users.autoApprove] = autoApprove }
        findById(id)
    }

    fun countAll(): Long = transaction { Users.selectAll().count() }

    fun updatePassword(userId: Long, newPasswordHash: String): Boolean = transaction {
        Users.update({ Users.id eq userId }) { it[Users.passwordHash] = newPasswordHash } > 0
    }

    /** Case-insensitive search by username or display name. */
    fun search(query: String, limit: Int): List<User> = transaction {
        val pattern = "%${query.trim()}%"
        Users.selectAll()
            .where { (Users.username.lowerCase() like pattern.lowercase()) or (Users.displayName.lowerCase() like pattern.lowercase()) }
            .limit(limit)
            .map { it.toUser() }
    }

    fun getFollowers(userId: Long, limit: Int, offset: Long): List<User> = transaction {
        val followerIds = Follows.selectAll()
            .where { Follows.followingId eq userId }
            .orderBy(Follows.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it[Follows.followerId] }
        followerIds.mapNotNull { findById(it) }
    }

    fun getFollowing(userId: Long, limit: Int, offset: Long): List<User> = transaction {
        val followingIds = Follows.selectAll()
            .where { Follows.followerId eq userId }
            .orderBy(Follows.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it[Follows.followingId] }
        followingIds.mapNotNull { findById(it) }
    }

    /**
     * Ranks users by their number of public, approved quotes. Aggregation is done in
     * plain Kotlin rather than SQL GROUP BY - simpler and safer to reason about at this
     * app's scale, and avoids relying on less battle-tested Exposed aggregate-query APIs.
     */
    fun getTopContributors(limit: Int): List<Pair<User, Int>> = transaction {
        val ownerIds = Quotes.selectAll()
            .where { (Quotes.status eq "APPROVED") and (Quotes.visibility eq "PUBLIC") }
            .map { it[Quotes.ownerId] }

        ownerIds.groupingBy { it }.eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .mapNotNull { (ownerId, count) -> findById(ownerId)?.let { user -> user to count } }
    }
}
