package com.arthurnagy.dexlens.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

/** The app's only network use is the optional download, so any other host is refused before a socket is opened. */
internal class HostAllowlistInterceptor(
    private val allowedHost: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        if (host != allowedHost) throw IOException("Blocked request to $host")
        return chain.proceed(chain.request())
    }
}

/** Limits a client to [allowedHost]. Redirects are off, because a redirect could lead to another host unchecked. */
internal fun OkHttpClient.Builder.restrictedToHost(allowedHost: String): OkHttpClient.Builder = this
    .addInterceptor(HostAllowlistInterceptor(allowedHost))
    .followRedirects(false)
    .followSslRedirects(false)
