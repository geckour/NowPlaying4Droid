package com.geckour.nowplaying4gpm.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.geckour.nowplaying4gpm.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object OkHttpProvider {

    val client: OkHttpClient = OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    addNetworkInterceptor(StethoInterceptor())
                }
            }.build()
}