package com.arthurnagy.dexlens.core.network.content

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Test
import retrofit2.Retrofit
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class ContentRemoteDataSourceTest {
    private fun MockWebServer.source() = RetrofitContentRemoteDataSource(
        Retrofit.Builder().baseUrl(url("/")).client(OkHttpClient()).build(),
    )

    @Test
    fun requestsTheExpectedRepositoryPaths() = runTest {
        MockWebServer().use { server ->
            repeat(2) { server.enqueue(MockResponse.Builder().code(200).body("png").build()) }
            server.start()
            val source = server.source()

            assertContentEquals("png".toByteArray(), source.artwork(25))
            assertEquals("png", source.flavorText { it.readBytes().decodeToString() })

            assertEquals("/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png", server.takeRequest().url.encodedPath)
            assertEquals("/PokeAPI/pokeapi/master/data/v2/csv/pokemon_species_flavor_text.csv", server.takeRequest().url.encodedPath)
        }
    }
}
