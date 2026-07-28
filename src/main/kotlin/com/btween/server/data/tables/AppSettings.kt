package com.btween.server.data.tables

import org.jetbrains.exposed.sql.*

/**
 * A single-row table (id is always 1) holding server-wide settings an admin can change
 * without redeploying. Currently just the default moderation behavior for new quotes.
 */
object AppSettings : Table("app_settings") {
    val id = integer("id")
    // If true, new quotes are published immediately unless the owner has an explicit
    // autoApprove=false override. If false, new quotes are PENDING by default unless the
    // owner has an explicit autoApprove=true override.
    val defaultAutoApprove = bool("default_auto_approve").default(false)
    // Guards the one-time backfill in DatabaseFactory that marks quotes created before the
    // pending-review feature existed as APPROVED, so they don't vanish from the feed the
    // moment this migration runs.
    val legacyQuotesMigrated = bool("legacy_quotes_migrated").default(false)

    override val primaryKey = PrimaryKey(id)
}
