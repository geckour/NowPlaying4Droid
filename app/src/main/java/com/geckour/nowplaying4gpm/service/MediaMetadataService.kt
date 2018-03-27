package com.geckour.nowplaying4gpm.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.IBinder
import android.preference.PreferenceManager
import android.widget.RemoteViews
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.util.*
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class MediaMetadataService : Service() {

    companion object {
        private const val ACTION_GPM_PLAY_STATE_CHANGED: String = "com.android.music.playstatechanged"
        private const val EXTRA_GPM_ARTIST: String = "artist"
        private const val EXTRA_GPM_ALBUM: String = "album"
        private const val EXTRA_GPM_TRACK: String = "track"
        private const val EXTRA_GPM_PLAYING: String = "playing"

        fun getIntent(context: Context): Intent =
                Intent(context, MediaMetadataService::class.java)
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private val jobs: ArrayList<Job> = ArrayList()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                when (action) {
                    ACTION_GPM_PLAY_STATE_CHANGED -> {
                        if (hasExtra(EXTRA_GPM_PLAYING).not()
                                || getBooleanExtra(EXTRA_GPM_PLAYING, true)) {
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

                            onUpdate(TrackCoreElement(title, artist, album))
                        } else onUpdate(TrackCoreElement(null, null, null))
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG.not()) Fabric.with(this, Crashlytics())

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_GPM_PLAY_STATE_CHANGED)
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
        jobs.cancelAll()
    }

    private fun updateSharedPreference(trackCoreElement: TrackCoreElement) {
        sharedPreferences.refreshCurrentTrackCoreElement(trackCoreElement)
    }

    private fun onUpdate(trackCoreElement: TrackCoreElement) {
        async {
            updateSharedPreference(trackCoreElement)
            updateWidget(trackCoreElement)
            updateNotification(trackCoreElement)
        }
    }

    private fun updateNotification(trackCoreElement: TrackCoreElement) =
            NotificationService.sendNotification(this, trackCoreElement)

    private fun updateWidget(trackCoreElement: TrackCoreElement) =
            AppWidgetManager.getInstance(this).apply {
                val ids = getAppWidgetIds(ComponentName(applicationContext, ShareWidgetProvider::class.java))

                updateAppWidget(
                        ids,
                        RemoteViews(this@MediaMetadataService.packageName, R.layout.widget_share).apply {
                            val summary =
                                    if (trackCoreElement.isAllNonNull) {
                                        if (trackCoreElement.isAllNonNull) {
                                            sharedPreferences.getFormatPattern(this@MediaMetadataService)
                                                    .getSharingText(trackCoreElement)
                                        } else {
                                            sharedPreferences.getSharingText(this@MediaMetadataService)
                                        }
                                    } else null

                            setTextViewText(R.id.widget_summary_share,
                                    summary
                                            ?: this@MediaMetadataService.getString(R.string.dialog_message_alert_no_metadata))

                            setOnClickPendingIntent(R.id.widget_share_root,
                                    ShareWidgetProvider.getPendingIntent(this@MediaMetadataService,
                                            ShareWidgetProvider.Action.SHARE))
                            setOnClickPendingIntent(R.id.widget_button_setting,
                                    ShareWidgetProvider.getPendingIntent(this@MediaMetadataService,
                                            ShareWidgetProvider.Action.OPEN_SETTING))
                        }
                )
            }
}