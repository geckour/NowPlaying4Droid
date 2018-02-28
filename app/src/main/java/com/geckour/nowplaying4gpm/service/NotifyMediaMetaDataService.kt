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
import com.geckour.nowplaying4gpm.api.model.Image
import com.geckour.nowplaying4gpm.util.*
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class NotifyMediaMetaDataService: NotificationListenerService() {

    companion object {
        const val ACTION_DESTROY_NOTIFICATION: String = "com.geckour.nowplaying4gpm.destroynotification"
        const val ACTION_SHOW_NOTIFICATION: String = "com.geckour.nowplaying4gpm.shownotification"
        const val ACTION_GPM_PLAY_STATE_CHANGED: String = "com.android.music.playstatechanged"
        const val EXTRA_GPM_ARTIST: String = "artist"
        const val EXTRA_GPM_ALBUM: String = "album"
        const val EXTRA_GPM_TRACK: String = "track"
        const val EXTRA_GPM_PLAYING: String = "playing"

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
                    SettingsActivity.getIntent(this@NotifyMediaMetaDataService).apply { startActivity(this) }
                } else {
                    when (action) {
                        ACTION_GPM_PLAY_STATE_CHANGED -> {
                            val title = if (hasExtra(EXTRA_GPM_TRACK)) getStringExtra(EXTRA_GPM_TRACK) else null
                            val artist = if (hasExtra(EXTRA_GPM_ARTIST)) getStringExtra(EXTRA_GPM_ARTIST) else null
                            val album = if (hasExtra(EXTRA_GPM_ALBUM)) getStringExtra(EXTRA_GPM_ALBUM) else null
                            sharedPreferences.refreshCurrentMetadata(title, artist, album)

                            if (hasExtra(EXTRA_GPM_PLAYING).not() || getBooleanExtra(EXTRA_GPM_PLAYING, true))
                                showNotification(title, artist, album)
                            else destroyNotification(true)
                        }

                        ACTION_DESTROY_NOTIFICATION -> destroyNotification()

                        ACTION_SHOW_NOTIFICATION -> showNotification()
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
            addAction(ACTION_GPM_PLAY_STATE_CHANGED)
            addAction(ACTION_DESTROY_NOTIFICATION)
            addAction(ACTION_SHOW_NOTIFICATION)
        }

        registerReceiver(receiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        try { unregisterReceiver(receiver) } catch (e: IllegalArgumentException) { Timber.e(e) }
        destroyNotification(true)
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

    private fun showNotification(title: String?, artist: String?, album: String?) {
        if (sharedPreferences.getWhetherReside()) {
            ui(jobs) {
                val albumArt =
                        getArtworkUriFromDevice(
                                this@NotifyMediaMetaDataService,
                                getAlbumIdFromDevice(this@NotifyMediaMetaDataService, title, artist, album)
                        )?.let {
                            contentResolver.openInputStream(it).let {
                                BitmapFactory.decodeStream(it, null, null).apply { it.close() }
                            }
                        } ?: run {
                            if (sharedPreferences.getWhetherUseApi().not()) null
                            else getBitmapFromUrl(
                                    this@NotifyMediaMetaDataService,
                                    getArtworkUrlFromLastFmApi(LastFmApiClient(), album, artist, Image.Size.MEDIUM)
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
    }

    private fun showNotification() {
        sharedPreferences.apply {
            val title = if (contains(SettingsActivity.PrefKey.PREF_KEY_CURRENT_TITLE.name)) getString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_TITLE.name, null) else null
            val artist = if (contains(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ARTIST.name)) getString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ARTIST.name, null) else null
            val album = if (contains(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ALBUM.name)) getString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ALBUM.name, null) else null

            showNotification(title, artist, album)
        }
    }

    private fun showDummyNotification() =
            startForeground(R.string.notification_channel_id_share, getDummyNotification())

    private fun destroyNotification(clearMetadata: Boolean = false) {
        stopForeground(true)
        if (clearMetadata) sharedPreferences.refreshCurrentMetadata(null, null, null)
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
                        if (Build.VERSION.SDK_INT >= 26 && sharedPreferences.getWhetherColorizeNotificationBg()) setColorized(true)
                        val color = Palette.from(this).generate().let{
                            when (paletteArray[sharedPreferences.getChoseColorIndex()]) {
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