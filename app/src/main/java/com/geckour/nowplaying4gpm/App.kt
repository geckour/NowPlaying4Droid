package com.geckour.nowplaying4gpm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.facebook.stetho.Stetho
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import timber.log.Timber

class App : Application() {

    companion object {
        const val MASTODON_CLIENT_NAME = "NowPlaying4Droid"
        const val MASTODON_CALLBACK = "np4gpm://mastodon.callback"
        const val MASTODON_WEB_URL = "https://github.com/geckour/NowPlaying4Droid"
        private const val SHORTCUT_ID_INVOKE_SHARE = "shortcut_id_invoke_share"
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        }

        if (Build.VERSION.SDK_INT >= 25) {
            val shortcutInfo = ShortcutInfo.Builder(this, SHORTCUT_ID_INVOKE_SHARE)
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher_round))
                    .setShortLabel(getString(R.string.shortcut_invoke_share))
                    .setIntent(SharingActivity.getIntent(this.applicationContext).apply {
                        action = Intent.ACTION_DEFAULT
                    })
                    .build()
            getSystemService(ShortcutManager::class.java).dynamicShortcuts = listOf(shortcutInfo)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channelDefault =
                NotificationChannel(
                        NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name,
                        getString(R.string.notification_channel_name_share),
                        NotificationManager.IMPORTANCE_LOW
                ).apply { this.description = getString(R.string.notification_channel_description_share) }

        val channelNotify =
                NotificationChannel(
                        NotificationService.Channel.NOTIFICATION_CHANNEL_NOTIFY.name,
                        getString(R.string.notification_channel_name_notify),
                        NotificationManager.IMPORTANCE_LOW
                ).apply { this.description = getString(R.string.notification_channel_description_notify) }

        getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(channelDefault)
            createNotificationChannel(channelNotify)
        }
    }
}