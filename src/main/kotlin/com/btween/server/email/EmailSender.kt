package com.btween.server.email

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
