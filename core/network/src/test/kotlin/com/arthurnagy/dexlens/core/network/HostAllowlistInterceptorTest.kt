package com.arthurnagy.dexlens.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class HostAllowlistInterceptorTest {
    // Scenario: Only the expected host
    @Test
    fun allowsOnlyTheConfiguredHost() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().code(200).build())
            server.start()
            val client = OkHttpClient.Builder().addInterceptor(HostAllowlistInterceptor(server.url("/").host)).build()

            val response = client.newCall(Request.Builder().url(server.url("/a")).build()).execute()

            assertEquals(200, response.code)
            response.close()
        }
    }

    // Scenario: Only the expected host
    @Test
    fun doesNotFollowRedirects() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().code(302).addHeader("Location", "https://example.com/elsewhere").build())
            server.start()
            val client = OkHttpClient.Builder().restrictedToHost(server.url("/").host).build()

            val response = client.newCall(Request.Builder().url(server.url("/a")).build()).execute()

            assertEquals(302, response.code)
            assertEquals(1, server.requestCount)
            response.close()
        }
    }

    // Scenario: Only the expected host
    @Test
    fun refusesAnyOtherHost() {
        val client = OkHttpClient.Builder().addInterceptor(HostAllowlistInterceptor("raw.githubusercontent.com")).build()
        val error = assertFailsWith<IOException> {
            client.newCall(Request.Builder().url("https://example.com/").build()).execute()
        }
        assertEquals("Blocked request to example.com", error.message)
    }
}
