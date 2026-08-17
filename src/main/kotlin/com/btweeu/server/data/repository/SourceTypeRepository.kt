package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.SourceTypes
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

data class SourceTypeItem(val id: Long, val name: String, val sortOrder: Int)

class SourceTypeRepository {

    private fun ResultRow.toItem() = SourceTypeItem(
        id = this[SourceTypes.id],
        name = this[SourceTypes.name],
        sortOrder = this[SourceTypes.sortOrder]
    )

    fun getAll(): List<SourceTypeItem> = transaction {
        SourceTypes.selectAll().orderBy(SourceTypes.sortOrder, SortOrder.ASC).map { it.toItem() }
    }

    fun create(name: String): SourceTypeItem = transaction {
        val maxOrder = SourceTypes.selectAll().maxOfOrNull { it[SourceTypes.sortOrder] } ?: -1
        val id = SourceTypes.insert {
            it[SourceTypes.name] = name
            it[sortOrder] = maxOrder + 1
            it[createdAt] = Instant.now()
        } get SourceTypes.id
        SourceTypes.selectAll().where { SourceTypes.id eq id }.map { it.toItem() }.single()
    }

    fun delete(id: Long): Boolean = transaction {
        SourceTypes.deleteWhere { with(SqlExpressionBuilder) { SourceTypes.id eq id } } > 0
    }

    /** Seeds the original hardcoded list on first run only, so existing installs don't
     * suddenly lose all their source type options. No-ops if any already exist. */
    fun seedDefaultsIfEmpty() = transaction {
        if (SourceTypes.selectAll().empty()) {
            val defaults = listOf("MOVIE", "TV_SERIES", "BOOK", "ANIME", "GAME", "PODCAST", "SPEECH", "OTHER")
            defaults.forEachIndexed { index, name ->
                SourceTypes.insert {
                    it[SourceTypes.name] = name
                    it[sortOrder] = index
                    it[createdAt] = Instant.now()
                }
            }
        }
    }
}
