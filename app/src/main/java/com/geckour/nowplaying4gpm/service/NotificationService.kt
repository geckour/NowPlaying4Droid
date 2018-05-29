package com.geckour.nowplaying4gpm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi
import com.geckour.nowplaying4gpm.App.Companion.PACKAGE_NAME_GPM
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.ui.SharingActivity
import com.geckour.nowplaying4gpm.util.*
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import timber.log.Timber
import java.io.ByteArrayOutputStream

class NotificationService : NotificationListenerService() {

    enum class Channel(val id: Int) {
        NOTIFICATION_CHANNEL_SHARE(180)
    }

    companion object {
        const val ACTION_DESTROY_NOTIFICATION = "com.geckour.nowplaying4gpm.destroynotification"
        const val ACTION_SHOW_NOTIFICATION = "com.geckour.nowplaying4gpm.shownotification"
        const val ACTION_NOTIFY_SUCCESS_START_SHARE = "com.geckour.nowplaying4gpm.notifysuccessstartshare"
        const val ACTION_NOTIFY_FAILURE_START_SHARE = "com.geckour.nowplaying4gpm.notifyfailurestartshare"
        private const val BUNDLE_KEY_TRACK_INFO = "bundle_key_track_info"
        private const val WEAR_PATH_TRACK_INFO_POST = "/track_info/post"
        private const val WEAR_PATH_TRACK_INFO_GET = "/track_info/get"
        private const val WEAR_PATH_POST_TWITTER = "/post/twitter"
        private const val WEAR_PATH_POST_SUCCESS = "/post/success"
        private const val WEAR_PATH_POST_FAILURE = "/post/failure"
        private const val WEAR_PATH_SHARE_DELEGATE = "/share/delegate"
        private const val WEAR_PATH_SHARE_SUCCESS = "/share/success"
        private const val WEAR_PATH_SHARE_FAILURE = "/share/failure"
        private const val WEAR_KEY_SUBJECT = "key_subject"
        private const val WEAR_KEY_ARTWORK = "key_artwork"

        fun sendRequestShowNotification(context: Context, trackInfo: TrackInfo?) {
            context.checkStoragePermission {
                it.sendBroadcast(Intent().apply {
                    action = ACTION_SHOW_NOTIFICATION
                    putExtra(BUNDLE_KEY_TRACK_INFO, trackInfo)
                })
            }
        }

        fun sendRequestNotifyShareResult(context: Context, success: Boolean) {
            context.sendBroadcast(Intent().apply {
                action =
                        if (success) ACTION_NOTIFY_SUCCESS_START_SHARE
                        else ACTION_NOTIFY_FAILURE_START_SHARE
            })
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
                                    extras.get(BUNDLE_KEY_TRACK_INFO) as? TrackInfo?
                                            ?: TrackInfo.empty
                                else TrackInfo.empty

                        async { showNotification(trackInfo) }
                    }

                    ACTION_NOTIFY_SUCCESS_START_SHARE -> onSuccessDelegateShare()

                    ACTION_NOTIFY_FAILURE_START_SHARE -> onFailureDelegateShare()
                }
            }
        }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private val lastFmApiClient: LastFmApiClient = LastFmApiClient()
    private val twitterApiClient: TwitterApiClient =
            TwitterApiClient(BuildConfig.TWITTER_CONSUMER_KEY, BuildConfig.TWITTER_CONSUMER_SECRET)
    private val jobs: ArrayList<Job> = ArrayList()

    private var currentTrack: TrackCoreElement = TrackCoreElement.empty
    private var resetCurrentTrackJob: Job? = null

    private val onMessageReceived: (MessageEvent) -> Unit = {
        when (it.path) {
            WEAR_PATH_TRACK_INFO_GET -> onPulledFromWear()

            WEAR_PATH_POST_TWITTER -> if (it.sourceNodeId != null) async { onRequestPostToTwitterFromWear(it.sourceNodeId) }

            WEAR_PATH_SHARE_DELEGATE -> if (it.sourceNodeId != null) onRequestDelegateShareFromWear(it.sourceNodeId)
        }
    }

    private var onSuccessDelegateShare: () -> Unit = {}
    private var onFailureDelegateShare: () -> Unit = {}

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            createDefaultChannel()
        }

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_DESTROY_NOTIFICATION)
            addAction(ACTION_SHOW_NOTIFICATION)
            addAction(ACTION_NOTIFY_SUCCESS_START_SHARE)
            addAction(ACTION_NOTIFY_FAILURE_START_SHARE)
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

        Wearable.getMessageClient(this).addListener(onMessageReceived)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        Wearable.getMessageClient(this).removeListener(onMessageReceived)
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
            reflectTrackInfo(TrackInfo(coreElement, null), false)

            artworkUri = getArtworkUri(notification, coreElement)
            reflectTrackInfo(TrackInfo(coreElement, artworkUri?.toString()))
        }.invokeOnCompletion {
            it?.apply { Timber.e(this) }
            sharedPreferences.refreshTempArtwork(artworkUri)
        }
    }

    private suspend fun reflectTrackInfo(info: TrackInfo, withArtwork: Boolean = true) {
        updateSharedPreference(info)
        updateWidget(info)
        updateNotification(info)
        if (withArtwork) updateWear(info)
    }

    private fun updateSharedPreference(trackInfo: TrackInfo) {
        sharedPreferences.refreshCurrentTrackInfo(trackInfo)
    }

    private fun updateNotification(trackInfo: TrackInfo) {
        showNotification(trackInfo)
    }

    private suspend fun updateWidget(trackInfo: TrackInfo) {
        AppWidgetManager.getInstance(this).apply {
            val ids = getAppWidgetIds(ComponentName(this@NotificationService, ShareWidgetProvider::class.java))

            ids.forEach {
                updateAppWidget(
                        it,
                        getShareWidgetViews(this@NotificationService, it, trackInfo.coreElement, trackInfo.artworkUriString?.getUri())
                )
            }
        }
    }

    private suspend fun updateWear(trackInfo: TrackInfo) {
        val subject =
                if (trackInfo.coreElement.isAllNonNull)
                    sharedPreferences.getFormatPattern(this).getSharingText(trackInfo.coreElement)
                else null
        val artwork = trackInfo.artworkUriString
                ?.getUri()
                ?.let {
                    ByteArrayOutputStream().apply {
                        getBitmapFromUri(this@NotificationService, it)
                                ?.compress(
                                        Bitmap.CompressFormat.JPEG,
                                        100,
                                        this
                                )
                    }.toByteArray()
                }

        Wearable.getDataClient(this)
                .putDataItem(
                        PutDataMapRequest.create(WEAR_PATH_TRACK_INFO_POST)
                                .apply {
                                    dataMap.apply {
                                        putString(WEAR_KEY_SUBJECT, subject)
                                        if (artwork != null) putAsset(WEAR_KEY_ARTWORK, Asset.createFromBytes(artwork))
                                    }
                                }.asPutDataRequest()
                )
    }

    private fun deleteWearTrackInfo(onComplete: () -> Unit = {}) {
        Wearable.getDataClient(this)
                .deleteDataItems(Uri.parse("wear://$WEAR_PATH_TRACK_INFO_POST"))
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener {
                    Timber.e(it)
                    onComplete()
                }
                .addOnCompleteListener { onComplete() }
    }

    private fun onPulledFromWear() {
        val trackInfo = sharedPreferences.getCurrentTrackInfo()
        if (trackInfo != null) {
            deleteWearTrackInfo {
                async { updateWear(trackInfo) }
            }
        }
    }

    private fun onRequestDelegateShareFromWear(sourceNodeId: String) {
        onSuccessDelegateShare = {
            Wearable.getMessageClient(this@NotificationService)
                    .sendMessage(sourceNodeId, WEAR_PATH_SHARE_SUCCESS, null)
            onSuccessDelegateShare = {}
            onFailureDelegateShare = {}
        }

        onFailureDelegateShare = {
            Wearable.getMessageClient(this@NotificationService)
                    .sendMessage(sourceNodeId, WEAR_PATH_SHARE_FAILURE, null)
            onSuccessDelegateShare = {}
            onFailureDelegateShare = {}
        }

        startActivity(SharingActivity.getIntent(this@NotificationService)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    private suspend fun onRequestPostToTwitterFromWear(sourceNodeId: String) {
        FirebaseAnalytics.getInstance(application)
                .logEvent(
                        FirebaseAnalytics.Event.SELECT_CONTENT,
                        Bundle().apply {
                            putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked direct share to twitter")
                        }
                )

        val trackInfo = sharedPreferences.getCurrentTrackInfo() ?: run {
            onFailureShareToTwitter(sourceNodeId)
            return
        }

        val subject = sharedPreferences.getFormatPattern(this)
                .getSharingText(trackInfo.coreElement)
        val artwork =
                if (trackInfo.artworkUriString == null) null
                else getBitmapFromUriString(this@NotificationService, trackInfo.artworkUriString)

        val accessToken = sharedPreferences.getTwitterAccessToken() ?: run {
            sharedPreferences.setAlertTwitterAuthFlag(true)
            onFailureShareToTwitter(sourceNodeId)
            return
        }

        twitterApiClient.post(accessToken, subject, artwork, trackInfo.coreElement.title)

        Wearable.getMessageClient(this@NotificationService)
                .sendMessage(sourceNodeId, WEAR_PATH_POST_SUCCESS, null)
    }

    private fun onFailureShareToTwitter(sourceNodeId: String) {
        Wearable.getMessageClient(this@NotificationService)
                .sendMessage(sourceNodeId, WEAR_PATH_POST_FAILURE, null)
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
        async {
            updateWidget(info)
            updateWear(info)
        }
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

    private fun destroyNotification() {
        if (Build.VERSION.SDK_INT >= 26) stopForeground(true)
        else cancelAllNotifications()
    }
}