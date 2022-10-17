package com.geckour.nowplaying4gpm.app.api

import android.util.Base64
import com.geckour.nowplaying4gpm.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    val clientBuilder: OkHttpClient.Builder
        get() = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)

    val client: OkHttpClient = clientBuilder
        .applyDebugger()
        .build()

    val spotifyAuthClient: OkHttpClient = clientBuilder
        .addInterceptor {
            val tokenString = Base64.encodeToString(
                "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}"
                    .toByteArray(),
                Base64.URL_SAFE or Base64.NO_WRAP
            )
            return@addInterceptor it.proceed(
                it.request()
                    .newBuilder()
                    .header("Authorization", "Basic $tokenString")
                    .build()
            )
        }
        .applyDebugger()
        .build()

    fun getSpotifyApiClient(token: String): OkHttpClient {
        return clientBuilder
            .addInterceptor {
                return@addInterceptor it.proceed(
                    it.request()
                        .newBuilder()
                        .header("Authorization", "Bearer $token")
                        .addHeader("Accept-Language", "ja")
                        .build()
                )
            }
            .applyDebugger()
            .build()
    }

    val mastodonInstancesClient: OkHttpClient = clientBuilder
        .addInterceptor {
            return@addInterceptor it.proceed(
                it.request()
                    .newBuilder()
                    .header("Authorization", "Bearer ${BuildConfig.MASTODON_INSTANCES_SECRET}")
                    .build()
            )
        }
        .applyDebugger()
        .build()

    private fun OkHttpClient.Builder.applyDebugger(): OkHttpClient.Builder =
        apply {
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(
                    HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY)
                )
            }
        }
}