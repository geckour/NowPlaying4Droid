package com.geckour.nowplaying4gpm.service

import android.app.*
import android.appwidget.AppWidgetManager
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
import android.widget.RemoteViews
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SettingsActivity.Companion.paletteArray
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.domain.model.ArtworkInfo
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.util.*
import com.geckour.nowplaying4gpm.util.AsyncUtil.getArtworkBitmap
import com.geckour.nowplaying4gpm.util.AsyncUtil.getArtworkUri
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class NotifyMediaMetaDataService : NotificationListenerService() {

    enum class Channel(val id: Int) {
        NOTIFICATION_CHANNEL_SHARE(180)
    }

    companion object {
        const val ACTION_DESTROY_NOTIFICATION: String = "com.geckour.nowplaying4gpm.destroynotification"
        const val ACTION_SHOW_NOTIFICATION: String = "com.geckour.nowplaying4gpm.shownotification"
        private const val ACTION_GPM_PLAY_STATE_CHANGED: String = "com.android.music.playstatechanged"
        private const val EXTRA_GPM_ARTIST: String = "artist"
        private const val EXTRA_GPM_ALBUM: String = "album"
        private const val EXTRA_GPM_TRACK: String = "track"
        private const val EXTRA_GPM_PLAYING: String = "playing"

        private fun getIntent(context: Context): Intent =
                Intent(context, NotifyMediaMetaDataService::class.java)

        fun launchService(context: Context?) {
            context?.checkStoragePermission {
                if (Build.VERSION.SDK_INT >= 26)
                    it.startForegroundService(getIntent(it))
                else it.startService(getIntent(it))
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

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                when (action) {
                    ACTION_GPM_PLAY_STATE_CHANGED -> {
                        val title =
                                if (hasExtra(EXTRA_GPM_TRACK))
                                    getStringExtra(EXTRA_GPM_TRACK)
                                else null
                        val artist =
                                if (hasExtra(EXTRA_GPM_ARTIST))
                                    getStringExtra(EXTRA_GPM_ARTIST)
                                else null
                        val album =
                                if (hasExtra(EXTRA_GPM_ALBUM))
                                    getStringExtra(EXTRA_GPM_ALBUM)
                                else null

                        val playStart: Boolean =
                                hasExtra(EXTRA_GPM_PLAYING).not()
                                        || getBooleanExtra(EXTRA_GPM_PLAYING, true)

                        onUpdate(playStart, TrackCoreElement(title, artist, album))
                    }

                    ACTION_DESTROY_NOTIFICATION -> destroyNotification()

                    ACTION_SHOW_NOTIFICATION -> {
                        if (context == null) return

                        val trackCoreElement =
                                sharedPreferences.getCurrentTrackInfo()?.coreElement
                                        ?: TrackCoreElement(null, null, null)

                        onUpdate(trackCoreElement.isComplete, trackCoreElement)
                    }
                }
            }
        }
    }

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
            addAction(ACTION_GPM_PLAY_STATE_CHANGED)
            addAction(ACTION_DESTROY_NOTIFICATION)
            addAction(ACTION_SHOW_NOTIFICATION)
        }

        registerReceiver(receiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }

        updateSharedPreference(TrackCoreElement(null, null, null))
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

    private fun updateSharedPreference(trackCoreElement: TrackCoreElement) {
        sharedPreferences.refreshCurrentTrackCoreElement(trackCoreElement)
    }

    private fun onUpdate(playStart: Boolean, trackCoreElement: TrackCoreElement) {
        ui(jobs) {
            async {
                updateWidget(playStart, trackCoreElement)
                updateNotification(playStart, trackCoreElement)
            }.await()

            updateSharedPreference(trackCoreElement)
        }
    }

    private suspend fun updateNotification(playStart: Boolean, trackCoreElement: TrackCoreElement) {
        sharedPreferences.apply {
            if (playStart && getWhetherReside() && trackCoreElement.isComplete) {
                showNotification(trackCoreElement)
            } else destroyNotification()
        }
    }

    private fun updateWidget(playStart: Boolean, trackCoreElement: TrackCoreElement) =
            AppWidgetManager.getInstance(this).apply {
                val ids = getAppWidgetIds(ComponentName(applicationContext, ShareWidgetProvider::class.java))

                updateAppWidget(
                        ids,
                        RemoteViews(this@NotifyMediaMetaDataService.packageName, R.layout.widget_share).apply {
                            val summary =
                                    if (playStart) {
                                        if (trackCoreElement.isComplete) {
                                            sharedPreferences.getFormatPattern(this@NotifyMediaMetaDataService)
                                                    .getSharingText(trackCoreElement)
                                        } else {
                                            sharedPreferences.getSharingText(this@NotifyMediaMetaDataService)
                                        }
                                    } else null

                            setTextViewText(R.id.widget_summary_share,
                                    summary
                                            ?: this@NotifyMediaMetaDataService.getString(R.string.dialog_message_alert_no_metadata))

                            setOnClickPendingIntent(R.id.widget_share_root,
                                    ShareWidgetProvider.getPendingIntent(this@NotifyMediaMetaDataService,
                                            ShareWidgetProvider.Action.SHARE))
                            setOnClickPendingIntent(R.id.widget_button_setting,
                                    ShareWidgetProvider.getPendingIntent(this@NotifyMediaMetaDataService,
                                            ShareWidgetProvider.Action.OPEN_SETTING))
                        }
                )
            }

    private suspend fun showNotification(trackCoreElement: TrackCoreElement) {
        checkStoragePermission {
            async {
                val albumArt =
                        if (this@NotifyMediaMetaDataService.trackCoreElement == trackCoreElement) {
                            notificationBitmap
                                    ?: getArtworkBitmap(
                                            this@NotifyMediaMetaDataService,
                                            lastFmApiClient,
                                            trackCoreElement)
                        } else getArtworkBitmap(
                                this@NotifyMediaMetaDataService,
                                lastFmApiClient,
                                trackCoreElement)

                notificationBitmap = albumArt
                this@NotifyMediaMetaDataService.trackCoreElement = trackCoreElement

                getNotification(albumArt, trackCoreElement).apply {
                    startForeground(Channel.NOTIFICATION_CHANNEL_SHARE.id, this)
                }
            }
        }
    }

    private fun showDummyNotification() =
            startForeground(Channel.NOTIFICATION_CHANNEL_SHARE.id, getDummyNotification())

    private fun destroyNotification() =
            stopForeground(true)

    private suspend fun getNotification(thumb: Bitmap?, trackCoreElement: TrackCoreElement): Notification? {
        if (trackCoreElement.isComplete.not()) return null

        val notificationBuilder =
                if (Build.VERSION.SDK_INT >= 26)
                    Notification.Builder(this, Channel.NOTIFICATION_CHANNEL_SHARE.name)
                else Notification.Builder(this)

        return notificationBuilder.apply {
            val actionOpenSetting =
                    PendingIntent.getActivity(
                            this@NotifyMediaMetaDataService,
                            0,
                            SettingsActivity.getIntent(this@NotifyMediaMetaDataService),
                            PendingIntent.FLAG_CANCEL_CURRENT
                    ).let {
                        Notification.Action.Builder(
                                Icon.createWithResource(this@NotifyMediaMetaDataService,
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
                    getArtworkUri(this@NotifyMediaMetaDataService,
                            lastFmApiClient,
                            TrackInfo(this@NotifyMediaMetaDataService.trackCoreElement))
            sharedPreferences.setCurrentArtWorkInfo(ArtworkInfo(uri?.toString(), trackCoreElement))

            setSmallIcon(R.drawable.ic_notification)
            setLargeIcon(thumb)
            setContentTitle(getString(R.string.notification_title))
            setContentText(notificationText)
            setContentIntent(
                    PendingIntent.getActivity(
                            this@NotifyMediaMetaDataService,
                            0,
                            SharingActivity.getIntent(this@NotifyMediaMetaDataService,
                                    sharedPreferences.getFormatPattern(this@NotifyMediaMetaDataService)
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