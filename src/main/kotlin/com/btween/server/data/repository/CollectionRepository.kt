package com.btween.server.data.repository

import com.btween.server.data.tables.CollectionItems
import com.btween.server.data.tables.Collections
import com.btween.server.domain.QuoteCollection
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class CollectionRepository {

    private fun ResultRow.toCollection() = QuoteCollection(
        id = this[Collections.id],
        ownerId = this[Collections.ownerId],
        name = this[Collections.name],
        createdAt = this[Collections.createdAt]
    )

    fun create(ownerId: Long, name: String): QuoteCollection = transaction {
        val id = Collections.insert {
            it[Collections.ownerId] = ownerId
            it[Collections.name] = name
            it[Collections.createdAt] = Instant.now()
        } get Collections.id
        Collections.selectAll().where { Collections.id eq id }.map { it.toCollection() }.single()
    }

    fun getForUser(ownerId: Long): List<QuoteCollection> = transaction {
        Collections.selectAll()
            .where { Collections.ownerId eq ownerId }
            .orderBy(Collections.createdAt, SortOrder.DESC)
            .map { it.toCollection() }
    }

    fun findById(id: Long): QuoteCollection? = transaction {
        Collections.selectAll().where { Collections.id eq id }.map { it.toCollection() }.singleOrNull()
    }

    /** Deletes if [ownerId] owns the collection. Item rows cascade-delete automatically. */
    fun delete(id: Long, ownerId: Long): Boolean = transaction {
        Collections.deleteWhere {
            with(SqlExpressionBuilder) { (Collections.id eq id) and (Collections.ownerId eq ownerId) }
        } > 0
    }

    fun addItem(collectionId: Long, quoteId: Long): Boolean = transaction {
        val alreadyIn = CollectionItems.selectAll()
            .where { (CollectionItems.collectionId eq collectionId) and (CollectionItems.quoteId eq quoteId) }
            .any()
        if (alreadyIn) return@transaction false

        CollectionItems.insert {
            it[CollectionItems.collectionId] = collectionId
            it[CollectionItems.quoteId] = quoteId
            it[CollectionItems.addedAt] = Instant.now()
        }
        true
    }

    fun removeItem(collectionId: Long, quoteId: Long): Boolean = transaction {
        CollectionItems.deleteWhere {
            with(SqlExpressionBuilder) { (CollectionItems.collectionId eq collectionId) and (CollectionItems.quoteId eq quoteId) }
        } > 0
    }

    /** Quote ids in the collection, most recently added first. */
    fun getQuoteIds(collectionId: Long): List<Long> = transaction {
        CollectionItems.selectAll()
            .where { CollectionItems.collectionId eq collectionId }
            .orderBy(CollectionItems.addedAt, SortOrder.DESC)
            .map { it[CollectionItems.quoteId] }
    }

    fun countItems(collectionId: Long): Long = transaction {
        CollectionItems.selectAll().where { CollectionItems.collectionId eq collectionId }.count()
    }
}
