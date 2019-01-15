package com.geckour.nowplaying4gpm.api

import android.util.Base64
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.geckour.nowplaying4gpm.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)

    val client: OkHttpClient = clientBuilder
            .applyDebugger()
            .build()

    var spotifyAuthToken: String? = null

    val spotifyAuthClient: OkHttpClient =
            clientBuilder
                    .addInterceptor {
                        val tokenString = Base64.encodeToString(
                                "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}"
                                        .toByteArray(),
                                Base64.URL_SAFE or Base64.NO_WRAP
                        )
                        return@addInterceptor it.proceed(it.request()
                                .newBuilder().header("Authorization", "Basic $tokenString")
                                .build())
                    }
                    .applyDebugger()
                    .build()

    val spotifyApiClient: OkHttpClient
        get() = spotifyAuthToken?.let { token ->
            clientBuilder
                    .addInterceptor {
                        return@addInterceptor it.proceed(it.request()
                                .newBuilder().header("Authorization",
                                        "Bearer $token")
                                .build())
                    }
                    .applyDebugger().build()
        } ?: throw IllegalStateException("Init auth token first.")

    val mastodonInstancesClient: OkHttpClient = clientBuilder
            .addInterceptor {
                return@addInterceptor it.proceed(it.request()
                        .newBuilder().header("Authorization",
                                "Bearer ${BuildConfig.MASTODON_INSTANCES_SECRET}")
                        .build())
            }
            .applyDebugger()
            .build()

    fun OkHttpClient.Builder.applyDebugger(): OkHttpClient.Builder =
            apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                            HttpLoggingInterceptor()
                                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                    addNetworkInterceptor(StethoInterceptor())
                }
            }
}