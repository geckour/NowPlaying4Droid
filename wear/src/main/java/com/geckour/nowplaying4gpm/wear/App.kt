package com.geckour.nowplaying4gpm.wear

import android.app.Application
import com.geckour.nowplaying4gpm.BuildConfig
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}