package com.geckour.nowplaying4gpm

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.geckour.nowplaying4gpm.service.MediaMetadataService
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.util.checkStoragePermission
import com.geckour.nowplaying4gpm.util.init
import timber.log.Timber

class App : Application() {

    companion object {
        fun launchMetaDataService(context: Context?) {
            context?.startService(MediaMetadataService.getIntent(context))
        }
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

        launchMetaDataService(this)
        launchNotificationService()
    }

    private fun launchNotificationService() {
        checkStoragePermission {
            it.startService(NotificationService.getIntent(it))
        }
    }
}