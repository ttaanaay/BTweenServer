package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.CollectionItems
import com.btweeu.server.data.tables.Collections
import com.btweeu.server.data.tables.Comments
import com.btweeu.server.data.tables.Follows
import com.btweeu.server.data.tables.Likes
import com.btweeu.server.data.tables.Notifications
import com.btweeu.server.data.tables.PasswordResets
import com.btweeu.server.data.tables.Quotes
import com.btweeu.server.data.tables.Users
import com.btweeu.server.domain.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

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
        isSuperAdmin = this[Users.isSuperAdmin],
        isBanned = this[Users.isBanned],
        emailVerified = this[Users.emailVerified],
        failedLoginAttempts = this[Users.failedLoginAttempts],
        lockedUntil = this[Users.lockedUntil],
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
            it[Users.emailVerified] = false
            it[Users.createdAt] = Instant.now()
        } get Users.id
        findById(id)!!
    }

    /** Creates an account for a user who signed in via Google/Facebook/Microsoft - no password.
     * The provider already verified this email address, so it starts out verified here too. */
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
            it[Users.emailVerified] = true
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

    fun setAdmin(id: Long, isAdmin: Boolean): User? = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.isAdmin] = isAdmin
            // Revoking moderator access has to revoke super admin access too - a super
            // admin with isAdmin=false would be a contradictory, half-revoked state.
            if (!isAdmin) it[Users.isSuperAdmin] = false
        }
        findById(id)
    }

    fun setSuperAdmin(id: Long, isSuperAdmin: Boolean): User? = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.isSuperAdmin] = isSuperAdmin
            // Granting super admin access implies regular (moderator-level) access too.
            if (isSuperAdmin) it[Users.isAdmin] = true
        }
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

    fun markEmailVerified(userId: Long): Boolean = transaction {
        Users.update({ Users.id eq userId }) { it[Users.emailVerified] = true } > 0
    }

    private val MAX_FAILED_ATTEMPTS = 5
    private val LOCKOUT_MINUTES = 15L

    /** Call after a failed login. Locks the account once the threshold is hit. */
    fun recordFailedLogin(userId: Long): Unit = transaction {
        val current = Users.selectAll().where { Users.id eq userId }
            .map { it[Users.failedLoginAttempts] }
            .singleOrNull() ?: return@transaction
        val newCount = current + 1

        Users.update({ Users.id eq userId }) {
            it[failedLoginAttempts] = newCount
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                it[lockedUntil] = Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES)
            }
        }
    }

    /** Call after a successful login - clears any accumulated failed-attempt count. */
    fun resetFailedLogins(userId: Long) = transaction {
        Users.update({ Users.id eq userId }) {
            it[failedLoginAttempts] = 0
            it[lockedUntil] = null
        }
    }

    /**
     * Deletes the account and everything it owns. The relevant tables already declare
     * ON DELETE CASCADE for these foreign keys, but this project's schema has evolved
     * iteratively via `createMissingTablesAndColumns` rather than fresh `CREATE TABLE`s, so
     * there's no strong guarantee every constraint was retroactively applied to the live
     * database. Deleting dependents by hand first, in dependency order, means this works
     * correctly either way - the manual deletes are simply redundant (and harmless) on rows
     * the database would have cascaded on its own.
     */
    fun deleteAccount(userId: Long): Boolean = transaction {
        val ownedQuoteIds = Quotes.selectAll().where { Quotes.ownerId eq userId }.map { it[Quotes.id] }

        // Comments: ones this user wrote, and ones anyone wrote on this user's quotes.
        Comments.deleteWhere { with(SqlExpressionBuilder) { Comments.userId eq userId } }
        if (ownedQuoteIds.isNotEmpty()) {
            Comments.deleteWhere { with(SqlExpressionBuilder) { Comments.quoteId inList ownedQuoteIds } }
        }

        // Likes: ones this user gave, and ones anyone gave on this user's quotes.
        Likes.deleteWhere { with(SqlExpressionBuilder) { Likes.userId eq userId } }
        if (ownedQuoteIds.isNotEmpty()) {
            Likes.deleteWhere { with(SqlExpressionBuilder) { Likes.quoteId inList ownedQuoteIds } }
        }

        // Collections: this user's own collections and their items, plus any OTHER user's
        // collection items that point at a quote this user owned (about to be deleted).
        val ownCollectionIds = Collections.selectAll().where { Collections.ownerId eq userId }.map { it[Collections.id] }
        if (ownCollectionIds.isNotEmpty()) {
            CollectionItems.deleteWhere { with(SqlExpressionBuilder) { CollectionItems.collectionId inList ownCollectionIds } }
        }
        if (ownedQuoteIds.isNotEmpty()) {
            CollectionItems.deleteWhere { with(SqlExpressionBuilder) { CollectionItems.quoteId inList ownedQuoteIds } }
        }
        Collections.deleteWhere { with(SqlExpressionBuilder) { Collections.ownerId eq userId } }

        // Notifications: this user was either the recipient or the actor.
        Notifications.deleteWhere {
            with(SqlExpressionBuilder) { (Notifications.recipientUserId eq userId) or (Notifications.actorUserId eq userId) }
        }

        // Follows: this user either side of the relationship.
        Follows.deleteWhere {
            with(SqlExpressionBuilder) { (Follows.followerId eq userId) or (Follows.followingId eq userId) }
        }

        PasswordResets.deleteWhere { with(SqlExpressionBuilder) { PasswordResets.userId eq userId } }

        // Quotes owned by this user, now that nothing else references them.
        Quotes.deleteWhere { with(SqlExpressionBuilder) { Quotes.ownerId eq userId } }

        Users.deleteWhere { with(SqlExpressionBuilder) { Users.id eq userId } } > 0
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
