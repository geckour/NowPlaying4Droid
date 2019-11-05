package com.geckour.nowplaying4gpm.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.model.Image
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.ui.settings.SettingsActivity
import com.sys1yagi.mastodon4j.MastodonRequest
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

suspend fun <T> asyncOrNull(
    onError: (Throwable) -> Unit = { Timber.e(it) },
    block: suspend CoroutineScope.() -> T
) = coroutineScope {
    async {
        try {
            block()
        } catch (t: Throwable) {
            onError(t)
            Crashlytics.logException(t)
            null
        }
    }
}

fun <T> MastodonRequest<T>.executeCatching(onCatch: ((Throwable) -> Unit)? = null): T? =
    try {
        execute()
    } catch (t: Throwable) {
        onCatch?.invoke(t) ?: Timber.e(t)
        null
    }

private suspend fun getArtworkUrlFromLastFmApi(
    client: LastFmApiClient,
    trackCoreElement: TrackInfo.TrackCoreElement,
    size: Image.Size = Image.Size.MEGA
): String? =
    if (trackCoreElement.album == null && trackCoreElement.artist == null) null
    else client.searchAlbum(
        trackCoreElement.album,
        trackCoreElement.artist
    )?.artworks?.let {
        it.find { image -> image.size == size.rawStr } ?: it.lastOrNull()
    }?.url

suspend fun refreshArtworkUriFromLastFmApi(
    context: Context,
    client: LastFmApiClient,
    trackCoreElement: TrackInfo.TrackCoreElement
): Uri? {
    val url = getArtworkUrlFromLastFmApi(client, trackCoreElement) ?: return null
    val file = context.getFileFromUrl(url) ?: return null

    return file.readBytes().refreshArtworkUri(context)
}

suspend fun Uri?.getFile(context: Context): File? =
    this?.let {
        try {
            val glideOptions =
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .signature { System.currentTimeMillis().toString() }
            withContext(Dispatchers.IO) {
                Glide.with(context)
                    .asFile().load(it).apply(glideOptions)
                    .submit().get()
            }
        } catch (t: Throwable) {
            Timber.e(t)
            Crashlytics.logException(t)
            null
        }
    }

suspend fun Context.getFileFromUriString(uriString: String): File? =
    try {
        Uri.parse(uriString).getFile(this)
    } catch (t: Throwable) {
        Timber.e(t)
        Crashlytics.logException(t)
        null
    }

suspend fun Context.getBitmapFromUriString(uriString: String): Bitmap? =
    try {
        val glideOptions =
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .signature { System.currentTimeMillis().toString() }
        withContext(Dispatchers.IO) {
            Glide.with(this@getBitmapFromUriString)
                .asBitmap().load(uriString).apply(glideOptions)
                .submit().get()
        }
    } catch (t: Throwable) {
        Timber.e(t)
        Crashlytics.logException(t)
        null
    }

suspend fun Context.checkStoragePermissionAsync(
    onNotGranted: (suspend (Context) -> Unit)? = null,
    onGranted: suspend (Context) -> Unit = {}
) {
    if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        == PackageManager.PERMISSION_GRANTED
    ) {
        onGranted(this)
    } else {
        onNotGranted?.invoke(this)
            ?: startActivity(
                SettingsActivity.getIntent(this).apply {
                    flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
                })
    }
}
