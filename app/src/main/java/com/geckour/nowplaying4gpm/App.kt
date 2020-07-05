package com.geckour.nowplaying4gpm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.geckour.nowplaying4gpm.api.di.clientModule
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.di.settingsViewModelModule
import com.geckour.nowplaying4gpm.util.refreshCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.refreshTempArtwork
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        startKoin {
            androidContext(this@App)
            modules(clientModule, settingsViewModelModule)
        }

        PreferenceManager.getDefaultSharedPreferences(this).apply {
            refreshCurrentTrackInfo(null)
            refreshTempArtwork(null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channelDefault =
            NotificationChannel(
                NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name,
                getString(R.string.notification_channel_name_share),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                this.description = getString(R.string.notification_channel_description_share)
            }

        val channelNotify =
            NotificationChannel(
                NotificationService.Channel.NOTIFICATION_CHANNEL_NOTIFY.name,
                getString(R.string.notification_channel_name_notify),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                this.description = getString(R.string.notification_channel_description_notify)
            }

        getSystemService(NotificationManager::class.java)?.apply {
            createNotificationChannel(channelDefault)
            createNotificationChannel(channelNotify)
        }
    }
}