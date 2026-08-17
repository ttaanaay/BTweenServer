package com.btweeu.server.data.repository

import com.btweeu.server.data.tables.Quotes
import com.btweeu.server.data.tables.Reports
import com.btweeu.server.data.tables.Users
import com.btweeu.server.domain.Report
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

data class FlaggedUser(val userId: Long, val reportCount: Int, val isBanned: Boolean)

class ReportRepository {

    private fun ResultRow.toReport() = Report(
        id = this[Reports.id],
        reporterId = this[Reports.reporterId],
        targetType = this[Reports.targetType],
        targetId = this[Reports.targetId],
        reason = this[Reports.reason],
        details = this[Reports.details],
        status = this[Reports.status],
        createdAt = this[Reports.createdAt]
    )

    fun create(reporterId: Long, targetType: String, targetId: Long, reason: String, details: String?): Report =
        transaction {
            val id = Reports.insert {
                it[Reports.reporterId] = reporterId
                it[Reports.targetType] = targetType
                it[Reports.targetId] = targetId
                it[Reports.reason] = reason
                it[Reports.details] = details
                it[Reports.createdAt] = Instant.now()
            } get Reports.id
            Reports.selectAll().where { Reports.id eq id }.map { it.toReport() }.single()
        }

    fun getByStatus(status: String, limit: Int, offset: Long): List<Report> = transaction {
        Reports.selectAll()
            .where { Reports.status eq status }
            .orderBy(Reports.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toReport() }
    }

    fun countPending(): Long = transaction {
        Reports.selectAll().where { Reports.status eq "PENDING" }.count()
    }

    fun findById(id: Long): Report? = transaction {
        Reports.selectAll().where { Reports.id eq id }.map { it.toReport() }.singleOrNull()
    }

    fun updateStatus(id: Long, status: String): Boolean = transaction {
        Reports.update({ Reports.id eq id }) { it[Reports.status] = status } > 0
    }

    /** Users worth an admin's attention: whoever a report ultimately points at, whether the
     * report is against their account directly (targetType "USER") or against one of their
     * quotes (targetType "QUOTE", resolved to the quote's owner here). Counts every report
     * regardless of status - a dismissed report still reflects something an admin may want
     * visibility into if the pattern repeats. */
    fun getFlaggedUsers(minReportCount: Int, limit: Int): List<FlaggedUser> = transaction {
        val allReports = Reports.selectAll().map { it[Reports.targetType] to it[Reports.targetId] }

        val quoteIds = allReports.filter { it.first == "QUOTE" }.map { it.second }.distinct()
        val quoteOwnerById = if (quoteIds.isEmpty()) {
            emptyMap()
        } else {
            Quotes.selectAll().where { Quotes.id inList quoteIds }
                .associate { it[Quotes.id] to it[Quotes.ownerId] }
        }

        val userIdCounts = allReports
            .mapNotNull { (type, targetId) ->
                when (type) {
                    "USER" -> targetId
                    "QUOTE" -> quoteOwnerById[targetId]
                    else -> null
                }
            }
            .groupingBy { it }
            .eachCount()

        val bannedById = Users.selectAll().where { Users.id inList userIdCounts.keys }
            .associate { it[Users.id] to it[Users.isBanned] }

        userIdCounts
            .filterValues { it >= minReportCount }
            .map { (userId, count) -> FlaggedUser(userId, count, bannedById[userId] ?: false) }
            .sortedByDescending { it.reportCount }
            .take(limit)
    }
}
