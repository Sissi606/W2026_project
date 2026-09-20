package com.example.cpen321application.ui.pixels

import com.example.cpen321application.data.remote.PixelSocketClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PixelColorTest {

    // Input: a six-digit hex colour with a leading '#'
    // Expected behavior: parsed to an opaque ARGB int, alpha forced to 0xFF so
    //   the cell is not drawn fully transparent
    // Expected output: 0xFFFF8800
    @Test
    fun `parses six-digit hex with hash`() {
        assertEquals(0xFFFF8800.toInt(), PixelArtViewModel.parseHexColor("#ff8800"))
    }

    // Input: the same colour without the leading '#'
    // Expected behavior: the prefix is optional
    // Expected output: 0xFFFF8800
    @Test
    fun `parses six-digit hex without hash`() {
        assertEquals(0xFFFF8800.toInt(), PixelArtViewModel.parseHexColor("ff8800"))
    }

    // Input: an eight-digit hex colour carrying its own alpha
    // Expected behavior: the given alpha is preserved, not overwritten
    // Expected output: 0x80FF8800
    @Test
    fun `parses eight-digit hex preserving alpha`() {
        assertEquals(0x80FF8800.toInt(), PixelArtViewModel.parseHexColor("#80ff8800"))
    }

    // Input: uppercase hex
    // Expected behavior: case-insensitive
    // Expected output: 0xFFABCDEF
    @Test
    fun `parses uppercase hex`() {
        assertEquals(0xFFABCDEF.toInt(), PixelArtViewModel.parseHexColor("#ABCDEF"))
    }

    // Input: a string that is not hex
    // Expected behavior: rejected, so one bad frame skips a pixel rather than
    //   crashing the screen
    // Expected output: null
    @Test
    fun `rejects non-hex input`() {
        assertNull(PixelArtViewModel.parseHexColor("#nothex"))
    }

    // Input: a hex string of the wrong length
    // Expected behavior: rejected rather than silently misinterpreted
    // Expected output: null
    @Test
    fun `rejects wrong-length hex`() {
        assertNull(PixelArtViewModel.parseHexColor("#fff"))
    }
}

class PixelSocketUrlTest {

    // Input: the deployed HTTPS base URL
    // Expected behavior: https maps to wss so the socket is encrypted too, and
    //   the pixel path is appended
    // Expected output: "wss://34-29-207-92.sslip.io/ws/pixels"
    @Test
    fun `maps https to wss`() {
        assertEquals(
            "wss://34-29-207-92.sslip.io/ws/pixels",
            PixelSocketClient.toWebSocketUrl("https://34-29-207-92.sslip.io")
        )
    }

    // Input: a local HTTP base URL with a port
    // Expected behavior: http maps to ws, port preserved
    // Expected output: "ws://10.0.2.2:3000/ws/pixels"
    @Test
    fun `maps http to ws`() {
        assertEquals(
            "ws://10.0.2.2:3000/ws/pixels",
            PixelSocketClient.toWebSocketUrl("http://10.0.2.2:3000")
        )
    }

    // Input: a base URL that already ends in a slash
    // Expected behavior: no doubled slash before the path
    // Expected output: "wss://example.com/ws/pixels"
    @Test
    fun `does not double the separating slash`() {
        assertEquals(
            "wss://example.com/ws/pixels",
            PixelSocketClient.toWebSocketUrl("https://example.com/")
        )
    }
}
