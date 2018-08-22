package com.geckour.nowplaying4gpm.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.geckour.nowplaying4gpm.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                            HttpLoggingInterceptor()
                                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                    addNetworkInterceptor(StethoInterceptor())
                }
            }.build()

    val mastodonInstancesClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor {
                return@addInterceptor it.proceed(it.request()
                        .newBuilder().header("Authorization",
                                "Bearer ${BuildConfig.MASTODON_INSTANCES_SECRET}")
                        .build())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                            HttpLoggingInterceptor()
                                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                    addNetworkInterceptor(StethoInterceptor())
                }
            }.build()
}