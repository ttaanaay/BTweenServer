package com.btween.server.data.repository

import com.btween.server.data.tables.Likes
import com.btween.server.data.tables.Quotes
import com.btween.server.domain.Quote
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class QuoteRepository {

    private fun ResultRow.toQuote() = Quote(
        id = this[Quotes.id],
        ownerId = this[Quotes.ownerId],
        text = this[Quotes.text],
        sourceTitle = this[Quotes.sourceTitle],
        sourceType = this[Quotes.sourceType],
        speaker = this[Quotes.speaker],
        author = this[Quotes.author],
        category = this[Quotes.category],
        tags = this[Quotes.tags].split(",").map { it.trim() }.filter { it.length > 0 },
        visibility = this[Quotes.visibility],
        status = this[Quotes.status],
        likeCount = this[Quotes.likeCount],
        createdAt = this[Quotes.createdAt],
        updatedAt = this[Quotes.updatedAt]
    )

    fun create(
        ownerId: Long,
        text: String,
        sourceTitle: String,
        sourceType: String,
        speaker: String,
        author: String?,
        category: String?,
        tags: List<String>,
        visibility: String,
        status: String
    ): Quote = transaction {
        val now = Instant.now()
        val id = Quotes.insert {
            it[Quotes.ownerId] = ownerId
            it[Quotes.text] = text
            it[Quotes.sourceTitle] = sourceTitle
            it[Quotes.sourceType] = sourceType
            it[Quotes.speaker] = speaker
            it[Quotes.author] = author
            it[Quotes.category] = category
            it[Quotes.tags] = tags.joinToString(",")
            it[Quotes.visibility] = visibility
            it[Quotes.status] = status
            it[Quotes.createdAt] = now
            it[Quotes.updatedAt] = now
        } get Quotes.id
        findById(id)!!
    }

    fun update(
        id: Long,
        ownerId: Long,
        text: String,
        sourceTitle: String,
        sourceType: String,
        speaker: String,
        author: String?,
        category: String?,
        tags: List<String>,
        visibility: String
    ): Quote? = transaction {
        val updated = Quotes.update({ (Quotes.id eq id) and (Quotes.ownerId eq ownerId) }) {
            it[Quotes.text] = text
            it[Quotes.sourceTitle] = sourceTitle
            it[Quotes.sourceType] = sourceType
            it[Quotes.speaker] = speaker
            it[Quotes.author] = author
            it[Quotes.category] = category
            it[Quotes.tags] = tags.joinToString(",")
            it[Quotes.visibility] = visibility
            it[Quotes.updatedAt] = Instant.now()
        }
        if (updated > 0) findById(id) else null
    }

    fun delete(id: Long, ownerId: Long): Boolean = transaction {
        Quotes.deleteWhere { with(SqlExpressionBuilder) { (Quotes.id eq id) and (Quotes.ownerId eq ownerId) } } > 0
    }

    /** Admin-only: deletes any quote regardless of ownership. */
    fun adminDelete(id: Long): Boolean = transaction {
        Quotes.deleteWhere { with(SqlExpressionBuilder) { Quotes.id eq id } } > 0
    }

    fun findById(id: Long): Quote? = transaction {
        Quotes.selectAll().where { Quotes.id eq id }.map { it.toQuote() }.singleOrNull()
    }

    fun getPublicFeed(limit: Int, offset: Long): List<Quote> = transaction {
        Quotes.selectAll()
            .where { (Quotes.visibility eq "PUBLIC") and (Quotes.status eq "APPROVED") }
            .orderBy(Quotes.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toQuote() }
    }

    fun getUserQuotes(ownerId: Long, includePrivate: Boolean, limit: Int, offset: Long): List<Quote> = transaction {
        val query = if (includePrivate) {
            // The owner viewing their own profile sees everything, including pending/rejected.
            Quotes.selectAll().where { Quotes.ownerId eq ownerId }
        } else {
            Quotes.selectAll().where {
                (Quotes.ownerId eq ownerId) and (Quotes.visibility eq "PUBLIC") and (Quotes.status eq "APPROVED")
            }
        }
        query.orderBy(Quotes.createdAt, SortOrder.DESC).limit(limit, offset).map { it.toQuote() }
    }

    fun like(userId: Long, quoteId: Long): Boolean = transaction {
        val alreadyLiked = Likes.selectAll()
            .where { (Likes.userId eq userId) and (Likes.quoteId eq quoteId) }
            .count() > 0
        if (!alreadyLiked) {
            Likes.insert {
                it[Likes.userId] = userId
                it[Likes.quoteId] = quoteId
                it[Likes.createdAt] = Instant.now()
            }
            Quotes.update({ Quotes.id eq quoteId }) {
                with(SqlExpressionBuilder) {
                    it.update(Quotes.likeCount, Quotes.likeCount + 1)
                }
            }
            true
        } else {
            false
        }
    }

    fun unlike(userId: Long, quoteId: Long): Boolean = transaction {
        val deleted = Likes.deleteWhere { with(SqlExpressionBuilder) { (Likes.userId eq userId) and (Likes.quoteId eq quoteId) } }
        if (deleted > 0) {
            Quotes.update({ (Quotes.id eq quoteId) and (Quotes.likeCount greater 0) }) {
                with(SqlExpressionBuilder) {
                    it.update(Quotes.likeCount, Quotes.likeCount - 1)
                }
            }
        }
        deleted > 0
    }

    fun likedQuoteIds(userId: Long, quoteIds: List<Long>): Set<Long> = transaction {
        if (quoteIds.size == 0) return@transaction emptySet()
        Likes.selectAll()
            .where { (Likes.userId eq userId) and (Likes.quoteId inList quoteIds) }
            .map { it[Likes.quoteId] }
            .toSet()
    }

    // ---- Admin / moderation ----

    fun getByStatus(status: String, limit: Int, offset: Long): List<Quote> = transaction {
        Quotes.selectAll()
            .where { Quotes.status eq status }
            .orderBy(Quotes.createdAt, SortOrder.ASC)
            .limit(limit, offset)
            .map { it.toQuote() }
    }

    fun setStatus(id: Long, status: String): Quote? = transaction {
        Quotes.update({ Quotes.id eq id }) { it[Quotes.status] = status }
        findById(id)
    }

    fun countAll(): Long = transaction { Quotes.selectAll().count() }

    fun countByStatus(status: String): Long = transaction {
        Quotes.selectAll().where { Quotes.status eq status }.count()
    }
}
