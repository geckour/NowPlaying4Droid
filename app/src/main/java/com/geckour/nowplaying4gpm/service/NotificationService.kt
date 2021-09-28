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
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.OkHttpProvider
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.Visibility
import com.geckour.nowplaying4gpm.util.checkStoragePermission
import com.geckour.nowplaying4gpm.util.destroyNotification
import com.geckour.nowplaying4gpm.util.digMediaController
import com.geckour.nowplaying4gpm.util.executeCatching
import com.geckour.nowplaying4gpm.util.getBitmapFromUriString
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getMastodonUserInfo
import com.geckour.nowplaying4gpm.util.getPackageStateListPostMastodon
import com.geckour.nowplaying4gpm.util.getReceivedDelegateShareNodeId
import com.geckour.nowplaying4gpm.util.getSharingText
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getTrackCoreElement
import com.geckour.nowplaying4gpm.util.getTwitterAccessToken
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import com.geckour.nowplaying4gpm.util.reflectTrackInfo
import com.geckour.nowplaying4gpm.util.refreshTempArtwork
import com.geckour.nowplaying4gpm.util.setAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.setReceivedDelegateShareNodeId
import com.geckour.nowplaying4gpm.util.showNotification
import com.geckour.nowplaying4gpm.util.storePackageStatePostMastodon
import com.geckour.nowplaying4gpm.util.toByteArray
import com.geckour.nowplaying4gpm.util.updateTrackInfo
import com.geckour.nowplaying4gpm.util.updateWear
import com.geckour.nowplaying4gpm.util.withCatching
import com.google.android.gms.wearable.MessageEvent
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
        const val WEAR_PATH_TRACK_INFO_POST = "/track_info/post"
        private const val WEAR_PATH_TRACK_INFO_GET = "/track_info/get"
        private const val WEAR_PATH_POST_TWITTER = "/post/twitter"
        private const val WEAR_PATH_POST_SUCCESS = "/post/success"
        private const val WEAR_PATH_POST_FAILURE = "/post/failure"
        private const val WEAR_PATH_SHARE_DELEGATE = "/share/delegate"
        private const val WEAR_PATH_SHARE_SUCCESS = "/share/success"
        private const val WEAR_PATH_SHARE_FAILURE = "/share/failure"
        const val WEAR_KEY_SUBJECT = "key_subject"
        const val WEAR_KEY_ARTWORK = "key_artwork"

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
                        notificationManager.destroyNotification(this@NotificationService)
                    }

                    ACTION_INVOKE_UPDATE -> {
                        val trackInfo = sharedPreferences.getCurrentTrackInfo() ?: return@apply
                        launch {
                            updateTrackInfo(
                                this@NotificationService,
                                sharedPreferences,
                                spotifyApiClient,
                                lastFmApiClient,
                                currentMetadata ?: return@launch,
                                currentSbn?.packageName ?: return@launch,
                                currentSbn?.notification,
                                trackInfo.coreElement,
                                this@NotificationService::onMetadataCleared
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

        notificationManager.destroyNotification(this)
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
                (notification.mediaMetadata
                    ?: digMediaController(this.packageName)?.metadata)?.let {
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

    private fun onMetadataCleared() {
        currentSbn = null
        currentMetadata = null

        refreshMetadataJob?.cancel()
        currentTrackClearJob?.cancel()

        sharedPreferences.refreshTempArtwork(null)

        currentTrackClearJob = launch {
            reflectTrackInfo(
                this@NotificationService,
                sharedPreferences,
                null
            )
        }

        notificationManager.destroyNotification(this)
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
                    this@NotificationService,
                    sharedPreferences,
                    spotifyApiClient,
                    lastFmApiClient,
                    metadata,
                    playerPackageName,
                    notification,
                    metadata.getTrackCoreElement(),
                    this@NotificationService::onMetadataCleared
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
                showNotification(this@NotificationService, sharedPreferences, status)
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
                launch { updateWear(this@NotificationService, sharedPreferences, trackInfo) }
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
}