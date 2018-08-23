package com.geckour.nowplaying4gpm

import android.app.Application
import com.facebook.stetho.Stetho
import timber.log.Timber

class App : Application() {

    companion object {
        const val MASTODON_CLIENT_NAME = "NowPlaying4Droid"
        const val MASTODON_CALLBACK = "np4gpm://mastodon.callback"
        const val MASTODON_WEB_URL = "https://github.com/geckour/NowPlaying4Droid"
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        }
    }
}