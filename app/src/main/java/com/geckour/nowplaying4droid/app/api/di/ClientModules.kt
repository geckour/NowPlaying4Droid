package com.geckour.nowplaying4droid.app.api.di

import com.geckour.nowplaying4droid.BuildConfig
import com.geckour.nowplaying4droid.app.api.LastFmApiClient
import com.geckour.nowplaying4droid.app.api.MastodonInstancesApiClient
import com.geckour.nowplaying4droid.app.api.SpotifyApiClient
import com.geckour.nowplaying4droid.app.api.YouTubeDataClient
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val clientModule = module {
    single { SpotifyApiClient(androidContext()) }
    single { LastFmApiClient() }
    single { MastodonInstancesApiClient() }
    single { YouTubeDataClient() }
}