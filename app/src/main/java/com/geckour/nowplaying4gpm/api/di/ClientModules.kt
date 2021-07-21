package com.geckour.nowplaying4gpm.api.di

import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.MastodonInstancesApiClient
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
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