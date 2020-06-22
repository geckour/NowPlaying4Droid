package com.geckour.nowplaying4gpm.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.OkHttpProvider
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.domain.model.SpotifySearchResult
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.util.ArtworkResolveMethod
import com.geckour.nowplaying4gpm.util.FormatPattern
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.Visibility
import com.geckour.nowplaying4gpm.util.checkStoragePermission
import com.geckour.nowplaying4gpm.util.checkStoragePermissionAsync
import com.geckour.nowplaying4gpm.util.containsPattern
import com.geckour.nowplaying4gpm.util.executeCatching
import com.geckour.nowplaying4gpm.util.getAppName
import com.geckour.nowplaying4gpm.util.getArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.getArtworkUriFromDevice
import com.geckour.nowplaying4gpm.util.getBitmapFromUriString
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getDebugSpotifySearchFlag
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.getMastodonUserInfo
import com.geckour.nowplaying4gpm.util.getNotification
import com.geckour.nowplaying4gpm.util.getPackageStateListPostMastodon
import com.geckour.nowplaying4gpm.util.getReceivedDelegateShareNodeId
import com.geckour.nowplaying4gpm.util.getShareWidgetViews
import com.geckour.nowplaying4gpm.util.getSharingText
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getTempArtworkUri
import com.geckour.nowplaying4gpm.util.getTwitterAccessToken
import com.geckour.nowplaying4gpm.util.getUri
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import com.geckour.nowplaying4gpm.util.readyForShare
import com.geckour.nowplaying4gpm.util.refreshArtworkUri
import com.geckour.nowplaying4gpm.util.refreshArtworkUriFromLastFmApi
import com.geckour.nowplaying4gpm.util.refreshArtworkUriFromSpotify
import com.geckour.nowplaying4gpm.util.refreshCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.refreshTempArtwork
import com.geckour.nowplaying4gpm.util.setAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.setCrashlytics
import com.geckour.nowplaying4gpm.util.setReceivedDelegateShareNodeId
import com.geckour.nowplaying4gpm.util.storePackageStatePostMastodon
import com.geckour.nowplaying4gpm.util.toByteArray
import com.geckour.nowplaying4gpm.util.withCatching
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Media
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class NotificationService : NotificationListenerService(), CoroutineScope {

    enum class Channel {
        NOTIFICATION_CHANNEL_SHARE, NOTIFICATION_CHANNEL_NOTIFY
    }

    enum class NotificationType(val id: Int, val channel: Channel) {
        SHARE(180, Channel.NOTIFICATION_CHANNEL_SHARE),
        NOTIFY_SUCCESS_MASTODON(190, Channel.NOTIFICATION_CHANNEL_NOTIFY),
        DEBUG_SPOTIFY_SEARCH_RESULT(191, Channel.NOTIFICATION_CHANNEL_NOTIFY)
    }

    companion object {
        private const val ACTION_CLEAR_TRACK_INFO = "com.geckour.nowplaying4gpm.cleartrackinfo"
        const val ACTION_DESTROY_NOTIFICATION = "com.geckour.nowplaying4gpm.destroynotification"
        const val ACTION_INVOKE_UPDATE = "com.geckour.nowplaying4gpm.invokeupdate"
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

        fun sendRequestInvokeUpdate(context: Context) {
            context.checkStoragePermission { it.sendBroadcast(Intent(ACTION_INVOKE_UPDATE)) }
        }

        fun getClearTrackInfoPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                Intent(ACTION_CLEAR_TRACK_INFO),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        fun getComponentName(context: Context) =
            ComponentName(context.applicationContext, NotificationService::class.java)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                when (action) {
                    ACTION_CLEAR_TRACK_INFO -> {
                        onMetadataCleared()
                    }

                    ACTION_DESTROY_NOTIFICATION -> {
                        notificationManager.destroyNotification()
                    }

                    ACTION_INVOKE_UPDATE -> {
                        if (context == null) return

                        val trackInfo = sharedPreferences.getCurrentTrackInfo()
                        launch { reflectTrackInfo(trackInfo) }
                    }

                    Intent.ACTION_USER_PRESENT -> {
                        val nodeId = sharedPreferences.getReceivedDelegateShareNodeId() ?: return
                        onRequestDelegateShareFromWear(nodeId, true)
                        sharedPreferences.setReceivedDelegateShareNodeId(null)
                    }
                }
            }
        }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private val notificationManager: NotificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }
    private val keyguardManager: KeyguardManager? by lazy {
        withCatching { getSystemService(KeyguardManager::class.java) }
    }
    private val lastFmApiClient: LastFmApiClient = LastFmApiClient()
    private val spotifyApiClient: SpotifyApiClient by lazy { SpotifyApiClient(this) }
    private val twitterApiClient: TwitterApiClient by lazy {
        TwitterApiClient(BuildConfig.TWITTER_CONSUMER_KEY, BuildConfig.TWITTER_CONSUMER_SECRET)
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    private var refreshMetadataJob: Job? = null
    private var currentTrackClearJob: Job? = null

    private var currentSbn: StatusBarNotification? = null
    private var currentMetadata: MediaMetadata? = null

    private val onMessageReceived: (MessageEvent) -> Unit = {
        when (it.path) {
            WEAR_PATH_TRACK_INFO_GET -> onPulledFromWear()

            WEAR_PATH_POST_TWITTER -> {
                if (it.sourceNodeId != null) launch { onRequestPostToTwitterFromWear(it.sourceNodeId) }
            }

            WEAR_PATH_SHARE_DELEGATE -> {
                if (it.sourceNodeId != null) onRequestDelegateShareFromWear(it.sourceNodeId)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        job = Job()
        setCrashlytics()

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_CLEAR_TRACK_INFO)
            addAction(ACTION_DESTROY_NOTIFICATION)
            addAction(ACTION_INVOKE_UPDATE)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        withCatching { unregisterReceiver(receiver) }

        notificationManager.destroyNotification()
        job.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        if (currentSbn == null) {
            withCatching {
                activeNotifications.sortedByDescending { it.postTime }
                    .firstOrNull { it.notification.mediaMetadata != null }
                    .apply { onNotificationPosted(this) }
            }
        }

        Wearable.getMessageClient(this).addListener(onMessageReceived)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        Wearable.getMessageClient(this).removeListener(onMessageReceived)

        requestRebind()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        if (sbn.packageName != packageName) {
            (sbn.notification.mediaMetadata ?: digMetadata(sbn.packageName))?.let {
                currentSbn = sbn
                sharedPreferences.storePackageStatePostMastodon(sbn.packageName)
                onMetadataChanged(it, sbn.packageName, sbn.notification)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn ?: return

        if (sbn.packageName == currentSbn?.packageName && sbn.id == currentSbn?.id) onMetadataCleared()
    }

    private fun requestRebind() {
        requestRebind(getComponentName(applicationContext))
    }

    private val Notification.mediaController: MediaController?
        get() = extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
            ?.let { MediaController(this@NotificationService, it) }

    private val Notification.mediaMetadata: MediaMetadata? get() = mediaController?.metadata

    private fun digMetadata(playerPackageName: String): MediaMetadata? =
        getSystemService(MediaSessionManager::class.java)
            ?.getActiveSessions(getComponentName(applicationContext))
            ?.firstOrNull { it.packageName == playerPackageName }
            ?.metadata

    private fun onMetadataCleared() {
        currentSbn = null
        currentMetadata = null

        refreshMetadataJob?.cancel()
        currentTrackClearJob?.cancel()

        sharedPreferences.refreshTempArtwork(null)

        currentTrackClearJob = launch { reflectTrackInfo(null) }

        notificationManager.destroyNotification()
    }

    private fun onMetadataChanged(
        metadata: MediaMetadata, playerPackageName: String, notification: Notification? = null
    ) {
        val trackCoreElement = metadata.getTrackCoreElement()
        if (trackCoreElement != currentMetadata?.getTrackCoreElement()) {
            refreshMetadataJob?.cancel()
            currentMetadata = metadata
            refreshMetadataJob = launch {
                currentTrackClearJob?.cancelAndJoin()

                val trackInfo = updateTrackInfo(
                    metadata, playerPackageName, notification, trackCoreElement
                ) ?: return@launch
                val allowedPostMastodon = sharedPreferences.getPackageStateListPostMastodon()
                    .firstOrNull { it.packageName == playerPackageName }
                    ?.state == true
                if (allowedPostMastodon) {
                    postMastodon(trackInfo)
                }
            }
        }
    }

    private suspend fun updateTrackInfo(
        metadata: MediaMetadata,
        playerPackageName: String,
        notification: Notification?,
        coreElement: TrackInfo.TrackCoreElement = metadata.getTrackCoreElement()
    ): TrackInfo? {
        if (onQuickUpdate(coreElement, playerPackageName).not()) {
            onMetadataCleared()
            return null
        }

        val containsSpotifyPattern = sharedPreferences.getFormatPattern(this@NotificationService)
            .containsPattern(FormatPattern.SPOTIFY_URL)
        FirebaseAnalytics.getInstance(application)
            .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
                putString(
                    FirebaseAnalytics.Param.ITEM_NAME,
                    if (containsSpotifyPattern) "ON: Spotify URL trying"
                    else "OFF: Spotify URL trying"
                )
            })
        val spotifyData =
            if (containsSpotifyPattern) {
                spotifyApiClient.getSpotifyData(coreElement).also {
                    notificationManager.showDebugSpotifySearchNotificationIfNeeded(it)
                }
            } else null

        val artworkUri = metadata.storeArtworkUri(
            coreElement, notification?.getArtworkBitmap()
        )

        val trackInfo = TrackInfo(
            coreElement,
            artworkUri?.toString(),
            playerPackageName,
            playerPackageName.getAppName(this),
            (spotifyData as? SpotifySearchResult.Success)?.data?.sharingUrl
        )

        reflectTrackInfo(trackInfo)

        return trackInfo
    }

    private fun MediaMetadata.getTrackCoreElement(): TrackInfo.TrackCoreElement = this.let {
        val track: String? =
            if (it.containsKey(MediaMetadata.METADATA_KEY_TITLE)) it.getString(MediaMetadata.METADATA_KEY_TITLE)
            else null
        val artist: String? = when {
            it.containsKey(MediaMetadata.METADATA_KEY_ARTIST) -> it.getString(MediaMetadata.METADATA_KEY_ARTIST)
            it.containsKey(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) -> it.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            else -> null
        }
        val album: String? =
            if (it.containsKey(MediaMetadata.METADATA_KEY_ALBUM)) it.getString(MediaMetadata.METADATA_KEY_ALBUM)
            else null
        val composer: String? =
            if (it.containsKey(MediaMetadata.METADATA_KEY_COMPOSER)) it.getString(MediaMetadata.METADATA_KEY_COMPOSER)
            else null

        TrackInfo.TrackCoreElement(track, artist, album, composer)
    }

    private suspend fun onQuickUpdate(
        coreElement: TrackInfo.TrackCoreElement, packageName: String
    ): Boolean {
        sharedPreferences.refreshTempArtwork(null)
        val trackInfo = TrackInfo(
            coreElement, null, packageName, packageName.getAppName(this), null
        )

        if (sharedPreferences.readyForShare(this, trackInfo).not()) {
            return false
        }

        reflectTrackInfo(trackInfo, false)

        return true
    }

    private suspend fun reflectTrackInfo(info: TrackInfo?, withArtwork: Boolean = true) {
        updateSharedPreference(info)
        updateWidget(info)
        updateNotification(info)
        if (withArtwork) {
            updateWear(info)
        }
    }

    private fun updateSharedPreference(trackInfo: TrackInfo?) {
        sharedPreferences.refreshCurrentTrackInfo(trackInfo)
    }

    private suspend fun updateNotification(trackInfo: TrackInfo?) {
        notificationManager.showNotification(trackInfo)
    }

    private suspend fun updateWidget(trackInfo: TrackInfo?) {
        AppWidgetManager.getInstance(this).apply {
            val ids = getAppWidgetIds(
                ComponentName(this@NotificationService, ShareWidgetProvider::class.java)
            )

            ids.forEach { id ->
                val widgetOptions = getAppWidgetOptions(id)
                withContext(Dispatchers.Main) {
                    updateAppWidget(
                        id, getShareWidgetViews(
                            this@NotificationService,
                            ShareWidgetProvider.blockCount(widgetOptions),
                            trackInfo
                        )
                    )
                }
            }
        }
    }

    private fun updateWear(trackInfo: TrackInfo?) {
        val subject = sharedPreferences.getFormatPattern(this@NotificationService)
            .getSharingText(trackInfo, sharedPreferences.getFormatPatternModifiers())
        val artwork = trackInfo?.artworkUriString?.getUri()

        Wearable.getDataClient(this@NotificationService).putDataItem(
            PutDataMapRequest.create(WEAR_PATH_TRACK_INFO_POST).apply {
                dataMap.apply {
                    putString(WEAR_KEY_SUBJECT, subject)
                    if (artwork != null) {
                        putAsset(
                            WEAR_KEY_ARTWORK, Asset.createFromUri(artwork)
                        )
                    }
                }
            }.asPutDataRequest()
        )
    }

    private suspend fun postMastodon(trackInfo: TrackInfo) {
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON)) {
            delay(sharedPreferences.getDelayDurationPostMastodon())

            val subject =
                sharedPreferences.getSharingText(this@NotificationService, trackInfo) ?: return

            FirebaseAnalytics.getInstance(application)
                .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked auto post")
                })

            val artworkBytes = if (sharedPreferences.getSwitchState(
                    PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK
                )
            ) {
                trackInfo.artworkUriString?.let {
                    return@let withCatching { getBitmapFromUriString(it)?.toByteArray() }
                }
            } else null

            val userInfo = sharedPreferences.getMastodonUserInfo() ?: return

            val mastodonClient = MastodonClient.Builder(
                userInfo.instanceName, OkHttpProvider.clientBuilder, Gson()
            ).accessToken(userInfo.accessToken.accessToken).build()

            val mediaId = artworkBytes?.let {
                Media(mastodonClient).postMedia(
                    MultipartBody.Part.createFormData(
                        "file", "artwork.png", RequestBody.create(MediaType.get("image/png"), it)
                    )
                ).executeCatching()?.id
            }
            val result = Statuses(mastodonClient).postStatus(subject,
                null,
                mediaId?.let { listOf(it) },
                false,
                null,
                sharedPreferences.getVisibilityMastodon().let {
                    when (it) {
                        Visibility.PUBLIC -> Status.Visibility.Public
                        Visibility.UNLISTED -> Status.Visibility.Unlisted
                        Visibility.PRIVATE -> Status.Visibility.Private
                    }
                }).executeCatching() ?: return

            showShortNotify(result)
        }
    }

    private suspend fun showShortNotify(status: Status) {
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON)) {
            notificationManager.apply {
                showNotification(status)
                delay(2500)
                cancel(NotificationType.NOTIFY_SUCCESS_MASTODON.id)
            }
        }
    }

    private fun deleteWearTrackInfo(onComplete: () -> Unit = {}) {
        Wearable.getDataClient(this).deleteDataItems(Uri.parse("wear://$WEAR_PATH_TRACK_INFO_POST"))
            .addOnSuccessListener { onComplete() }.addOnFailureListener {
                Timber.e(it)
                onComplete()
            }.addOnCompleteListener { onComplete() }
    }

    private fun onPulledFromWear() {
        val trackInfo = sharedPreferences.getCurrentTrackInfo()
        if (trackInfo != null) {
            deleteWearTrackInfo {
                launch { updateWear(trackInfo) }
            }
        }
    }

    private fun onRequestDelegateShareFromWear(
        sourceNodeId: String, invokeOnReleasedLock: Boolean = false
    ) {
        if (keyguardManager?.isDeviceLocked?.not() == true) {
            startActivity(SharingActivity.getIntent(this@NotificationService).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            })
        } else {
            sharedPreferences.setReceivedDelegateShareNodeId(sourceNodeId)
        }

        if (invokeOnReleasedLock.not()) Wearable.getMessageClient(this@NotificationService)
            .sendMessage(
                sourceNodeId, WEAR_PATH_SHARE_SUCCESS, null
            )
    }

    private suspend fun onRequestPostToTwitterFromWear(sourceNodeId: String) {
        FirebaseAnalytics.getInstance(application)
            .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
                putString(
                    FirebaseAnalytics.Param.ITEM_NAME, "Invoked direct share to twitter"
                )
            })

        val trackInfo = sharedPreferences.getCurrentTrackInfo()

        val subject = sharedPreferences.getSharingText(this, trackInfo) ?: run {
            onFailureShareToTwitter(sourceNodeId)
            return
        }

        requireNotNull(trackInfo)

        val artwork = trackInfo.artworkUriString?.let {
            getBitmapFromUriString(it)
        }

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

    private suspend fun MediaMetadata.storeArtworkUri(
        coreElement: TrackInfo.TrackCoreElement, notificationBitmap: Bitmap?
    ): Uri? {
        // Check whether arg metadata and current metadata are the same or not
        val cacheInfo = sharedPreferences.getCurrentTrackInfo()
        if (coreElement.isAllNonNull && cacheInfo?.artworkUriString != null && coreElement == cacheInfo.coreElement) {
            return sharedPreferences.getTempArtworkUri(this@NotificationService)
        }

        sharedPreferences.getArtworkResolveOrder().filter { it.enabled }.forEach { method ->
            Timber.d("np4d artwork resolve method: $method")
            when (method.key) {
                ArtworkResolveMethod.ArtworkResolveMethodKey.CONTENT_RESOLVER -> {
                    this@NotificationService.getArtworkUriFromDevice(coreElement)?.apply {
                        sharedPreferences.refreshTempArtwork(this)
                        return this
                    }
                }
                ArtworkResolveMethod.ArtworkResolveMethodKey.MEDIA_METADATA_URI -> {
                    this.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)?.let { uri ->
                        getBitmapFromUriString(uri)?.refreshArtworkUri(this@NotificationService)
                            ?.let { return it }
                    }
                }
                ArtworkResolveMethod.ArtworkResolveMethodKey.MEDIA_METADATA_BITMAP -> {
                    this.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?.let { bitmap ->
                        bitmap.refreshArtworkUri(this@NotificationService)
                            ?.let { return it }
                    }
                }
                ArtworkResolveMethod.ArtworkResolveMethodKey.NOTIFICATION_BITMAP -> {
                    notificationBitmap?.refreshArtworkUri(this@NotificationService)
                        ?.let { return it }
                }
                ArtworkResolveMethod.ArtworkResolveMethodKey.LAST_FM -> {
                    refreshArtworkUriFromLastFmApi(
                        this@NotificationService, lastFmApiClient, coreElement
                    )?.let { return it }
                }
                ArtworkResolveMethod.ArtworkResolveMethodKey.SPOTIFY -> {
                    refreshArtworkUriFromSpotify(
                        this@NotificationService, spotifyApiClient, coreElement
                    )?.let { return it }
                }
            }
        }

        return null
    }

    private fun Notification.getArtworkBitmap(): Bitmap? =
        (getLargeIcon()?.loadDrawable(this@NotificationService) as? BitmapDrawable)?.bitmap?.let {
            withCatching { it.copy(it.config, false) }
        }

    private suspend fun NotificationManager.showNotification(trackInfo: TrackInfo?) {
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_RESIDE) && sharedPreferences.readyForShare(
                this@NotificationService, trackInfo
            )
        ) {
            checkStoragePermissionAsync {
                getNotification(this@NotificationService, trackInfo)
                    ?.apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForeground(NotificationType.SHARE.id, this)
                        } else {
                            notify(NotificationType.SHARE.id, this)
                        }
                    }
            }
        } else this.destroyNotification()
    }

    private suspend fun NotificationManager.showNotification(status: Status) {
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON)) {
            checkStoragePermissionAsync {
                getNotification(this@NotificationService, status)?.apply {
                    notify(NotificationType.NOTIFY_SUCCESS_MASTODON.id, this)
                }
            }
        }
    }

    private fun NotificationManager.destroyNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        } else {
            this.cancel(NotificationType.SHARE.id)
        }
        this.cancel(NotificationType.NOTIFY_SUCCESS_MASTODON.id)
        this.cancel(NotificationType.DEBUG_SPOTIFY_SEARCH_RESULT.id)
    }

    private fun NotificationManager.showDebugSpotifySearchNotificationIfNeeded(
        spotifySearchResult: SpotifySearchResult
    ) {
        if (sharedPreferences.getDebugSpotifySearchFlag()) {
            val notification =
                getNotification(this@NotificationService, spotifySearchResult)
            notify(NotificationType.DEBUG_SPOTIFY_SEARCH_RESULT.id, notification)
        }
    }
}