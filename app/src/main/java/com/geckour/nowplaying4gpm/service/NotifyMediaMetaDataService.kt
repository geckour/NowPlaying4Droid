package com.geckour.nowplaying4gpm.service

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SettingsActivity.Companion.paletteArray
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.util.*
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class NotifyMediaMetaDataService: NotificationListenerService() {

    companion object {
        const val ACTION_GPM_META_CHANGED: String = "com.android.music.metachanged"
        const val ACTION_GPM_PLAY_STATE_CHANGED: String = "com.android.music.playstatechanged"
        const val EXTRA_GPM_ARTIST: String = "artist"
        const val EXTRA_GPM_ALBUM: String = "album"
        const val EXTRA_GPM_TRACK: String = "track"
        const val EXTRA_GPM_PLAYING: String = "playing"
        const val ACTION_GPM_QUEUE_CHANGED: String = "com.android.music.queuechanged"

        fun getIntent(context: Context): Intent = Intent(context, NotifyMediaMetaDataService::class.java)
    }

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }
    private val jobs: ArrayList<Job> = ArrayList()

    private val receiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                if (ContextCompat.checkSelfPermission(
                                this@NotifyMediaMetaDataService,
                                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    SettingsActivity.getIntent(this@NotifyMediaMetaDataService).apply {
                        startActivity(this)
                    }
                } else {
                    when (action) {
                        ACTION_GPM_META_CHANGED -> {
                            showNotification(this)
                        }

                        ACTION_GPM_PLAY_STATE_CHANGED -> {
                            if (hasExtra(EXTRA_GPM_PLAYING) && !getBooleanExtra(EXTRA_GPM_PLAYING, true)) destroyNotification()
                            else showNotification(this)
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            createDefaultChannel()
            showDummyNotification()
            destroyNotification()
        }

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_GPM_META_CHANGED)
            addAction(ACTION_GPM_PLAY_STATE_CHANGED)
        }
        registerReceiver(receiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
        jobs.cancelAll()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDefaultChannel() {
        val name = getString(R.string.notification_channel_name_share)
        val description = getString(R.string.notification_channel_description_share)
        val channel =
                NotificationChannel(
                        getString(R.string.notification_channel_id_share),
                        name,
                        NotificationManager.IMPORTANCE_LOW
                ).apply { this.description = description }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showNotification(intent: Intent) {
        ui(jobs) {
            val title = if (intent.hasExtra(EXTRA_GPM_TRACK)) intent.getStringExtra(EXTRA_GPM_TRACK) else null
            val artist = if (intent.hasExtra(EXTRA_GPM_ARTIST)) intent.getStringExtra(EXTRA_GPM_ARTIST) else null
            val album = if (intent.hasExtra(EXTRA_GPM_ALBUM)) intent.getStringExtra(EXTRA_GPM_ALBUM) else null
            sharedPreferences.refreshCurrent(title, artist, album)

            val albumArt =
                    try {
                        contentResolver.openInputStream(
                                getArtworkUriFromDevice(
                                        getAlbumIdFromDevice(this@NotifyMediaMetaDataService, title, artist, album)
                                )
                        ).let {
                            BitmapFactory.decodeStream(it, null, null).apply { it.close() }
                        }
                    } catch (e: Throwable) {
                        Timber.e(e)

                        getBitmapFromUrl(
                                this@NotifyMediaMetaDataService,
                                getArtworkUrlFromLastFmApi(LastFmApiClient(), album, artist)
                        )
                    }

            getNotification(
                    albumArt,
                    title,
                    artist,
                    album
            )?.apply { startForeground(R.string.notification_channel_id_share, this) }
        }
    }

    private fun showDummyNotification() =
            startForeground(R.string.notification_channel_id_share, getDummyNotification())

    private fun destroyNotification() {
        stopForeground(true)
        sharedPreferences.refreshCurrent(null, null, null)
    }

    private fun getNotification(thumb: Bitmap?, title: String?, artist: String?, album: String?): Notification? =
            if (title == null || artist == null || album == null) null
            else {
                (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, getString(R.string.notification_channel_id_share))
                else Notification.Builder(this)).apply {
                    val actionOpenSetting =
                            PendingIntent.getActivity(
                                    this@NotifyMediaMetaDataService,
                                    0,
                                    SettingsActivity.getIntent(this@NotifyMediaMetaDataService),
                                    PendingIntent.FLAG_CANCEL_CURRENT
                            ).let {
                                Notification.Action.Builder(
                                        Icon.createWithResource(this@NotifyMediaMetaDataService, R.drawable.ic_settings_black_24px),
                                        getString(R.string.action_open_pref),
                                        it
                                ).build()
                            }
                    val notificationText =
                            sharedPreferences.getString(
                                    SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                                    getString(R.string.default_sharing_text_pattern))
                                    .getSharingText(title, artist, album)

                    setSmallIcon(R.drawable.ic_notification)
                    setLargeIcon(thumb)
                    setContentTitle(getString(R.string.notification_title))
                    setContentText(notificationText)
                    setContentIntent(
                            PendingIntent.getActivity(
                                    this@NotifyMediaMetaDataService,
                                    0,
                                    SharingActivity.createIntent(this@NotifyMediaMetaDataService, title, artist, album),
                                    PendingIntent.FLAG_CANCEL_CURRENT
                            )
                    )
                    setOngoing(true)
                    if (Build.VERSION.SDK_INT >= 24) {
                        setStyle(Notification.DecoratedMediaCustomViewStyle())
                        addAction(actionOpenSetting)
                    }
                    thumb?.apply {
                        if (Build.VERSION.SDK_INT >= 26) setColorized(true)
                        val color = Palette.from(this).generate().let{
                            when (paletteArray[sharedPreferences.getChoseColorIndexFromPreference()]) {
                                R.string.palette_light_vibrant -> it.getLightVibrantColor(Color.WHITE)
                                R.string.palette_vibrant -> it.getVibrantColor(Color.WHITE)
                                R.string.palette_dark_vibrant -> it.getDarkVibrantColor(Color.WHITE)
                                R.string.palette_light_muted -> it.getLightMutedColor(Color.WHITE)
                                R.string.palette_muted -> it.getMutedColor(Color.WHITE)
                                R.string.palette_dark_muted -> it.getDarkMutedColor(Color.WHITE)
                                else -> it.getLightVibrantColor(Color.WHITE)
                            }
                        }
                        setColor(color)
                    }
                }.build()
            }

    private fun getDummyNotification(): Notification =
            (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, getString(R.string.notification_channel_id_share))
            else Notification.Builder(this)).apply {
                setSmallIcon(R.drawable.ic_notification)
                setContentTitle(getString(R.string.notification_title))
                setContentText(getString(R.string.notification_text_dummy))
            }.build()
}