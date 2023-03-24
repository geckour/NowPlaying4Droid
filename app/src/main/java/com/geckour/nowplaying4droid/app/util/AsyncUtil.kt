package com.geckour.nowplaying4droid.app.util

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
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.api.LastFmApiClient
import com.geckour.nowplaying4droid.app.api.OkHttpProvider
import com.geckour.nowplaying4droid.app.api.SpotifyApiClient
import com.geckour.nowplaying4droid.app.api.YouTubeDataClient
import com.geckour.nowplaying4droid.app.api.model.Image
import com.geckour.nowplaying4droid.app.domain.model.SpotifyResult
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.receiver.ShareWidgetProvider
import com.geckour.nowplaying4droid.app.service.NotificationService
import com.geckour.nowplayingsubjectbuilder.lib.model.FormatPattern
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
import kotlin.math.max

inline fun <reified T> MastodonRequest<T>.executeCatching(
    noinline onCatch: ((Throwable) -> Unit)? = null
): T? = withCatching({ onCatch?.invoke(it) }) { execute() }

private suspend fun getArtworkUrlFromLastFmApi(
    client: LastFmApiClient,
    trackCoreElement: TrackDetail.TrackCoreElement,
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
    trackCoreElement: TrackDetail.TrackCoreElement
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

suspend fun updateTrackDetail(
    context: Context,
    sharedPreferences: SharedPreferences,
    spotifyApiClient: SpotifyApiClient,
    youTubeDataClient: YouTubeDataClient,
    lastFmApiClient: LastFmApiClient,
    metadata: MediaMetadata,
    playerPackageName: String,
    notification: Notification?,
    coreElement: TrackDetail.TrackCoreElement = metadata.getTrackCoreElement(),
    onClearMetadata: () -> Unit = {}
): TrackDetail? {
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

    val trackInfo = updateTrackDetail(
        context,
        sharedPreferences,
        spotifyApiClient,
        youTubeDataClient,
        lastFmApiClient,
        metadata,
        playerPackageName,
        notification,
        coreElement,
        true,
        onClearMetadata
    )

    reflectTrackDetail(context, sharedPreferences, trackInfo, true)

    return trackInfo
}

suspend fun updateTrackDetail(
    context: Context,
    sharedPreferences: SharedPreferences,
    spotifyApiClient: SpotifyApiClient,
    youTubeDataClient: YouTubeDataClient,
    lastFmApiClient: LastFmApiClient,
    metadata: MediaMetadata,
    playerPackageName: String,
    notification: Notification?,
    coreElement: TrackDetail.TrackCoreElement = metadata.getTrackCoreElement(),
    viaService: Boolean = false,
    onClearMetadata: () -> Unit = {}
): TrackDetail? {
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

    val formatPattern = sharedPreferences.getFormatPattern(context)
    val containsSpotifyPattern = formatPattern.containsPattern(FormatPattern.SPOTIFY_URL)
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

    val containsYTMPattern = formatPattern.containsPattern(FormatPattern.YOUTUBE_MUSIC_URL)
    val youTubeMusicUrl =
        if (containsYTMPattern && playerPackageName == "com.google.android.apps.youtube.music") {
            youTubeDataClient.searchYouTube(coreElement)
        } else null

    val artworkUri = metadata.storeArtworkUri(
        context,
        playerPackageName,
        sharedPreferences,
        spotifyApiClient,
        lastFmApiClient,
        coreElement,
        notification?.getArtworkBitmap(context),
        if (needSpotifyDataForPlayer) spotifyData else null
    )

    val trackDetail = TrackDetail(
        if (needSpotifyDataForPlayer) {
            coreElement.withSpotifyData(spotifyData)
        } else coreElement,
        artworkUri?.toString(),
        playerPackageName,
        spotifyData,
        youTubeMusicUrl
    )

    if (viaService.not()) reflectTrackDetail(
        context,
        sharedPreferences,
        trackDetail,
        true
    )

    return trackDetail
}

suspend fun onQuickUpdate(
    context: Context,
    sharedPreferences: SharedPreferences,
    coreElement: TrackDetail.TrackCoreElement,
    packageName: String,
    viaService: Boolean = false
): Boolean {
    sharedPreferences.refreshTempArtwork(null)

    val trackDetail = TrackDetail(
        coreElement,
        null,
        packageName,
        null,
        null
    )

    if (sharedPreferences.readyForShare(context, trackDetail).not()) {
        return false
    }

    if (viaService.not()) reflectTrackDetail(
        context,
        sharedPreferences,
        trackDetail,
        false
    )

    return true
}

suspend fun reflectTrackDetail(
    context: Context,
    sharedPreferences: SharedPreferences,
    trackDetail: TrackDetail?,
    withArtwork: Boolean
) {
    updateSharedPreference(sharedPreferences, trackDetail)
    updateWidget(context, trackDetail)
    if (withArtwork) {
        updateWear(context, sharedPreferences, trackDetail)
    }

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.showNotification(
        context,
        sharedPreferences,
        notificationManager,
        trackDetail
    )
}

private fun updateSharedPreference(
    sharedPreferences: SharedPreferences,
    trackDetail: TrackDetail?
) {
    sharedPreferences.refreshCurrentTrackDetail(trackDetail)
}

private fun updateWidget(context: Context, trackDetail: TrackDetail?) {
    context.sendBroadcast(ShareWidgetProvider.getUpdateIntent(context, trackDetail))
}

suspend fun updateWear(
    context: Context,
    sharedPreferences: SharedPreferences,
    trackDetail: TrackDetail? = sharedPreferences.getCurrentTrackDetail()
) {
    val subject = sharedPreferences.getSharingText(context, trackDetail)
    val artwork = trackDetail?.artworkUriString?.let { uriString ->
        context.getBitmapFromUriString(uriString)?.let {
            val scale = 400f / max(it.width, it.height)
            val scaled = Bitmap.createScaledBitmap(
                it,
                (it.width * scale).toInt(),
                (it.height * scale).toInt(),
                false
            )
            if (scaled.width != it.width && scaled.height != it.height) it.recycle()
            scaled
        }
    }
    Wearable.getDataClient(context)
        .putDataItem(
            PutDataMapRequest.create(NotificationService.WEAR_PATH_POST_SHARING_INFO).apply {
                dataMap.apply {
                    subject?.let { putString(NotificationService.WEAR_KEY_SUBJECT, it) }
                    artwork?.toByteArray()?.let {
                        putAsset(
                            NotificationService.WEAR_KEY_ARTWORK,
                            Asset.createFromBytes(it)
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
    trackDetail: TrackDetail?
) {
    if (areNotificationsEnabled()
        && sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_RESIDE)
        && sharedPreferences.readyForShare(context, trackDetail)
    ) {
        trackDetail?.getNotification(context)?.let { notification ->
            context.checkStoragePermissionAsync {
                notify(NotificationService.NotificationType.SHARE.id, notification)
            }
        }
    } else notificationManager.destroyNotification()
}

private suspend fun TrackDetail.getNotification(context: Context): Notification {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val notificationBuilder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(
                context,
                NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name
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
            getClearTrackDetailPendingIntent(context)
        ).build()
        val notificationText = sharedPreferences.getSharingText(
            context,
            this@getNotification,
        )?.foldBreaks()

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
        style = Notification.BigPictureStyle()
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
    playerPackageName: String,
    sharedPreferences: SharedPreferences,
    spotifyApiClient: SpotifyApiClient,
    lastFmApiClient: LastFmApiClient,
    coreElement: TrackDetail.TrackCoreElement,
    notificationBitmap: Bitmap?,
    spotifyData: SpotifyResult.Data?
): Uri? {
    // Check whether arg metadata and current metadata are the same or not
    val cacheInfo = sharedPreferences.getCurrentTrackDetail()
    if (coreElement.isAllNonNull && cacheInfo?.artworkUriString != null && coreElement == cacheInfo.coreElement) {
        return sharedPreferences.getTempArtworkUri(context)
    }

    sharedPreferences.getArtworkResolveOrder().filter { it.enabled }.forEach { method ->
        when (method.key) {
            ArtworkResolveMethod.ArtworkResolveMethodKey.CONTENT_RESOLVER -> {
                context.getArtworkUriFromDevice(coreElement)?.also {
                    sharedPreferences.refreshTempArtwork(it)
                    return it
                }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.MEDIA_METADATA_URI -> {
                this.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)?.let { uri ->
                    context.getBitmapFromUriString(uri)
                        ?.refreshArtworkUri(context)
                        ?.also { return it }
                }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.MEDIA_METADATA_BITMAP -> {
                this.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?.let { bitmap ->
                    bitmap.refreshArtworkUri(context)
                        ?.also { return it }
                }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.NOTIFICATION_BITMAP -> {
                notificationBitmap?.refreshArtworkUri(context)
                    ?.also { return it }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.LAST_FM -> {
                refreshArtworkUriFromLastFmApi(
                    context,
                    lastFmApiClient,
                    coreElement
                )?.also { return it }
            }
            ArtworkResolveMethod.ArtworkResolveMethodKey.SPOTIFY -> {
                val result = spotifyApiClient.getSpotifyData(
                    coreElement,
                    playerPackageName,
                    sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_SEARCH_SPOTIFY_STRICTLY
                    )
                )
                refreshArtworkUriFromSpotify(
                    context,
                    spotifyData ?: (result as? SpotifyResult.Success)?.data
                )?.also { return it }
            }
        }
    }

    return null
}

private fun Notification.getArtworkBitmap(context: Context): Bitmap? =
    (getLargeIcon()?.loadDrawable(context) as? BitmapDrawable)?.bitmap?.let {
        withCatching { it.copy(it.config, false) }
    }

private fun TrackDetail.TrackCoreElement.withSpotifyData(
    data: SpotifyResult.Data?
): TrackDetail.TrackCoreElement =
    TrackDetail.TrackCoreElement(
        data?.trackName ?: title,
        data?.artistName ?: artist,
        data?.albumName ?: album,
        composer
    )

suspend fun postMastodon(
    context: Context,
    sharedPreferences: SharedPreferences,
    trackDetail: TrackDetail
) {
    if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON)) {
        delay(sharedPreferences.getDelayDurationPostMastodon())

        val subject =
            sharedPreferences.getSharingText(context, trackDetail) ?: return

        FirebaseAnalytics.getInstance(context.applicationContext)
            .logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked auto post")
            })

        val artworkBytes = if (sharedPreferences.getSwitchState(
                PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK
            )
        ) {
            trackDetail.artworkUriString?.let {
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

suspend fun forceUpdateTrackDetailIfNeeded(
    context: Context,
    sharedPreferences: SharedPreferences,
    spotifyApiClient: SpotifyApiClient,
    youTubeDataClient: YouTubeDataClient,
    lastFmApiClient: LastFmApiClient,
    onError: () -> Unit = {}
): TrackDetail? {
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
            val trackDetail = updateTrackDetail(
                context,
                sharedPreferences,
                spotifyApiClient,
                youTubeDataClient,
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
                postMastodon(context, sharedPreferences, trackDetail)

            return trackDetail
        }
    }

    return sharedPreferences.getCurrentTrackDetail()
}