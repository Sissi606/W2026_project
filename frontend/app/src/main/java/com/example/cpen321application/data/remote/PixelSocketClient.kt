package com.example.cpen321application.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/** One cell of the 16x16 grid, exactly as the course server sends it. */
@Serializable
data class PixelUpdate(val x: Int, val y: Int, val color: String)

/** What the screen needs to know about the connection itself. */
sealed interface PixelEvent {
    data object Connected : PixelEvent
    data class Pixel(val update: PixelUpdate) : PixelEvent
    data class Disconnected(val reason: String) : PixelEvent
}

/**
 * Streams pixel updates from our backend's relay.
 *
 * Unauthenticated by design: the three buttons must work independently, so this
 * cannot depend on Button 1 having been used first. A separate OkHttp client is
 * built here rather than reusing ApiClient's, precisely so the auth interceptor
 * is not in the chain.
 */
class PixelSocketClient(private val baseUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        // The stream is silent during the 5-second pause between images, so
        // the read timeout must not treat quiet as failure. Pings keep the
        // connection alive through any intermediate proxy instead.
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    /**
     * Emits until the collector stops. Callers handle reconnection: OkHttp does
     * not retry a closed WebSocket on its own.
     */
    fun connect(): Flow<PixelEvent> = callbackFlow {
        val request = Request.Builder().url(toWebSocketUrl(baseUrl)).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                trySend(PixelEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // A malformed frame skips that one pixel rather than tearing
                // down a stream that is otherwise healthy.
                val update = runCatching { json.decodeFromString<PixelUpdate>(text) }
                    .getOrNull() ?: return
                trySend(PixelEvent.Pixel(update))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(PixelEvent.Disconnected(t.message ?: "connection failed"))
                close()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                trySend(PixelEvent.Disconnected("closed ($code)"))
                close()
            }
        }

        val socket = client.newWebSocket(request, listener)
        awaitClose { socket.cancel() }
    }

    companion object {
        const val PIXEL_PATH = "ws/pixels"

        /**
         * Derives the socket URL from the HTTP base URL, so there is a single
         * address to configure. https maps to wss, http to ws.
         */
        fun toWebSocketUrl(baseUrl: String): String {
            val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            return when {
                normalized.startsWith("https://") ->
                    normalized.replaceFirst("https://", "wss://") + PIXEL_PATH
                normalized.startsWith("http://") ->
                    normalized.replaceFirst("http://", "ws://") + PIXEL_PATH
                else -> normalized + PIXEL_PATH
            }
        }
    }
}
