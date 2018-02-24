package com.geckour.nowplaying4gpm

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import timber.log.Timber

class App: Application() {

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        }

        if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name).not()) {
            sharedPreferences.edit()
                    .putString(SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, getString(R.string.default_sharing_text_pattern))
                    .apply()
        }
    }
}