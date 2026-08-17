package com.btweeu.server.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import java.io.ByteArrayInputStream

class PushNotificationService(serviceAccountJson: String) {

    private val messaging: FirebaseMessaging

    init {
        val credentials = GoogleCredentials.fromStream(
            ByteArrayInputStream(serviceAccountJson.toByteArray(Charsets.UTF_8))
        )
        val options = FirebaseOptions.builder().setCredentials(credentials).build()
        val app = FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(options)
        messaging = FirebaseMessaging.getInstance(app)
    }

    /**
     * Sends to up to 500 tokens per Firebase's own multicast limit - splits into batches
     * transparently so callers don't need to think about that. Returns the tokens that
     * turned out to be invalid (app uninstalled, notifications disabled at the OS level,
     * etc) so the caller can clean them out of the database.
     *
     * Deliberately sent as a data-only message (no .setNotification(...)) rather than a
     * notification message. Notification messages get displayed automatically by Android
     * itself whenever the app isn't in the foreground - bypassing the app's own code
     * entirely, which for us meant a stale/duplicate quote could linger on screen instead
     * of showing this send's actual text. Data messages always route through the app's own
     * onMessageReceived(), in every app state, so the notification shown always matches
     * what was just sent.
     */
    fun sendToTokens(tokens: List<String>, title: String, body: String): List<String> {
        val invalidTokens = mutableListOf<String>()

        tokens.chunked(500).forEach { batch ->
            val message = MulticastMessage.builder()
                .putData("title", title)
                .putData("body", body)
                .addAllTokens(batch)
                .build()

            val response = messaging.sendEachForMulticast(message)
            response.responses.forEachIndexed { index, sendResponse ->
                if (!sendResponse.isSuccessful) {
                    val exception = sendResponse.exception
                    if (exception is FirebaseMessagingException &&
                        (exception.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                            exception.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT)
                    ) {
                        invalidTokens.add(batch[index])
                    }
                }
            }
        }

        return invalidTokens
    }
}
