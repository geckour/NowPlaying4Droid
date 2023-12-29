package com.geckour.nowplaying4droid.app.api

import android.util.Base64
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import com.geckour.nowplaying4droid.BuildConfig
import com.geckour.nowplaying4droid.app.util.withCatching
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    private const val APPLE_TOKEN_EXPIRE_DURATION = 15777000000
    private val appleJwt = JWT.create()
        .withIssuer("5228F48D57")
        .withKeyId("AFWU7W3X4T")
    private val appleJwtAlgorithm = Algorithm.ECDSA256(
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
    private var appleToken: String? = null

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
        .addInterceptor { chain ->
            appleToken?.let {
                val verifier = JWT.require(appleJwtAlgorithm).build()
                runCatching { verifier.verify(it) }
                    .onFailure { t ->
                        Timber.e(t)
                        if (t is TokenExpiredException) return@let null
                    }
            } ?: run {
                refreshAppleToken()
            }
            return@addInterceptor chain.proceed(
                chain.request()
                    .newBuilder()
                    .apply {
                        appleToken?.let {
                            header("Authorization", "Bearer $it")
                        }
                    }
                    .build()
            )
        }
        .applyDebugger()
        .build()

    private fun refreshAppleToken(current: Long = System.currentTimeMillis()) {
        withCatching {
            appleToken = appleJwt
                .withIssuedAt(Date(current))
                .withExpiresAt(Date(current + APPLE_TOKEN_EXPIRE_DURATION))
                .sign(appleJwtAlgorithm)
        }
    }

    private fun OkHttpClient.Builder.applyDebugger(): OkHttpClient.Builder =
        apply {
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(
                    HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY)
                )
            }
        }

    private fun Collection<*>.toJsonElement(): JsonElement = JsonArray(mapNotNull { it.toJsonElement() })

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