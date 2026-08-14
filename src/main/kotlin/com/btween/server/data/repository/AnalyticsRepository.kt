package com.btween.server.data.repository

import com.btween.server.data.tables.Comments
import com.btween.server.data.tables.LoginEvents
import com.btween.server.data.tables.Likes
import com.btween.server.data.tables.Quotes
import com.btween.server.data.tables.Users
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class DailyCounts(
    val date: LocalDate,
    val newUsers: Int,
    val newQuotes: Int,
    val newLikes: Int,
    val newComments: Int,
    // Distinct users who logged in that day - a truer "usage" signal than newUsers, which
    // only counts brand-new registrations.
    val activeUsers: Int
)

class AnalyticsRepository {

    /** Groups by UTC calendar day. Fine-grained-enough for a personal-scale app that this
     * fetches raw timestamps and buckets them in Kotlin, rather than a DB-specific date-trunc
     * SQL function - keeps it portable if the underlying Postgres setup ever changes. */
    fun getDailyCounts(days: Int): List<DailyCounts> = transaction {
        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)

        val userDates = Users.selectAll().where { Users.createdAt greaterEq since }
            .map { it[Users.createdAt].atZone(ZoneOffset.UTC).toLocalDate() }
        val quoteDates = Quotes.selectAll().where { Quotes.createdAt greaterEq since }
            .map { it[Quotes.createdAt].atZone(ZoneOffset.UTC).toLocalDate() }
        val likeDates = Likes.selectAll().where { Likes.createdAt greaterEq since }
            .map { it[Likes.createdAt].atZone(ZoneOffset.UTC).toLocalDate() }
        val commentDates = Comments.selectAll().where { Comments.createdAt greaterEq since }
            .map { it[Comments.createdAt].atZone(ZoneOffset.UTC).toLocalDate() }
        val logins = LoginEvents.selectAll().where { LoginEvents.occurredAt greaterEq since }
            .map { it[LoginEvents.occurredAt].atZone(ZoneOffset.UTC).toLocalDate() to it[LoginEvents.userId] }

        val today = LocalDate.now(ZoneOffset.UTC)
        (0 until days).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            DailyCounts(
                date = date,
                newUsers = userDates.count { it == date },
                newQuotes = quoteDates.count { it == date },
                newLikes = likeDates.count { it == date },
                newComments = commentDates.count { it == date },
                activeUsers = logins.filter { it.first == date }.map { it.second }.distinct().size
            )
        }
    }
}
