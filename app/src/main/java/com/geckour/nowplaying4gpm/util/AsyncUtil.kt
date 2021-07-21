package com.geckour.nowplaying4gpm.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.model.Image
import com.geckour.nowplaying4gpm.domain.model.SpotifySearchResult
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.ui.settings.SettingsActivity
import com.sys1yagi.mastodon4j.MastodonRequest
import timber.log.Timber

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
    client: SpotifyApiClient,
    trackCoreElement: TrackInfo.TrackCoreElement
): Uri? {
    val url =
        (client.getSpotifyData(trackCoreElement) as? SpotifySearchResult.Success)?.data?.artworkUrl
            ?: return null

    return context.getBitmapFromUriString(url)?.refreshArtworkUri(context)
}

suspend fun Context.getBitmapFromUriString(
    uriString: String,
    maxSize: Int? = null
): Bitmap? = withCatching {
    Timber.d("np4d uriString: $uriString")
    val drawable = Coil.execute(
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
    if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        onGranted(this)
    } else {
        onNotGranted?.invoke(this) ?: startActivity(SettingsActivity.getIntent(this).apply {
            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
