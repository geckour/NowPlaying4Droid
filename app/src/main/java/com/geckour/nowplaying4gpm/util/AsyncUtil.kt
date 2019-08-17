package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.model.Image
import com.geckour.nowplaying4gpm.domain.model.MediaIdInfo
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.sys1yagi.mastodon4j.MastodonRequest
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

fun getExceptionHandler(onError: (Throwable) -> Unit = { Timber.e(it) }): CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, throwable ->
        onError(throwable)
        Crashlytics.logException(throwable)
    }

suspend fun <T> asyncOrNull(
    onError: (Throwable) -> Unit = { Timber.e(it) },
    block: suspend CoroutineScope.() -> T
) =
    coroutineScope {
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

suspend fun <T> MastodonRequest<T>.toJob(): Deferred<T?> =
    asyncOrNull {
        execute()
    }

private suspend fun getAlbumIdFromDevice(context: Context, trackCoreElement: TrackInfo.TrackCoreElement): MediaIdInfo? =
    asyncOrNull {
        if (trackCoreElement.isAllNonNull.not()) return@asyncOrNull null

        return@asyncOrNull context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID),
            getContentQuerySelection(
                requireNotNull(trackCoreElement.title),
                requireNotNull(trackCoreElement.artist),
                requireNotNull(trackCoreElement.album)
            ),
            null,
            null
        )?.use { it.getAlbumIdFromDevice() }
    }.await()

private fun Cursor?.getAlbumIdFromDevice(): MediaIdInfo? =
    this?.let {
        (if (it.moveToFirst()) {
            MediaIdInfo(
                it.getLong(it.getColumnIndex(MediaStore.Audio.Media._ID)),
                it.getLong(it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
            )
        } else null)
    }

private suspend fun getArtworkUriFromDevice(context: Context, mediaIdInfo: MediaIdInfo?): Uri? =
    mediaIdInfo?.let {
        asyncOrNull {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.mediaTrackId)
            try {
                val retriever = MediaMetadataRetriever().apply { setDataSource(context, uri) }
                retriever.embeddedPicture?.let uri@{ biteArray ->
                    refreshArtworkUriFromBitmap(
                        context,
                        BitmapFactory.decodeByteArray(biteArray, 0, biteArray.size)
                            ?: return@uri null
                    )
                } ?: ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    it.mediaAlbumId
                )?.let {
                    context.contentResolver.openInputStream(it)?.close()
                        ?: throw IllegalStateException()
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .refreshTempArtwork(it)
                    it
                }
            } catch (t: Throwable) {
                Timber.e(t)
                Crashlytics.logException(t)
                null
            }
        }.await()
    }

suspend fun getArtworkUriFromDevice(context: Context, trackCoreElement: TrackInfo.TrackCoreElement): Uri? =
    getArtworkUriFromDevice(context, getAlbumIdFromDevice(context, trackCoreElement))

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
        it.find { it.size == size.rawStr } ?: it.lastOrNull()
    }?.url

suspend fun refreshArtworkUriFromLastFmApi(
    context: Context,
    client: LastFmApiClient,
    trackCoreElement: TrackInfo.TrackCoreElement
): Uri? {
    val url = getArtworkUrlFromLastFmApi(client, trackCoreElement) ?: return null
    val bitmap = getBitmapFromUrl(context, url)?.let { it.copy(it.config, false) }
        ?: return null
    val uri = refreshArtworkUriFromBitmap(context, bitmap)
    bitmap.recycle()

    return uri
}

suspend fun refreshArtworkUriFromBitmap(context: Context, bitmap: Bitmap): Uri? =
    withContext(Dispatchers.IO) {
        if (bitmap.isRecycled) {
            Timber.e(IllegalStateException("Bitmap is recycled"))
            return@withContext null
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val dirName = "images"
        val fileName = "temp_artwork.jpg"
        val dir = File(context.filesDir, dirName)
        val file = File(dir, fileName)

        if (file.exists()) file.delete()
        if (dir.exists().not()) dir.mkdir()

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            it.flush()
        }

        FileProvider.getUriForFile(context, BuildConfig.FILES_AUTHORITY, file).apply {
            sharedPreferences.refreshTempArtwork(this)
        }
    }

suspend fun getBitmapFromUrl(context: Context, url: String?): Bitmap? =
    url?.let {
        try {
            val glideOptions =
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .signature { System.currentTimeMillis().toString() }
            withContext(Dispatchers.IO) {
                Glide.with(context)
                    .asBitmap().load(it).apply(glideOptions)
                    .submit().get()
            }
        } catch (t: Throwable) {
            Timber.e(t)
            Crashlytics.logException(t)
            null
        }
    }

suspend fun getBitmapFromUri(context: Context, uri: Uri?): Bitmap? =
    uri?.let {
        try {
            val glideOptions =
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .signature { System.currentTimeMillis().toString() }
            withContext(Dispatchers.IO) {
                Glide.with(context)
                    .asBitmap().load(uri).apply(glideOptions)
                    .submit().get()
            }
        } catch (t: Throwable) {
            Timber.e(t)
            Crashlytics.logException(t)
            null
        }
    }

suspend fun getBitmapFromUriString(context: Context, uriString: String): Bitmap? =
    try {
        getBitmapFromUri(context, Uri.parse(uriString))
    } catch (t: Throwable) {
        Timber.e(t)
        Crashlytics.logException(t)
        null
    }
