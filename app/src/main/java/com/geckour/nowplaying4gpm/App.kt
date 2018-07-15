package com.geckour.nowplaying4gpm

import android.app.Application
import android.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.geckour.nowplaying4gpm.util.init
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        }

        PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .init(this)
    }
}