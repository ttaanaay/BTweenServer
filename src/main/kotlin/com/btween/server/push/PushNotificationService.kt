package com.btween.server.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
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
     */
    fun sendToTokens(tokens: List<String>, title: String, body: String): List<String> {
        val invalidTokens = mutableListOf<String>()

        tokens.chunked(500).forEach { batch ->
            val message = MulticastMessage.builder()
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
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
