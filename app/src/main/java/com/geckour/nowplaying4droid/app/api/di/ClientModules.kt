package com.geckour.nowplaying4droid.app.api.di

import com.geckour.nowplaying4droid.BuildConfig
import com.geckour.nowplaying4droid.app.api.LastFmApiClient
import com.geckour.nowplaying4droid.app.api.MastodonInstancesApiClient
import com.geckour.nowplaying4droid.app.api.SpotifyApiClient
import com.geckour.nowplaying4droid.app.api.TwitterApiClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val clientModule = module {
    single { SpotifyApiClient(androidContext()) }
    single {
        TwitterApiClient(
            BuildConfig.TWITTER_CONSUMER_KEY,
            BuildConfig.TWITTER_CONSUMER_SECRET
        )
    }
    single { LastFmApiClient() }
    single { MastodonInstancesApiClient() }
}