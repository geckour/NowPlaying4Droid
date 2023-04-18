package com.geckour.nowplaying4droid.app.api

import android.util.Base64
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.geckour.nowplaying4droid.BuildConfig
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
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

    val appleMusicApiClient: OkHttpClient = clientBuilder
        .addInterceptor {
            val current = Date()
            val token = JWT.create()
                .withIssuer("5228F48D57")
                .withIssuedAt(current)
                .withExpiresAt(Date(current.time + 15777000000))
                .withKeyId("AFWU7W3X4T")
                .sign(
                    Algorithm.ECDSA256(
                        null,
                        KeyFactory.getInstance("EC")
                            .generatePrivate(
                                PKCS8EncodedKeySpec(
                                    Base64.decode(
                                        BuildConfig.APPLE_MUSIC_KIT_SECRET_KEY,
                                        Base64.DEFAULT
                                    )
                                )
                            ) as ECPrivateKey
                    )
                )
            return@addInterceptor it.proceed(
                it.request()
                    .newBuilder()
                    .header("Authorization", "Bearer $token")
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

    fun Collection<*>.toJsonElement(): JsonElement = JsonArray(mapNotNull { it.toJsonElement() })

    private fun Map<*, *>.toJsonElement(): JsonElement = JsonObject(
        mapNotNull {
            (it.key as? String ?: return@mapNotNull null) to it.value.toJsonElement()
        }.toMap(),
    )

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is Map<*, *> -> toJsonElement()
        is Collection<*> -> toJsonElement()
        else -> JsonPrimitive(toString())
    }
}