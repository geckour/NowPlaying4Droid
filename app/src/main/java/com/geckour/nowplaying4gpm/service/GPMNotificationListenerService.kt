package com.geckour.nowplaying4gpm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi
import android.support.v7.graphics.Palette
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.util.*
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class GPMNotificationListenerService : NotificationListenerService() {

    enum class Channel(val id: Int) {
        NOTIFICATION_CHANNEL_SHARE(180)
    }

    companion object {
        private const val PACKAGE_NAME_GPM: String = "com.google.android.music"
        const val ACTION_DESTROY_NOTIFICATION: String = "com.geckour.nowplaying4gpm.destroynotification"
        const val ACTION_SHOW_NOTIFICATION: String = "com.geckour.nowplaying4gpm.shownotification"
        const val BUNDLE_KEY_TRACK_INFO: String = "bundle_key_track_info"

        fun sendNotification(context: Context, trackInfo: TrackInfo?) {
            context.checkStoragePermission {
                it.sendBroadcast(Intent().apply {
                    action = ACTION_SHOW_NOTIFICATION
                    putExtra(BUNDLE_KEY_TRACK_INFO, trackInfo)
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

                        val trackInfo =
                                if (this.extras != null && extras.containsKey(BUNDLE_KEY_TRACK_INFO))
                                    extras.get(BUNDLE_KEY_TRACK_INFO) as TrackInfo
                                else TrackInfo(TrackCoreElement(null, null, null), null)

                        async { showNotification(trackInfo) }
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

    override fun onListenerConnected() {
        super.onListenerConnected()

        if (Build.VERSION.SDK_INT >= 24) {
            activeNotifications.forEach {
                Timber.d("iterated package name: ${it.packageName}")
                if (it.packageName == PACKAGE_NAME_GPM)
                    onUpdate(it.notification)
            }
        }
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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        Timber.d("SBN package name: ${sbn?.packageName}")
        if (sbn == null) return

        if (sbn.packageName == PACKAGE_NAME_GPM)
            onUpdate(sbn.notification)
    }

    private fun onUpdate(notification: Notification) {
        async {
            val track: String = notification.extras.getString(Notification.EXTRA_TITLE)
            val artist: String = notification.extras.getString(Notification.EXTRA_TEXT)
            val album: String = notification.extras.getString(Notification.EXTRA_INFO_TEXT)
            val artworkUri: Uri = notification.extras.getParcelable(Notification.EXTRA_BACKGROUND_IMAGE_URI)
            Timber.d("track: $track, artist: $artist, album: $album, artworkUri: $artworkUri")

            val info = TrackInfo(TrackCoreElement(track, artist, album), artworkUri.toString())

            updateSharedPreference(info)
            updateWidget(trackCoreElement)
            updateNotification(info)
        }
    }

    private fun updateSharedPreference(trackInfo: TrackInfo) {
        sharedPreferences.refreshCurrentTrackInfo(trackInfo)
    }

    private fun updateNotification(trackInfo: TrackInfo) =
            showNotification(trackInfo)

    private fun updateWidget(trackCoreElement: TrackCoreElement) =
            AppWidgetManager.getInstance(this).apply {
                val ids = getAppWidgetIds(ComponentName(this@GPMNotificationListenerService, ShareWidgetProvider::class.java))

                updateAppWidget(
                        ids,
                        RemoteViews(this@GPMNotificationListenerService.packageName, R.layout.widget_share).apply {
                            val summary =
                                    if (trackCoreElement.isAllNonNull) {
                                        if (trackCoreElement.isAllNonNull) {
                                            sharedPreferences.getFormatPattern(this@GPMNotificationListenerService)
                                                    .getSharingText(trackCoreElement)
                                        } else {
                                            sharedPreferences.getSharingText(this@GPMNotificationListenerService)
                                        }
                                    } else null

                            setTextViewText(R.id.widget_summary_share,
                                    summary
                                            ?: this@GPMNotificationListenerService.getString(R.string.dialog_message_alert_no_metadata))

                            setOnClickPendingIntent(R.id.widget_share_root,
                                    ShareWidgetProvider.getPendingIntent(this@GPMNotificationListenerService,
                                            ShareWidgetProvider.Action.SHARE))
                            setOnClickPendingIntent(R.id.widget_button_setting,
                                    ShareWidgetProvider.getPendingIntent(this@GPMNotificationListenerService,
                                            ShareWidgetProvider.Action.OPEN_SETTING))
                        }
                )
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

    private fun showNotification(trackInfo: TrackInfo) {
        if (trackInfo.coreElement.isAllNonNull) {
            checkStoragePermission {
                ui(jobs) {
                    val albumArt =
                            async {
                                when {
                                    trackInfo.artworkUriString != null -> {
                                        Glide.with(this@GPMNotificationListenerService)
                                                .asBitmap()
                                                .load(Uri.parse(trackInfo.artworkUriString))
                                                .submit()
                                                .get()
                                    }

                                    trackInfo.coreElement == this@GPMNotificationListenerService.trackCoreElement -> notificationBitmap

                                    else -> AsyncUtil.getArtworkBitmap(
                                            this@GPMNotificationListenerService,
                                            lastFmApiClient,
                                            trackCoreElement)
                                }
                            }.await()

                    notificationBitmap = albumArt
                    this@GPMNotificationListenerService.trackCoreElement = trackInfo.coreElement

                    getNotification(albumArt, trackInfo.coreElement)?.apply {
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
                            this@GPMNotificationListenerService,
                            0,
                            SettingsActivity.getIntent(this@GPMNotificationListenerService),
                            PendingIntent.FLAG_CANCEL_CURRENT
                    ).let {
                        Notification.Action.Builder(
                                Icon.createWithResource(this@GPMNotificationListenerService,
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

            val artworkUri =
                    if (sharedPreferences.getWhetherBundleArtwork())
                        AsyncUtil.getArtworkUri(this@GPMNotificationListenerService, LastFmApiClient(), trackCoreElement)
                    else null

            setSmallIcon(R.drawable.ic_notification)
            setLargeIcon(thumb)
            setContentTitle(getString(R.string.notification_title))
            setContentText(notificationText)
            setContentIntent(
                    PendingIntent.getActivity(
                            this@GPMNotificationListenerService,
                            0,
                            SharingActivity.getIntent(
                                    this@GPMNotificationListenerService,
                                    sharedPreferences.getFormatPattern(this@GPMNotificationListenerService)
                                            .getSharingText(trackCoreElement),
                                    artworkUri),
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
                    when (SettingsActivity.paletteArray[sharedPreferences.getChoseColorIndex()]) {
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