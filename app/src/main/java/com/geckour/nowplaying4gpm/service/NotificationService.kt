package com.geckour.nowplaying4gpm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.util.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import timber.log.Timber

class NotificationService : NotificationListenerService() {

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
                                    extras.get(BUNDLE_KEY_TRACK_INFO) as? TrackInfo? ?: TrackInfo.empty
                                else TrackInfo.empty

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

    private var currentTrack: TrackCoreElement = TrackCoreElement.empty
    private var resetCurrentTrackJob: Job? = null

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

    override fun onListenerConnected() {
        super.onListenerConnected()

        activeNotifications.forEach {
            onNotificationPosted(it)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if (sbn.packageName == PACKAGE_NAME_GPM) {
            val coreElement = getTrackCoreElement(sbn.notification)
            val notificationBitmap = (sbn.notification.getLargeIcon()?.loadDrawable(this@NotificationService) as? BitmapDrawable?)?.bitmap
            if (currentTrack != coreElement && notificationBitmap != null) {
                resetCurrentTrackJob?.cancel()
                currentTrack = coreElement
                onUpdate(sbn.notification)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        if (sbn.packageName == PACKAGE_NAME_GPM) {
            onDestroyNotification()
            resetCurrentTrackJob =
                    async {
                        delay(250)
                        currentTrack = TrackCoreElement.empty
                    }
        }
    }

    private fun getTrackCoreElement(notification: Notification): TrackCoreElement =
            notification.extras.let {
                val track: String? = if (it.containsKey(Notification.EXTRA_TITLE)) it.getString(Notification.EXTRA_TITLE) else null
                val artist: String? = if (it.containsKey(Notification.EXTRA_TEXT)) it.getString(Notification.EXTRA_TEXT) else null
                val album: String? = if (it.containsKey(Notification.EXTRA_INFO_TEXT)) it.getString(Notification.EXTRA_SUB_TEXT) else null
                TrackCoreElement(track, artist, album)
            }

    private fun onUpdate(notification: Notification) {
        var artworkUri: Uri? = null
        async(onError = {
            sharedPreferences.refreshTempArtwork(artworkUri)
        }) {
            val coreElement = getTrackCoreElement(notification)
            artworkUri = getArtworkUri(notification, coreElement)
            val info = TrackInfo(coreElement, artworkUri?.toString())

            updateSharedPreference(info)
            updateWidget(info)
            updateNotification(info)
        }.invokeOnCompletion {
            it?.apply { Timber.e(this) }
            sharedPreferences.refreshTempArtwork(artworkUri)
        }
    }

    private fun updateSharedPreference(trackInfo: TrackInfo) {
        sharedPreferences.refreshCurrentTrackInfo(trackInfo)
    }

    private fun updateNotification(trackInfo: TrackInfo) =
            showNotification(trackInfo)

    private suspend fun updateWidget(trackInfo: TrackInfo) =
            AppWidgetManager.getInstance(this).apply {
                val ids = getAppWidgetIds(ComponentName(this@NotificationService, ShareWidgetProvider::class.java))

                ids.forEach {
                    updateAppWidget(
                            it,
                            getShareWidgetViews(this@NotificationService, it, trackInfo.coreElement, trackInfo.artworkUriString?.getUri())
                    )
                }
            }

    private suspend fun getArtworkUri(notification: Notification, coreElement: TrackCoreElement): Uri? {
        var artworkUri =
                getArtworkUriFromDevice(this@NotificationService, coreElement)?.apply {
                    sharedPreferences.refreshTempArtwork(this)
                }

        if (artworkUri == null) {
            val notificationBitmap = (notification.getLargeIcon()?.loadDrawable(this@NotificationService) as? BitmapDrawable?)?.bitmap

            if (notificationBitmap != null) {
                val placeholderBitmap = (getDrawable(R.mipmap.bg_default_album_art) as BitmapDrawable).bitmap
                artworkUri =
                        if (notificationBitmap.similarity(placeholderBitmap) > 0.9 && sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_USE_API)) {
                            getArtworkUriFromLastFmApi(this@NotificationService, lastFmApiClient, coreElement)
                        } else refreshArtworkUriFromBitmap(this, notificationBitmap)
            }
        }

        return artworkUri
    }

    private fun onDestroyNotification() {
        val info = TrackInfo.empty
        updateSharedPreference(info)
        async { updateWidget(info) }
        destroyNotification()
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
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_RESIDE) && trackInfo.coreElement.isAllNonNull) {
            checkStoragePermission {
                ui(jobs) {
                    getNotification(this@NotificationService, trackInfo)?.apply {
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

    private fun getDummyNotification(): Notification =
            (if (Build.VERSION.SDK_INT >= 26)
                Notification.Builder(this, Channel.NOTIFICATION_CHANNEL_SHARE.name)
            else Notification.Builder(this)).apply {
                setSmallIcon(R.drawable.ic_notification)
                setContentTitle(getString(R.string.notification_title))
                setContentText(getString(R.string.notification_text_dummy))
            }.build()
}