package com.geckour.nowplaying4gpm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.support.annotation.RequiresApi
import android.support.v7.graphics.Palette
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SettingsActivity.Companion.paletteArray
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.util.*
import com.geckour.nowplaying4gpm.util.AsyncUtil.getArtworkBitmap
import com.geckour.nowplaying4gpm.util.AsyncUtil.getArtworkUri
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class NotificationService : NotificationListenerService() {

    enum class Channel(val id: Int) {
        NOTIFICATION_CHANNEL_SHARE(180)
    }

    companion object {
        const val ACTION_DESTROY_NOTIFICATION: String = "com.geckour.nowplaying4gpm.destroynotification"
        const val ACTION_SHOW_NOTIFICATION: String = "com.geckour.nowplaying4gpm.shownotification"
        const val BUNDLE_KEY_TRACK_INFO: String = "bundle_key_track_info"

        fun getIntent(context: Context): Intent =
                Intent(context, NotificationService::class.java)

        fun sendNotification(context: Context, trackCoreElement: TrackCoreElement?) {
            context.checkStoragePermission {
                it.startService(getIntent(context))
                it.sendBroadcast(Intent().apply {
                    action = NotificationService.ACTION_SHOW_NOTIFICATION
                    putExtra(NotificationService.BUNDLE_KEY_TRACK_INFO,
                            trackCoreElement ?: TrackCoreElement(null, null, null))
                })
            }
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                when (action) {
                    ACTION_DESTROY_NOTIFICATION -> destroyNotification()

                    ACTION_SHOW_NOTIFICATION -> {
                        if (context == null) return

                        val trackCoreElement =
                                (intent.extras?.get(BUNDLE_KEY_TRACK_INFO) as TrackCoreElement?)
                                        ?: TrackCoreElement(null, null, null)

                        async { showNotification(trackCoreElement) }
                    }
                }
            }
        }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private val lastFmApiClient: LastFmApiClient = LastFmApiClient()
    private val jobs: ArrayList<Job> = ArrayList()
    private var notificationBitmap: Bitmap? = null
    private var trackCoreElement: TrackCoreElement =
            TrackCoreElement(null, null, null)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG.not()) Fabric.with(this, Crashlytics())


        if (Build.VERSION.SDK_INT >= 26) {
            createDefaultChannel()
            showDummyNotification()
            destroyNotification()
        }

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_DESTROY_NOTIFICATION)
            addAction(ACTION_SHOW_NOTIFICATION)
        }

        registerReceiver(receiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }

        destroyNotification()
        jobs.cancelAll()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDefaultChannel() {
        val name = getString(R.string.notification_channel_name_share)
        val description = getString(R.string.notification_channel_description_share)

        val channel =
                NotificationChannel(
                        Channel.NOTIFICATION_CHANNEL_SHARE.name,
                        name,
                        NotificationManager.IMPORTANCE_LOW
                ).apply { this.description = description }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showNotification(trackCoreElement: TrackCoreElement) {
        if (trackCoreElement.isAllNonNull) {
            checkStoragePermission {
                ui(jobs) {
                    val albumArt =
                            async {
                                when (trackCoreElement) {
                                    this@NotificationService.trackCoreElement -> notificationBitmap
                                    else -> getArtworkBitmap(
                                            this@NotificationService,
                                            lastFmApiClient,
                                            trackCoreElement)
                                }
                            }.await()

                    notificationBitmap = albumArt
                    this@NotificationService.trackCoreElement = trackCoreElement

                    getNotification(albumArt, trackCoreElement)?.apply {
                        startForeground(Channel.NOTIFICATION_CHANNEL_SHARE.id, this)
                    }
                }
            }
        } else destroyNotification()
    }

    private fun showDummyNotification() =
            startForeground(Channel.NOTIFICATION_CHANNEL_SHARE.id, getDummyNotification())

    private fun destroyNotification() =
            if (Build.VERSION.SDK_INT >= 26) stopForeground(true)
            else cancelAllNotifications()

    private suspend fun getNotification(thumb: Bitmap?, trackCoreElement: TrackCoreElement): Notification? {
        if (trackCoreElement.isAllNonNull.not()) return null

        val notificationBuilder =
                if (Build.VERSION.SDK_INT >= 26)
                    Notification.Builder(this, Channel.NOTIFICATION_CHANNEL_SHARE.name)
                else Notification.Builder(this)

        return notificationBuilder.apply {
            val actionOpenSetting =
                    PendingIntent.getActivity(
                            this@NotificationService,
                            0,
                            SettingsActivity.getIntent(this@NotificationService),
                            PendingIntent.FLAG_CANCEL_CURRENT
                    ).let {
                        Notification.Action.Builder(
                                Icon.createWithResource(this@NotificationService,
                                        R.drawable.ic_settings_black_24px),
                                getString(R.string.action_open_pref),
                                it
                        ).build()
                    }
            val notificationText =
                    sharedPreferences.getString(
                            PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                            getString(R.string.default_sharing_text_pattern))
                            .getSharingText(trackCoreElement)

            val uri =
                    if (sharedPreferences.getWhetherBundleArtwork())
                        getArtworkUri(this@NotificationService,
                                lastFmApiClient,
                                TrackInfo(trackCoreElement))
                    else null

            setSmallIcon(R.drawable.ic_notification)
            setLargeIcon(thumb)
            setContentTitle(getString(R.string.notification_title))
            setContentText(notificationText)
            setContentIntent(
                    PendingIntent.getActivity(
                            this@NotificationService,
                            0,
                            SharingActivity.getIntent(this@NotificationService,
                                    sharedPreferences.getFormatPattern(this@NotificationService)
                                            .getSharingText(trackCoreElement),
                                    uri),
                            PendingIntent.FLAG_CANCEL_CURRENT
                    )
            )
            setOngoing(true)
            if (Build.VERSION.SDK_INT >= 24) {
                setStyle(Notification.DecoratedMediaCustomViewStyle())
                addAction(actionOpenSetting)
            }
            thumb?.apply {
                if (Build.VERSION.SDK_INT >= 26
                        && sharedPreferences.getWhetherColorizeNotificationBg()) {
                    setColorized(true)
                }

                val color = Palette.from(this).generate().let {
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
            (if (Build.VERSION.SDK_INT >= 26)
                Notification.Builder(this, Channel.NOTIFICATION_CHANNEL_SHARE.name)
            else Notification.Builder(this)).apply {
                setSmallIcon(R.drawable.ic_notification)
                setContentTitle(getString(R.string.notification_title))
                setContentText(getString(R.string.notification_text_dummy))
            }.build()
}