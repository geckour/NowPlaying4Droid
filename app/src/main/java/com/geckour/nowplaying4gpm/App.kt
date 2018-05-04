package com.geckour.nowplaying4gpm

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.geckour.nowplaying4gpm.util.init
import timber.log.Timber

class App : Application() {

    companion object {
        const val PACKAGE_NAME_GPM: String = "com.google.android.music"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        }

        sharedPreferences.init(this)
    }
}