package com.example.cpen321application.ui.pixels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.data.remote.PixelEvent
import com.example.cpen321application.data.remote.PixelSocketClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ConnectionState { CONNECTING, LIVE, RECONNECTING }

class PixelArtViewModel(
    private val socketClient: PixelSocketClient
) : ViewModel() {

    /**
     * The grid, one colour per cell, row-major. A SnapshotStateList is used so
     * Compose recomposes only the cell that changed -- with 256 cells updating
     * many times a second, replacing a whole immutable grid each time would
     * mean redrawing everything on every pixel.
     *
     * NO_COLOR marks a cell the stream has not painted yet, which is what keeps
     * the canvas genuinely blank at the start rather than filled with black.
     */
    val cells = mutableStateListOf<Int>().apply {
        repeat(GRID_SIZE * GRID_SIZE) { add(NO_COLOR) }
    }

    var connectionState by mutableStateOf(ConnectionState.CONNECTING)
        private set

    var pixelsReceived by mutableStateOf(0)
        private set

    init {
        listen()
    }

    /**
     * Reconnects on its own: a dropped socket would otherwise leave a frozen
     * half-built image with no way back short of leaving the screen.
     */
    private fun listen() {
        viewModelScope.launch {
            var backoffMs = INITIAL_BACKOFF_MS

            while (isActive) {
                socketClient.connect().collect { event ->
                    when (event) {
                        is PixelEvent.Connected -> {
                            connectionState = ConnectionState.LIVE
                            backoffMs = INITIAL_BACKOFF_MS
                        }

                        is PixelEvent.Pixel -> {
                            apply(event.update.x, event.update.y, event.update.color)
                        }

                        is PixelEvent.Disconnected -> {
                            connectionState = ConnectionState.RECONNECTING
                        }
                    }
                }

                // The flow completed, meaning the socket closed.
                if (!isActive) break
                connectionState = ConnectionState.RECONNECTING
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    /** Ignores out-of-range coordinates rather than crashing on a bad frame. */
    private fun apply(x: Int, y: Int, hex: String) {
        if (x !in 0 until GRID_SIZE || y !in 0 until GRID_SIZE) return

        val color = parseHexColor(hex) ?: return
        cells[y * GRID_SIZE + x] = color
        pixelsReceived += 1
    }

    companion object {
        const val GRID_SIZE = 16
        const val NO_COLOR = 0

        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 15_000L

        /**
         * Accepts #RRGGBB and #AARRGGBB, with or without the leading '#'.
         * Returns null for anything else, so one malformed colour cannot take
         * down the stream.
         */
        fun parseHexColor(hex: String): Int? {
            val cleaned = hex.removePrefix("#").trim()
            val value = cleaned.toLongOrNull(radix = 16) ?: return null

            return when (cleaned.length) {
                6 -> (0xFF000000L or value).toInt()
                8 -> value.toInt()
                else -> null
            }
        }

        fun factory(socketClient: PixelSocketClient) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PixelArtViewModel(socketClient) as T
        }
    }
}
