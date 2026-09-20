package com.example.cpen321application.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ClientTimeTest {

    // Input: a fixed instant read in a zone 7 hours behind UTC
    // Expected behavior: 24-hour clock with a +hh:mm offset, matching the
    //   format the server produces so both clocks read alike on screen
    // Expected output: "09:05:03 GMT-07:00"
    @Test
    fun `formats a zone behind UTC`() {
        val clock = Clock.fixed(
            Instant.parse("2026-09-19T16:05:03Z"),
            ZoneId.of("America/Vancouver")
        )

        assertEquals("09:05:03 GMT-07:00", ClientTime.nowFormatted(clock))
    }

    // Input: a fixed instant read in a zone 2 hours ahead of UTC
    // Expected behavior: the offset sign flips; afternoon stays 24-hour
    // Expected output: "23:59:59 GMT+02:00"
    @Test
    fun `formats a zone ahead of UTC`() {
        val clock = Clock.fixed(
            Instant.parse("2026-09-19T21:59:59Z"),
            ZoneId.of("Europe/Berlin")
        )

        assertEquals("23:59:59 GMT+02:00", ClientTime.nowFormatted(clock))
    }

    // Input: a fixed instant in a zone offset by a non-whole number of hours
    // Expected behavior: leftover minutes are rendered, not truncated
    // Expected output: "17:45:00 GMT+05:45"
    @Test
    fun `formats a fractional-hour offset`() {
        val clock = Clock.fixed(
            Instant.parse("2026-09-19T12:00:00Z"),
            ZoneId.of("Asia/Kathmandu")
        )

        assertEquals("17:45:00 GMT+05:45", ClientTime.nowFormatted(clock))
    }

    // Input: a fixed instant at UTC
    // Expected behavior: a zero offset renders as "+00:00", never "Z"
    // Expected output: "00:00:00 GMT+00:00"
    @Test
    fun `formats zero offset as plus zero`() {
        val clock = Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneId.of("UTC"))

        assertEquals("00:00:00 GMT+00:00", ClientTime.nowFormatted(clock))
    }

    // Input: the real system clock
    // Expected behavior: the default argument works, and output always matches
    //   the agreed shape
    // Expected output: a string matching hh:mm:ss GMT±hh:mm
    @Test
    fun `default clock produces the agreed shape`() {
        val pattern = Regex("""^\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}$""")

        assertTrue(pattern.matches(ClientTime.nowFormatted()))
    }
}
