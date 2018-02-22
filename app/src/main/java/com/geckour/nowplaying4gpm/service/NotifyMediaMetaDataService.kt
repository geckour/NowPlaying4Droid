package com.geckour.nowplaying4gpm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.util.getSharingText
import timber.log.Timber

class NotifyMediaMetaDataService: NotificationListenerService() {

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, NotifyMediaMetaDataService::class.java)
    }

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val controller: MediaController by lazy { getSystemService(MediaSessionManager::class.java).getActiveSessions(null).last() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        Timber.d("pyon!")

        controller.registerCallback(
                object: MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        super.onMetadataChanged(metadata)

                        metadata?.apply {
                            getNotification(
                                    getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
                                    getString(MediaMetadata.METADATA_KEY_TITLE),
                                    getString(MediaMetadata.METADATA_KEY_ARTIST),
                                    getString(MediaMetadata.METADATA_KEY_ALBUM)
                            ).apply {
                                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(0, this)
                            }
                        }
                    }
                }
        )

        Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("My notification")
                .setContentText("Hello World!")
                .build()
                .apply { (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(0, this) }
    }

    private fun getNotification(thumb: Bitmap?, title: String, artist: String, album: String): Notification =
            (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
            else Notification.Builder(this)).apply {
                setSmallIcon(R.mipmap.ic_launcher)
                setLargeIcon(thumb)
                setContentTitle(getString(R.string.notification_title))
                val notificationText =
                        sharedPreferences.getString(
                                SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                                getString(R.string.default_sharing_text_pattern))
                                .getSharingText(title, artist, album)
                setContentText(notificationText)
                setContentIntent(
                        PendingIntent.getActivity(
                                this@NotifyMediaMetaDataService,
                                0,
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("twitter://post?message=$notificationText")
                                },
                                0
                        )
                )
            }.build()
}