package com.btween.server.data.repository

import com.btween.server.data.tables.Notifications
import com.btween.server.domain.Notification
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class NotificationRepository {

    private fun ResultRow.toNotification() = Notification(
        id = this[Notifications.id],
        recipientUserId = this[Notifications.recipientUserId],
        actorUserId = this[Notifications.actorUserId],
        type = this[Notifications.type],
        quoteId = this[Notifications.quoteId],
        isRead = this[Notifications.isRead],
        createdAt = this[Notifications.createdAt]
    )

    /** No-op if recipientUserId == actorUserId - you don't get notified about your own actions. */
    fun create(recipientUserId: Long, actorUserId: Long, type: String, quoteId: Long?) {
        if (recipientUserId == actorUserId) return
        transaction {
            Notifications.insert {
                it[Notifications.recipientUserId] = recipientUserId
                it[Notifications.actorUserId] = actorUserId
                it[Notifications.type] = type
                it[Notifications.quoteId] = quoteId
                it[Notifications.createdAt] = Instant.now()
            }
        }
    }

    fun getForUser(userId: Long, limit: Int, offset: Long): List<Notification> = transaction {
        Notifications.selectAll()
            .where { Notifications.recipientUserId eq userId }
            .orderBy(Notifications.createdAt, SortOrder.DESC)
            .limit(limit, offset)
            .map { it.toNotification() }
    }

    fun countUnread(userId: Long): Long = transaction {
        Notifications.selectAll()
            .where { (Notifications.recipientUserId eq userId) and (Notifications.isRead eq false) }
            .count()
    }

    fun markRead(id: Long, userId: Long): Boolean = transaction {
        Notifications.update({ (Notifications.id eq id) and (Notifications.recipientUserId eq userId) }) {
            it[Notifications.isRead] = true
        } > 0
    }

    fun markAllRead(userId: Long) = transaction {
        Notifications.update({ Notifications.recipientUserId eq userId }) {
            it[Notifications.isRead] = true
        }
    }
}
