package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.Comments
import com.btweeu.server.domain.Comment
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class CommentRepository {

    private fun ResultRow.toComment() = Comment(
        id = this[Comments.id],
        quoteId = this[Comments.quoteId],
        userId = this[Comments.userId],
        text = this[Comments.text],
        createdAt = this[Comments.createdAt],
        updatedAt = this[Comments.updatedAt]
    )

    fun create(quoteId: Long, userId: Long, text: String): Comment = transaction {
        val id = Comments.insert {
            it[Comments.quoteId] = quoteId
            it[Comments.userId] = userId
            it[Comments.text] = text
            it[Comments.createdAt] = Instant.now()
        } get Comments.id
        Comments.selectAll().where { Comments.id eq id }.map { it.toComment() }.single()
    }

    /** Updates if [userId] owns the comment. Returns the updated Comment, or null if not found/owned. */
    fun update(id: Long, userId: Long, text: String): Comment? = transaction {
        val updated = Comments.update({ (Comments.id eq id) and (Comments.userId eq userId) }) {
            it[Comments.text] = text
            it[Comments.updatedAt] = Instant.now()
        }
        if (updated > 0) findById(id) else null
    }

    fun getForQuote(quoteId: Long, limit: Int, offset: Long): List<Comment> = transaction {
        Comments.selectAll()
            .where { Comments.quoteId eq quoteId }
            .orderBy(Comments.createdAt, SortOrder.ASC)
            .limit(limit, offset)
            .map { it.toComment() }
    }

    /** Admin drill-down: a user's own comments, most recent first. */
    fun getForUser(userId: Long, limit: Int, offset: Long): List<Comment> = transaction {
        Comments.selectAll()
            .where { Comments.userId eq userId }
            .orderBy(Comments.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toComment() }
    }

    fun countForQuote(quoteId: Long): Long = transaction {
        Comments.selectAll().where { Comments.quoteId eq quoteId }.count()
    }

    fun findById(id: Long): Comment? = transaction {
        Comments.selectAll().where { Comments.id eq id }.map { it.toComment() }.singleOrNull()
    }

    /** Deletes if [userId] owns the comment. Returns false if not found or not owned. */
    fun delete(id: Long, userId: Long): Boolean = transaction {
        Comments.deleteWhere { with(SqlExpressionBuilder) { (Comments.id eq id) and (Comments.userId eq userId) } } > 0
    }

    /** Admin-only: deletes any comment regardless of ownership. */
    fun adminDelete(id: Long): Boolean = transaction {
        Comments.deleteWhere { with(SqlExpressionBuilder) { Comments.id eq id } } > 0
    }
}
