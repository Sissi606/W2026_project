package com.example.cpen321application.util

import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Formats the phone's local time to match what the server returns, so the two
 * clocks appear on screen in the same shape. `xxx` renders the offset as
 * +hh:mm, and 'GMT' is quoted so it is emitted literally.
 *
 * java.time is used directly rather than a desugaring library because minSdk is
 * 26, where it is part of the platform.
 */
object ClientTime {

    private val FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss 'GMT'xxx")

    /** The clock is a parameter so tests can pin it to a fixed instant. */
    fun nowFormatted(clock: Clock = Clock.systemDefaultZone()): String =
        ZonedDateTime.now(clock).format(FORMATTER)
}
