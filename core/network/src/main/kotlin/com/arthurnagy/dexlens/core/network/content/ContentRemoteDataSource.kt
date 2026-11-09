package com.arthurnagy.dexlens.core.network.content

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.io.InputStream
import javax.inject.Inject

/** The optional download's files, all from PokeAPI's public repositories on raw.githubusercontent.com. */
interface ContentRemoteDataSource {
    suspend fun sprite(id: Int): ByteArray

    suspend fun artwork(id: Int): ByteArray

    /** Streams the flavor text table to [read] without holding the whole file in memory. */
    suspend fun <T> flavorText(read: suspend (InputStream) -> T): T
}

internal interface ContentApi {
    @GET("PokeAPI/sprites/master/sprites/pokemon/{id}.png")
    suspend fun sprite(
        @Path("id") id: Int,
    ): ResponseBody

    @GET("PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/{id}.png")
    suspend fun artwork(
        @Path("id") id: Int,
    ): ResponseBody

    @Streaming
    @GET("PokeAPI/pokeapi/master/data/v2/csv/pokemon_species_flavor_text.csv")
    suspend fun flavorText(): ResponseBody
}

internal class RetrofitContentRemoteDataSource
    @Inject
    constructor(
        retrofit: Retrofit,
    ) : ContentRemoteDataSource {
        private val api = retrofit.create(ContentApi::class.java)

        override suspend fun sprite(id: Int): ByteArray = api.sprite(id).use { it.bytes() }

        override suspend fun artwork(id: Int): ByteArray = api.artwork(id).use { it.bytes() }

        override suspend fun <T> flavorText(read: suspend (InputStream) -> T): T = api.flavorText().use { read(it.byteStream()) }
    }
