package com.btween.server.data.repository

import com.btween.server.data.tables.Reports
import com.btween.server.domain.Report
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

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
}
