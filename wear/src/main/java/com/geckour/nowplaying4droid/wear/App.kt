package com.geckour.nowplaying4droid.wear

import android.app.Application
import com.geckour.nowplaying4droid.BuildConfig
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}