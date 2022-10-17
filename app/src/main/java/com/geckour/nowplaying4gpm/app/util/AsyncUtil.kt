package com.geckour.nowplaying4gpm.app.util

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.app.api.LastFmApiClient
import com.geckour.nowplaying4gpm.app.api.OkHttpProvider
import com.geckour.nowplaying4gpm.app.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.app.api.model.Image
import com.geckour.nowplaying4gpm.app.domain.model.SpotifyResult
import com.geckour.nowplaying4gpm.app.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.app.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.app.service.NotificationService
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Media
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import kotlin.math.max

inline fun <reified T> MastodonRequest<T>.executeCatching(
    noinline onCatch: ((Throwable) -> Unit)? = null
): T? = withCatching({ onCatch?.invoke(it) }) { execute() }

private suspend fun getArtworkUrlFromLastFmApi(
    client: LastFmApiClient,
    trackCoreElement: TrackInfo.TrackCoreElement,
    size: Image.Size = Image.Size.MEGA
): String? = if (trackCoreElement.album == null && trackCoreElement.artist == null) null
else client.searchAlbum(
    trackCoreElement.album, trackCoreElement.artist
)?.artworks?.let {
    it.find { image -> image.size == size.rawStr } ?: it.lastOrNull()
}?.url

suspend fun refreshArtworkUriFromLastFmApi(
    context: Context,
    client: LastFmApiClient,
    trackCoreElement: TrackInfo.TrackCoreElement
): Uri? {
    val url = getArtworkUrlFromLastFmApi(client, trackCoreElement) ?: return null

    return context.getBitmapFromUriString(url)?.refreshArtworkUri(context)
}

suspend fun refreshArtworkUriFromSpotify(
    context: Context,
    spotifyData: SpotifyResult.Data?
): Uri? {
    val url = spotifyData?.artworkUrl ?: return null

    return context.getBitmapFromUriString(url)?.refreshArtworkUri(context)
}

suspend fun Context.getBitmapFromUriString(
    uriString: String,
    maxSize: Int? = null
): Bitmap? = withCatching {
    val drawable = Coil.imageLoader(this).execute(
        ImageRequest.Builder(this)
            .data(uriString)
            .allowHardware(false)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .apply { maxSize?.let { size(it) } }
            .build()
    ).drawable as BitmapDrawable?

    return@withCatching drawable?.bitmap
}

suspend fun Context.checkStoragePermissionAsync(
    onNotGranted: (suspend (Context) -> Unit)? = null, onGranted: suspend (Context) -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        onGranted(this)
    } else {
        onNotGranted?.invoke(this)
    }
}

suspend fun updateTrackInfo(
    context: Context,
    sharedPreferences: SharedPreferences,
    spotifyApiClient: SpotifyApiClient,
    lastFmApiClient: LastFmApiClient,
    metadata: MediaMetadata,
    playerPackageName: String,
    notification: Notification?,
    coreElement: TrackInfo.TrackCoreElement = metadata.getTrackCoreElement(),
    onClearMetadata: () -> Unit = {}
): TrackInfo? {
    val quickUpdateSucceeded = onQuickUpdate(
        context,
        sharedPreferences,
        coreElement,
        playerPackageName
    )
    if (quickUpdateSucceeded.not()) {
        onClearMetadata()
        return null
    }

    val trackInfo = updateTrackInfo(
        context,
        sharedPreferences,
        spotifyApiClient,
        lastFmApiClient,
        metadata,
        playerPackageName,
        notification,
        coreElement,
        true,
        onClearMetadata
    )

    reflectTrackInfo(context, sharedPreferences, trackInfo, withArtwork = true)

    return trackInfo
}

suspend fun updateTrackInfo(
    context: Context,
    sharedPreferences: SharedPreferences,
    spotifyApiClient: SpotifyApiClient,
    lastFmApiClient: LastFmApiClient,
    metadata: MediaMetadata,
    playerPackageName: String,
    notification: Notification?,
    coreElement: TrackInfo.TrackCoreElement = metadata.getTrackCoreElement(),
    viaService: Boolean = false,
    onClearMetadata: () -> Unit = {}
): TrackInfo? {
    if (viaService.not() && onQuickUpdate(
            context,
            sharedPreferences,
            coreElement,
            playerPackageName
        ).not()
    ) {
        onClearMetadata()
        return null
    }

    val containsSpotifyPattern = sharedPreferences.getFormatPattern(context)
        .containsPattern(FormatPattern.SPOTIFY_URL)
    FirebaseAnalytics.getInstance(context.applicationContext)
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
    val needSpotifyDataForPlayer =
        useSpotifyData && useSpotifyDataPackageState.contains(playerPackageName)
    val spotifyResult =
        if (containsSpotifyPattern
            || sharedPreferences.getArtworkResolveOrder()
                .first { it.key == ArtworkResolveMethod.ArtworkResolveMethodKey.SPOTIFY }
                .enabled
            || needSpotifyDataForPlayer
        ) {
            spotifyApiClient.getSpotifyData(
                coreElement,
                playerPackageName,
                sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_SEARCH_SPOTIFY_STRICTLY)
            )
        } else null
    val spotifyData = (spotifyResult as? SpotifyResult.Success)?.data

    val artworkUri = metadata.storeArtworkUri(
        context,
        sharedPreferences,
        lastFmApiClient,
        coreElement,
        notification?.getArtworkBitmap(context),
        if (needSpotifyDataForPlayer) spotifyData else null
    )

    val trackInfo = TrackInfo(
        if (needSpotifyDataForPlayer) {
            coreElement.withSpotifyData(spotifyData)
        } else coreElement,
        artworkUri?.toString(),
        playerPackageName,
        playerPackageName.getAppName(context),
        spotifyData
    )

    if (viaService.not()) reflectTrackInfo(
        context,
        sharedPreferences,
        trackInfo,
        withArtwork = true
    )

    return trackInfo
}

suspend fun onQuickUpdate(
    context: Context,
    sharedPreferences: SharedPreferences,
    coreElement: TrackInfo.TrackCoreElement,
    packageName: String,
    viaService: Boolean = false
): Boolean {
    sharedPreferences.refreshTempArtwork(null)

    val trackInfo = TrackInfo(
        coreElement,
        null,
        packageName,
        packageName.getAppName(context),
        null
    )

    if (sharedPreferences.readyForShare(context, trackInfo).not()) {
        return false
    }

    if (viaService.not()) reflectTrackInfo(
        context,
        sharedPreferences,
        trackInfo,
        withArtwork = false
    )

    return true
}

suspend fun reflectTrackInfo(
    context: Context,
    sharedPreferences: SharedPreferences,
    info: TrackInfo?,
    withArtwork: Boolean
) {
    updateSharedPreference(sharedPreferences, info)
    updateWidget(context, info)
    if (withArtwork) {
        updateWear(context, sharedPreferences, info)
    }

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.showNotification(
        context,
        sharedPreferences,
        notificationManager,
        info
    )
}

private fun updateSharedPreference(sharedPreferences: SharedPreferences, trackInfo: TrackInfo?) {
    sharedPreferences.refreshCurrentTrackInfo(trackInfo)
}

private fun updateWidget(context: Context, trackInfo: TrackInfo?) {
    context.sendBroadcast(ShareWidgetProvider.getUpdateIntent(context, trackInfo))
}

suspend fun updateWear(
    context: Context,
    sharedPreferences: SharedPreferences,
    trackInfo: TrackInfo? = sharedPreferences.getCurrentTrackInfo()
) {
    val subject = sharedPreferences.getFormatPattern(context)
        .getSharingText(trackInfo, sharedPreferences.getFormatPatternModifiers())
    val artwork = trackInfo?.artworkUriString?.let { uriString ->
        context.getBitmapFromUriString(uriString)?.let {
            val scale = 400f / max(it.width, it.height)
            val scaled = Bitmap.createScaledBitmap(
                it,
                (it.width * scale).toInt(),
                (it.height * scale).toInt(),
                false
            )
            it.recycle()
            scaled
        }
    }
    Wearable.getDataClient(context)
        .putDataItem(
            PutDataMapRequest.create(NotificationService.WEAR_PATH_POST_SHARING_INFO).apply {
                dataMap.apply {
                    subject?.let { putString(NotificationService.WEAR_KEY_SUBJECT, it) }
                    artwork?.let {
                        putAsset(
                            NotificationService.WEAR_KEY_ARTWORK,
                            Asset.createFromBytes(artwork.toByteArray())
                        )
                    }
                }
            }.asPutDataRequest()
        )
}

private suspend fun NotificationManager.showNotification(
    context: Context,
    sharedPreferences: SharedPreferences,
    notificationManager: NotificationManager,
    trackInfo: TrackInfo?
) {
    if (areNotificationsEnabled()
        && sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_RESIDE)
        && sharedPreferences.readyForShare(context, trackInfo)
    ) {
        trackInfo?.getNotification(context)?.let { notification ->
            context.checkStoragePermissionAsync {
                notify(NotificationService.NotificationType.SHARE.id, notification)
            }
        }
    } else notificationManager.destroyNotification()
}

private suspend fun TrackInfo.getNotification(context: Context): Notification {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val notificationBuilder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(
                context,
                com.geckour.nowplaying4gpm.app.service.NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name
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

private suspend fun MediaMetadata.storeArtworkUri(
    context: Context,
    sharedPreferences: SharedPreferences,
    lastFmApiClient: LastFmApiClient,
    coreElement: TrackInfo.TrackCoreElement,
    notificationBitmap: Bitmap?,
    spotifyData: SpotifyResult.Data?
): Uri? {
    // Check whether arg metadata and current metadata are the same or not
    val cacheInfo = sharedPreferences.getCurrentTrackInfo()
    if (coreElement.isAllNonNull && cacheInfo?.artworkUriString != null && coreElement == cacheInfo.coreElement) {
        return sharedPreferences.getTempArtworkUri(context)
    }

    sharedPreferences.getArtworkResolveOrder().filter { it.enabled }.forEach { method ->
        when (method.key) {
            ArtworkResolveMethod.ArtworkResolveMethodKey.CONTENT_RESOLVER -> {
                context.getArtworkUriFromDevice(coreElement)?.apply {
                    sharedPreferences.refreshTempArtwork(this)
                    return this
                }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.MEDIA_METADATA_URI -> {
                this.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)?.let { uri ->
                    context.getBitmapFromUriString(uri)
                        ?.refreshArtworkUri(context)
                        ?.let { return it }
                }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.MEDIA_METADATA_BITMAP -> {
                this.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?.let { bitmap ->
                    bitmap.refreshArtworkUri(context)
                        ?.let { return it }
                }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.NOTIFICATION_BITMAP -> {
                notificationBitmap?.refreshArtworkUri(context)
                    ?.let { return it }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.LAST_FM -> {
                refreshArtworkUriFromLastFmApi(
                    context,
                    lastFmApiClient,
                    coreElement
                )?.let { return it }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.SPOTIFY -> {
                refreshArtworkUriFromSpotify(context, spotifyData)
                    ?.let { return it }
            }
        }
    }

    return null
}

private fun Notification.getArtworkBitmap(context: Context): Bitmap? =
    (getLargeIcon()?.loadDrawable(context) as? BitmapDrawable)?.bitmap?.let {
        withCatching { it.copy(it.config, false) }
    }

private fun TrackInfo.TrackCoreElement.withSpotifyData(
    data: SpotifyResult.Data?
): TrackInfo.TrackCoreElement =
    TrackInfo.TrackCoreElement(
        data?.trackName ?: title,
        data?.artistName ?: artist,
        data?.albumName ?: album,
        composer
    )

suspend fun postMastodon(
    context: Context,
    sharedPreferences: SharedPreferences,
    trackInfo: TrackInfo
) {
    if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON)) {
        delay(sharedPreferences.getDelayDurationPostMastodon())

        val subject =
            sharedPreferences.getSharingText(context, trackInfo) ?: return

        FirebaseAnalytics.getInstance(context.applicationContext)
            .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked auto post")
            })

        val artworkBytes = if (sharedPreferences.getSwitchState(
                PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK
            )
        ) {
            trackInfo.artworkUriString?.let {
                return@let withCatching {
                    context.getBitmapFromUriString(it)?.toByteArray()
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

        showShortNotify(context, sharedPreferences, result)
    }
}

private suspend fun showShortNotify(
    context: Context,
    sharedPreferences: SharedPreferences,
    status: Status
) {
    if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON)) {
        context.getSystemService(NotificationManager::class.java)?.apply {
            showNotification(context, sharedPreferences, status)
            delay(2500)
            cancel(NotificationService.NotificationType.NOTIFY_SUCCESS_MASTODON.id)
        }
    }
}

suspend fun NotificationManager.showNotification(
    context: Context,
    sharedPreferences: SharedPreferences,
    status: Status
) {
    if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON)) {
        context.checkStoragePermissionAsync {
            notify(
                NotificationService.NotificationType.NOTIFY_SUCCESS_MASTODON.id,
                status.getNotification(context)
            )
        }
    }
}

private suspend fun Status.getNotification(context: Context): Notification {
    val notificationBuilder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(
                context,
                NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name
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

suspend fun forceUpdateTrackInfoIfNeeded(
    context: Context,
    sharedPreferences: SharedPreferences,
    spotifyApiClient: SpotifyApiClient,
    lastFmApiClient: LastFmApiClient,
    onError: () -> Unit = {}
): TrackInfo? {
    if (sharedPreferences.readyForShare(context).not()) {
        val isNotificationServiceRunning =
            context.getSystemService(ActivityManager::class.java)
                ?.getRunningServices(1)
                ?.any {
                    it.service.className == NotificationService::class.java.name
                } == true
        if (isNotificationServiceRunning) {
            onError()
            return null
        } else {
            val mediaController = context.digMediaController() ?: run {
                onError()
                return null
            }
            val metadata = mediaController.metadata ?: run {
                onError()
                return null
            }
            sharedPreferences.storePackageStatePostMastodon(mediaController.packageName)
            val trackInfo = updateTrackInfo(
                context,
                sharedPreferences,
                spotifyApiClient,
                lastFmApiClient,
                metadata,
                mediaController.packageName,
                null
            ) ?: run {
                onError()
                return null
            }

            val allowedPostMastodon = sharedPreferences.getPackageStateListPostMastodon()
                .firstOrNull { it.packageName == mediaController.packageName }
                ?.state == true
            if (allowedPostMastodon)
                postMastodon(context, sharedPreferences, trackInfo)

            return trackInfo
        }
    }

    return sharedPreferences.getCurrentTrackInfo()
}