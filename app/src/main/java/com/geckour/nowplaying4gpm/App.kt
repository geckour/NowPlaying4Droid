package com.geckour.nowplaying4gpm

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SettingsActivity.Companion.paletteArray
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
            if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name).not())
                putInt(SettingsActivity.PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name, paletteArray.indexOf(R.string.palette_light_vibrant))
            if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name).not())
                putBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name, true)
        }.apply()
    }
}