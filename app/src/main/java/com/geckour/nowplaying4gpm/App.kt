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

        sharedPreferences.edit().apply {
            if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name).not())
                putString(SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, getString(R.string.default_sharing_text_pattern))
            if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_CHOSE_COLOR_ID.name).not())
                putInt(SettingsActivity.PrefKey.PREF_KEY_CHOSE_COLOR_ID.name, R.string.palette_light_vibrant)
        }.apply()
    }
}