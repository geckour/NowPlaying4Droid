package com.geckour.nowplaying4gpm.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.Html
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.OkHttpProvider
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.domain.model.SpotifyResult
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
import com.geckour.nowplaying4gpm.util.foldBreaks
import com.geckour.nowplaying4gpm.util.getAppName
import com.geckour.nowplaying4gpm.util.getArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.getArtworkUriFromDevice
import com.geckour.nowplaying4gpm.util.getBitmapFromUriString
import com.geckour.nowplaying4gpm.util.getClearTrackInfoPendingIntent
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.getMastodonUserInfo
import com.geckour.nowplaying4gpm.util.getOptimizedColor
import com.geckour.nowplaying4gpm.util.getPackageStateListPostMastodon
import com.geckour.nowplaying4gpm.util.getPackageStateListSpotify
import com.geckour.nowplaying4gpm.util.getReceivedDelegateShareNodeId
import com.geckour.nowplaying4gpm.util.getSettingsIntent
import com.geckour.nowplaying4gpm.util.getShareIntent
import com.geckour.nowplaying4gpm.util.getSharingText
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getTempArtworkUri
import com.geckour.nowplaying4gpm.util.getTwitterAccessToken
import com.geckour.nowplaying4gpm.util.getUri
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import com.geckour.nowplaying4gpm.util.readyForShare
import com.geckour.nowplaying4gpm.util.refreshArtworkUri
import com.geckour.nowplaying4gpm.util.refreshArtworkUriFromLastFmApi
import com.geckour.nowplaying4gpm.util.refreshCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.refreshTempArtwork
import com.geckour.nowplaying4gpm.util.setAlertTwitterAuthFlag
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.android.ext.android.get
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
        const val ACTION_CLEAR_TRACK_INFO = "com.geckour.nowplaying4gpm.cleartrackinfo"
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
                        val trackInfo = sharedPreferences.getCurrentTrackInfo() ?: return@apply
                        launch {
                            updateTrackInfo(
                                currentMetadata ?: return@launch,
                                currentSbn?.packageName ?: return@launch,
                                currentSbn?.notification,
                                trackInfo.coreElement
                            )
                        }
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
    private val lastFmApiClient: LastFmApiClient = get()
    private val spotifyApiClient: SpotifyApiClient = get()
    private val twitterApiClient: TwitterApiClient = get()

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    private var refreshMetadataJob: Job? = null
    private var currentTrackClearJob: Job? = null

    private var currentSbn: StatusBarNotification? = null
    private var currentMetadata: MediaMetadata? = null

    private val onActiveSessionChanged: MediaSessionManager.OnActiveSessionsChangedListener by lazy {
        object : MediaSessionManager.OnActiveSessionsChangedListener {
            val componentName = getComponentName(applicationContext)

            override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
                if (controllers.isNullOrEmpty()) return

                requestRebind(componentName)
            }
        }
    }

    private val onMessageReceived: (MessageEvent) -> Unit = {
        when (it.path) {
            WEAR_PATH_TRACK_INFO_GET -> onPulledFromWear()

            WEAR_PATH_POST_TWITTER -> {
                launch { onRequestPostToTwitterFromWear(it.sourceNodeId) }
            }

            WEAR_PATH_SHARE_DELEGATE -> {
                onRequestDelegateShareFromWear(it.sourceNodeId)
            }
        }
    }

    private val sbnChannel = Channel<StatusBarNotification>(CONFLATED)

    @OptIn(FlowPreview::class)
    private val sbnFlow = sbnChannel.receiveAsFlow().debounce(200)

    override fun onCreate() {
        super.onCreate()

        job = Job()

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_CLEAR_TRACK_INFO)
            addAction(ACTION_DESTROY_NOTIFICATION)
            addAction(ACTION_INVOKE_UPDATE)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, intentFilter)

        sbnFlow.onEach { it.process() }.launchIn(this)
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
        withCatching {
            getSystemService(MediaSessionManager::class.java)
                ?.removeOnActiveSessionsChangedListener(onActiveSessionChanged)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        Wearable.getMessageClient(this).removeListener(onMessageReceived)
        withCatching {
            getSystemService(MediaSessionManager::class.java)
                ?.addOnActiveSessionsChangedListener(
                    onActiveSessionChanged,
                    getComponentName(this)
                )
        }

        requestRebind()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        sbnChannel.trySend(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn ?: return

        if (sbn.packageName == currentSbn?.packageName && sbn.id == currentSbn?.id) onMetadataCleared()
    }

    private fun requestRebind() {
        requestRebind(getComponentName(applicationContext))
    }

    private fun StatusBarNotification.process() {
        checkStoragePermission {
            if (this.packageName != this@NotificationService.packageName) {
                (notification.mediaMetadata ?: digMetadata(this.packageName))?.let {
                    currentSbn = this
                    sharedPreferences.storePackageStatePostMastodon(this.packageName)
                    onMetadataChanged(it, this.packageName, this.notification)
                }
            }
        }
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
        metadata: MediaMetadata,
        playerPackageName: String,
        notification: Notification? = null
    ) {
        if (metadata != currentMetadata) {
            refreshMetadataJob?.cancel()
            currentMetadata = metadata
            refreshMetadataJob = launch {
                currentTrackClearJob?.cancelAndJoin()

                val trackInfo = updateTrackInfo(
                    metadata, playerPackageName, notification, metadata.getTrackCoreElement()
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
        val useSpotifyData =
            sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_USE_SPOTIFY_DATA)
        val useSpotifyDataPackageState =
            sharedPreferences.getPackageStateListSpotify()
                .filter { it.state }
                .map { it.packageName }
        val spotifyResult =
            if (containsSpotifyPattern
                || sharedPreferences.getArtworkResolveOrder()
                    .first { it.key == ArtworkResolveMethod.ArtworkResolveMethodKey.SPOTIFY }
                    .enabled
                || (useSpotifyData && useSpotifyDataPackageState.contains(playerPackageName))
            ) {
                spotifyApiClient.getSpotifyData(coreElement, playerPackageName)
            } else null
        val spotifyData = (spotifyResult as? SpotifyResult.Success)?.data

        val artworkUri = metadata.storeArtworkUri(
            coreElement,
            notification?.getArtworkBitmap(),
            spotifyData
        )

        val trackInfo = TrackInfo(
            if (useSpotifyData && useSpotifyDataPackageState.contains(playerPackageName)) {
                coreElement.withSpotifyData(spotifyData)
            } else coreElement,
            artworkUri?.toString(),
            playerPackageName,
            playerPackageName.getAppName(this),
            spotifyData
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

    private fun TrackInfo.TrackCoreElement.withSpotifyData(data: SpotifyResult.Data?): TrackInfo.TrackCoreElement =
        TrackInfo.TrackCoreElement(
            data?.trackName ?: title,
            data?.artistName ?: artist,
            data?.albumName ?: album,
            composer
        )

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

    private fun updateWidget(trackInfo: TrackInfo?) {
        sendBroadcast(ShareWidgetProvider.getUpdateIntent(this, trackInfo))
    }

    private fun updateWear(trackInfo: TrackInfo?) {
        val subject = sharedPreferences.getFormatPattern(this@NotificationService)
            .getSharingText(trackInfo, sharedPreferences.getFormatPatternModifiers())
        val artwork = trackInfo?.artworkUriString?.getUri()

        Wearable.getDataClient(this@NotificationService).putDataItem(
            PutDataMapRequest.create(WEAR_PATH_TRACK_INFO_POST).apply {
                dataMap.apply {
                    subject?.let { putString(WEAR_KEY_SUBJECT, it) }
                    artwork?.let { putAsset(WEAR_KEY_ARTWORK, Asset.createFromUri(it)) }
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
                    return@let withCatching {
                        getBitmapFromUriString(it)?.toByteArray()
                    }
                }
            } else null

            val userInfo = sharedPreferences.getMastodonUserInfo() ?: return

            val mastodonClient = MastodonClient.Builder(
                userInfo.instanceName, OkHttpProvider.clientBuilder, Gson()
            ).accessToken(userInfo.accessToken.accessToken).build()

            val mediaId = artworkBytes?.let {
                Media(mastodonClient).postMedia(
                    MultipartBody.Part.createFormData(
                        "file",
                        "artwork.png",
                        it.toRequestBody("image/png".toMediaTypeOrNull())
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
        coreElement: TrackInfo.TrackCoreElement,
        notificationBitmap: Bitmap?,
        spotifyData: SpotifyResult.Data?
    ): Uri? {
        // Check whether arg metadata and current metadata are the same or not
        val cacheInfo = sharedPreferences.getCurrentTrackInfo()
        if (coreElement.isAllNonNull && cacheInfo?.artworkUriString != null && coreElement == cacheInfo.coreElement) {
            return sharedPreferences.getTempArtworkUri(this@NotificationService)
        }

        sharedPreferences.getArtworkResolveOrder().filter { it.enabled }.forEach { method ->
            when (method.key) {
                ArtworkResolveMethod.ArtworkResolveMethodKey.CONTENT_RESOLVER -> {
                    this@NotificationService.getArtworkUriFromDevice(coreElement)?.apply {
                        sharedPreferences.refreshTempArtwork(this)
                        return this
                    }
                }
                ArtworkResolveMethod.ArtworkResolveMethodKey.MEDIA_METADATA_URI -> {
                    this.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)?.let { uri ->
                        getBitmapFromUriString(uri)
                            ?.refreshArtworkUri(this@NotificationService)
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
                        this@NotificationService,
                        lastFmApiClient,
                        coreElement
                    )?.let { return it }
                }
                ArtworkResolveMethod.ArtworkResolveMethodKey.SPOTIFY -> {
                    spotifyData?.artworkUrl
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
                trackInfo?.getNotification(this@NotificationService)?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForeground(NotificationType.SHARE.id, it)
                    } else {
                        notify(NotificationType.SHARE.id, it)
                    }
                }
            }
        } else this.destroyNotification()
    }

    private suspend fun NotificationManager.showNotification(status: Status) {
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON)) {
            checkStoragePermissionAsync {
                notify(
                    NotificationType.NOTIFY_SUCCESS_MASTODON.id,
                    status.getNotification(this@NotificationService)
                )
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

    private suspend fun TrackInfo.getNotification(context: Context): Notification {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val notificationBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(
                    context,
                    Channel.NOTIFICATION_CHANNEL_SHARE.name
                )
            else Notification.Builder(context)

        return notificationBuilder.apply {
            val actionOpenSetting = Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_settings),
                context.getString(R.string.action_open_pref),
                getSettingsIntent(context)
            ).build()
            val actionClear = Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_clear),
                context.getString(R.string.action_clear_notification),
                getClearTrackInfoPendingIntent(context)
            ).build()
            val notificationText = sharedPreferences.getFormatPattern(context)
                .getSharingText(this@getNotification, sharedPreferences.getFormatPatternModifiers())
                ?.foldBreaks()

            val thumb = this@getNotification.artworkUriString?.let {
                context.getBitmapFromUriString(it)
            }
            setSmallIcon(R.drawable.ic_notification)
            val showArtworkInNotification = sharedPreferences.getSwitchState(
                PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION
            )
            if (showArtworkInNotification) setLargeIcon(thumb)
            setContentTitle(context.getString(R.string.notification_title))
            setContentText(notificationText)
            setContentIntent(getShareIntent(context))
            setOngoing(true)
            style = Notification.DecoratedMediaCustomViewStyle()
            addAction(actionOpenSetting)
            addAction(actionClear)
            thumb?.apply {
                if (Build.VERSION.SDK_INT >= 26 && sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG
                    )
                ) {
                    setColorized(true)

                    val color = Palette.from(this)
                        .maximumColorCount(12)
                        .generate()
                        .getOptimizedColor(context)
                    setColor(color)
                }
            }
        }.build()
    }

    private suspend fun Status.getNotification(context: Context): Notification {
        val notificationBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(
                    context,
                    Channel.NOTIFICATION_CHANNEL_SHARE.name
                )
            else Notification.Builder(context)

        return notificationBuilder.apply {
            val notificationText =
                Html.fromHtml(this@getNotification.content, Html.FROM_HTML_MODE_COMPACT).toString()

            val thumb = this@getNotification.mediaAttachments
                .firstOrNull()
                ?.url
                ?.let { context.getBitmapFromUriString(it) }

            setSmallIcon(R.drawable.ic_notification_notify)
            setLargeIcon(thumb)
            setContentTitle(context.getString(R.string.notification_title_notify_success_mastodon))
            setContentText(notificationText)
            style = Notification.DecoratedMediaCustomViewStyle()
            thumb?.apply {
                val color = Palette.from(this)
                    .maximumColorCount(24)
                    .generate()
                    .getOptimizedColor(context)
                setColor(color)
            }
        }.build()
    }
}