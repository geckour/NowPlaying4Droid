package com.geckour.nowplaying4droid.app.api.di

import com.geckour.nowplaying4droid.app.api.AppleMusicApiClient
import com.geckour.nowplaying4droid.app.api.LastFmApiClient
import com.geckour.nowplaying4droid.app.api.MastodonInstancesApiClient
import com.geckour.nowplaying4droid.app.api.SpotifyApiClient
import com.geckour.nowplaying4droid.app.api.YouTubeDataClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val clientModule = module {
    single { SpotifyApiClient(androidContext()) }
    single { LastFmApiClient() }
    single { MastodonInstancesApiClient() }
    single { YouTubeDataClient() }
    single { AppleMusicApiClient() }
}