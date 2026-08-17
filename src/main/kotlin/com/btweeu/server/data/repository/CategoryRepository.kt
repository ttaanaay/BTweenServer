package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.Categories
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

data class Category(val id: Long, val name: String, val icon: String, val sortOrder: Int)

class CategoryRepository {

    private fun ResultRow.toCategory() = Category(
        id = this[Categories.id],
        name = this[Categories.name],
        icon = this[Categories.icon],
        sortOrder = this[Categories.sortOrder]
    )

    fun getAll(): List<Category> = transaction {
        Categories.selectAll().orderBy(Categories.sortOrder, SortOrder.ASC).map { it.toCategory() }
    }

    fun create(name: String, icon: String): Category = transaction {
        val maxOrder = Categories.selectAll().maxOfOrNull { it[Categories.sortOrder] } ?: -1
        val id = Categories.insert {
            it[Categories.name] = name
            it[Categories.icon] = icon
            it[sortOrder] = maxOrder + 1
            it[createdAt] = Instant.now()
        } get Categories.id
        Categories.selectAll().where { Categories.id eq id }.map { it.toCategory() }.single()
    }

    fun delete(id: Long): Boolean = transaction {
        Categories.deleteWhere { with(SqlExpressionBuilder) { Categories.id eq id } } > 0
    }

    fun update(id: Long, name: String, icon: String): Category? = transaction {
        val updated = Categories.update({ Categories.id eq id }) {
            it[Categories.name] = name
            it[Categories.icon] = icon
        }
        if (updated == 0) return@transaction null
        Categories.selectAll().where { Categories.id eq id }.map { it.toCategory() }.singleOrNull()
    }

    /** Seeds the original hardcoded category list on first run only, so existing installs
     * don't suddenly show an empty category row. No-ops if any categories already exist. */
    fun seedDefaultsIfEmpty() = transaction {
        if (Categories.selectAll().empty()) {
            val defaults = listOf(
                "Life" to "sun",
                "Love" to "heart",
                "Motivation" to "flame",
                "Success" to "trophy",
                "Wisdom" to "brain",
                "Humor" to "laugh",
                "Books" to "book",
                "Movie" to "movie"
            )
            defaults.forEachIndexed { index, (name, icon) ->
                Categories.insert {
                    it[Categories.name] = name
                    it[Categories.icon] = icon
                    it[sortOrder] = index
                    it[createdAt] = Instant.now()
                }
            }
        }
    }
}
