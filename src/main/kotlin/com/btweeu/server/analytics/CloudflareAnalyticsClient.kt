package com.btweeu.server.analytics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit

@Serializable
data class VisitorStatsResponse(
    val totalVisits: Long,
    val dailyVisits: List<DailyVisits>,
    val topReferrers: List<ReferrerVisits>
)

@Serializable
data class DailyVisits(val date: String, val visits: Long)

@Serializable
data class ReferrerVisits(val referrer: String, val visits: Long)

// ---- Raw shapes matching Cloudflare's GraphQL response exactly ----

@Serializable
private data class GraphQlEnvelope(val data: GraphQlData? = null, val errors: List<GraphQlError>? = null)

@Serializable
private data class GraphQlError(val message: String)

@Serializable
private data class GraphQlData(val viewer: Viewer? = null)

@Serializable
private data class Viewer(val accounts: List<AccountNode> = emptyList())

@Serializable
private data class AccountNode(
    val total: List<TotalGroup> = emptyList(),
    val byDay: List<DayGroup> = emptyList(),
    val byReferrer: List<ReferrerGroup> = emptyList()
)

@Serializable
private data class TotalGroup(val sum: VisitsSum)

@Serializable
private data class DayGroup(val sum: VisitsSum, val dimensions: DayDimensions)

@Serializable
private data class ReferrerGroup(val sum: VisitsSum, val dimensions: ReferrerDimensions)

@Serializable
private data class VisitsSum(val visits: Long)

@Serializable
private data class DayDimensions(val date: String)

@Serializable
private data class ReferrerDimensions(@SerialName("metric") val refererHost: String)

/**
 * Cloudflare's Web Analytics (RUM) data lives in the account-scoped
 * `rumPageloadEventsAdaptiveGroups` GraphQL dataset, filtered down to just this site via
 * `siteTag` (Web Analytics sites aren't the same thing as DNS zones, so this isn't a zoneTag).
 * All three of token/account tag/site tag are required together - see AppConfig for why they're
 * optional at the config level.
 */
class CloudflareAnalyticsClient(
    private val httpClient: HttpClient,
    private val apiToken: String?,
    private val accountTag: String?,
    private val siteTag: String?
) {
    val isEnabled: Boolean get() = apiToken != null && accountTag != null && siteTag != null

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getVisitorStats(days: Int = 30): VisitorStatsResponse? {
        if (!isEnabled) return null

        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS).toString()
        val until = Instant.now().toString()

        val query = """
            query VisitorStats(${'$'}accountTag: string, ${'$'}filter: AccountRumPageloadEventsAdaptiveGroupsFilter_InputObject) {
              viewer {
                accounts(filter: { accountTag: ${'$'}accountTag }) {
                  total: rumPageloadEventsAdaptiveGroups(filter: ${'$'}filter, limit: 1) {
                    sum { visits }
                  }
                  byDay: rumPageloadEventsAdaptiveGroups(filter: ${'$'}filter, limit: 100, orderBy: [date_ASC]) {
                    sum { visits }
                    dimensions { date }
                  }
                  byReferrer: rumPageloadEventsAdaptiveGroups(filter: ${'$'}filter, limit: 10, orderBy: [sum_visits_DESC]) {
                    sum { visits }
                    dimensions { metric: refererHost }
                  }
                }
              }
            }
        """.trimIndent()

        val requestBody = buildJsonRequest(query, since, until)

        val response = httpClient.post("https://api.cloudflare.com/client/v4/graphql") {
            header("Authorization", "Bearer $apiToken")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val envelope = json.decodeFromString<GraphQlEnvelope>(response.body())
        if (!envelope.errors.isNullOrEmpty()) {
            throw RuntimeException("Cloudflare GraphQL error: ${envelope.errors.joinToString { it.message }}")
        }

        val account = envelope.data?.viewer?.accounts?.firstOrNull() ?: return VisitorStatsResponse(0, emptyList(), emptyList())

        return VisitorStatsResponse(
            totalVisits = account.total.firstOrNull()?.sum?.visits ?: 0,
            dailyVisits = account.byDay.map { DailyVisits(it.dimensions.date, it.sum.visits) },
            topReferrers = account.byReferrer.map {
                ReferrerVisits(it.dimensions.refererHost.ifBlank { "Direct" }, it.sum.visits)
            }
        )
    }

    /** Escapes a string for safe embedding inside a JSON string literal - just the query
     * text here, which only ever contains backslashes, quotes, and newlines from the
     * multi-line GraphQL literal above, not arbitrary untrusted input. */
    private fun escapeJsonString(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")
        .replace("\t", "\\t")

    private fun buildJsonRequest(query: String, since: String, until: String): String {
        // Built directly rather than through kotlinx.serialization data classes, since the
        // variables shape (siteTag inside an AND array alongside datetime bounds) is fiddly
        // to model and this is the only place it's needed.
        return """
            {
              "query": "${escapeJsonString(query)}",
              "variables": {
                "accountTag": "$accountTag",
                "filter": {
                  "AND": [
                    { "datetime_geq": "$since", "datetime_leq": "$until" },
                    { "siteTag": "$siteTag" }
                  ]
                }
              }
            }
        """.trimIndent()
    }
}
