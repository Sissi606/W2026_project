package com.example.cpen321application.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiClientTest {

    // Input: a base URL with no trailing slash, as pasted into local.properties
    // Expected behavior: a slash is appended, since Retrofit rejects a base URL
    //   without one and would crash at startup
    // Expected output: "https://example.com/"
    @Test
    fun `appends a missing trailing slash`() {
        assertEquals(
            "https://example.com/",
            ApiClient.normalizeBaseUrl("https://example.com")
        )
    }

    // Input: a base URL that already ends in a slash
    // Expected behavior: left alone rather than doubled
    // Expected output: the input unchanged
    @Test
    fun `leaves an existing trailing slash alone`() {
        assertEquals(
            "https://example.com/",
            ApiClient.normalizeBaseUrl("https://example.com/")
        )
    }

    // Input: the emulator's host address with a port
    // Expected behavior: a port does not confuse the check
    // Expected output: "http://10.0.2.2:3000/"
    @Test
    fun `handles a host with a port`() {
        assertEquals(
            "http://10.0.2.2:3000/",
            ApiClient.normalizeBaseUrl("http://10.0.2.2:3000")
        )
    }
}
