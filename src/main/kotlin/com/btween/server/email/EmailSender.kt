package com.btween.server.email

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Sends transactional emails. [ConsoleEmailSender] is the only implementation right now -
 * it just logs the message, since no email provider is configured yet. Swap in a real
 * implementation (Resend, SendGrid, plain SMTP, etc.) before relying on this in production;
 * until then, reset codes are only visible by checking the Render server logs.
 */
interface EmailSender {
    fun send(to: String, subject: String, body: String)
}

class ConsoleEmailSender : EmailSender {
    override fun send(to: String, subject: String, body: String) {
        println("=== EMAIL (no real provider configured - logged instead) ===")
        println("To: $to")
        println("Subject: $subject")
        println(body)
        println("=== END EMAIL ===")
    }
}

@Serializable
private data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val text: String,
    val html: String
)

/**
 * Sends via Resend's HTTP API (https://resend.com) - a single POST with an API key, no SMTP
 * setup needed. [fromAddress] must be on a domain verified in the Resend dashboard (Resend
 * rejects sends from unverified domains), e.g. "BTween <no-reply@yourdomain.com>".
 *
 * Failures are logged but not thrown - a person's registration or password reset shouldn't
 * fail outright just because the email provider hiccuped; the code still exists server-side
 * (they just won't have gotten the email that round, and can hit resend/retry).
 */
class ResendEmailSender(
    private val apiKey: String,
    private val fromAddress: String
) : EmailSender {

    private val logger = LoggerFactory.getLogger(ResendEmailSender::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override fun send(to: String, subject: String, body: String) {
        runBlocking {
            try {
                val html = body.trim().split("\n").joinToString("<br/>") { it }
                val response = client.post("https://api.resend.com/emails") {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ResendEmailRequest(
                            from = fromAddress,
                            to = listOf(to),
                            subject = subject,
                            text = body,
                            html = html
                        )
                    )
                }
                if (!response.status.isSuccess()) {
                    logger.error("Resend send failed (${response.status}): ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                logger.error("Resend send threw an exception", e)
            }
        }
    }
}
