package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.model.Image
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.sys1yagi.mastodon4j.MastodonRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> CoroutineScope.asyncOrNull(
        onError: (Throwable) -> Unit = { Timber.e(it) },
        block: suspend CoroutineScope.() -> T) =
        async {
            try {
                block()
            } catch (t: Throwable) {
                onError(t)
                null
            }
        }

fun <T> MastodonRequest<T>.toJob(coroutineScope: CoroutineScope): Deferred<T?> =
        coroutineScope.asyncOrNull {
            execute()
        }

private suspend fun getAlbumIdFromDevice(context: Context, coroutineScope: CoroutineScope, trackCoreElement: TrackCoreElement): Long? =
        coroutineScope.asyncOrNull {
            if (trackCoreElement.isAllNonNull.not()) return@asyncOrNull null

            return@asyncOrNull context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                    getContentQuerySelection(
                            requireNotNull(trackCoreElement.title),
                            requireNotNull(trackCoreElement.artist),
                            requireNotNull(trackCoreElement.album)),
                    null,
                    null
            )?.use {
                (if (it.moveToNext()) {
                    it.getLong(it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                } else null).apply { it.close() }
            }
        }.await()

private suspend fun getArtworkUriFromDevice(context: Context, coroutineScope: CoroutineScope, albumId: Long?): Uri? =
        albumId?.let {
            coroutineScope.asyncOrNull {
                ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), it
                )?.let {
                    try {
                        context.contentResolver.openInputStream(it)?.close()
                                ?: throw IllegalStateException()
                        PreferenceManager.getDefaultSharedPreferences(context)
                                .refreshTempArtwork(it)
                        it
                    } catch (e: Throwable) {
                        Timber.e(e)
                        null
                    }
                }
            }.await()
        }

suspend fun getArtworkUriFromDevice(context: Context, coroutineScope: CoroutineScope, trackCoreElement: TrackCoreElement): Uri? =
        getArtworkUriFromDevice(context, coroutineScope, getAlbumIdFromDevice(context, coroutineScope, trackCoreElement))

private suspend fun getArtworkUrlFromLastFmApi(client: LastFmApiClient,
                                               trackCoreElement: TrackCoreElement,
                                               size: Image.Size = Image.Size.MEGA): String? =
        if (trackCoreElement.album == null && trackCoreElement.artist == null) null
        else client.searchAlbum(
                trackCoreElement.album,
                trackCoreElement.artist)?.artworks?.let {
            it.find { it.size == size.rawStr } ?: it.lastOrNull()
        }?.url

suspend fun refreshArtworkUriFromLastFmApi(context: Context, coroutineScope: CoroutineScope,
                                           client: LastFmApiClient,
                                           trackCoreElement: TrackCoreElement): Uri? {
    val url = getArtworkUrlFromLastFmApi(client, trackCoreElement) ?: return null
    val bitmap = getBitmapFromUrl(context, coroutineScope, url)?.let { it.copy(it.config, false) }
            ?: return null
    val uri = refreshArtworkUriFromBitmap(context, coroutineScope, bitmap)
    bitmap.recycle()

    return uri
}

suspend fun refreshArtworkUriFromBitmap(context: Context, coroutineScope: CoroutineScope, bitmap: Bitmap): Uri? =
        coroutineScope.asyncOrNull {
            if (bitmap.isRecycled) {
                Timber.e(IllegalStateException("Bitmap is recycled"))
                return@asyncOrNull null
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
        }.await()

private suspend fun getBitmapFromUrl(context: Context, coroutineScope: CoroutineScope, url: String?): Bitmap? =
        url?.let {
            try {
                coroutineScope.asyncOrNull {
                    Glide.with(context)
                            .asBitmap().load(it)
                            .submit().get()
                }.await()
            } catch (e: Throwable) {
                Timber.e(e)
                null
            }
        }

suspend fun getBitmapFromUri(context: Context, coroutineScope: CoroutineScope, uri: Uri?): Bitmap? =
        try {
            val glideOptions =
                    RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .signature { System.currentTimeMillis().toString() }
            uri.let {
                coroutineScope.asyncOrNull {
                    Glide.with(context)
                            .asBitmap().load(uri).apply(glideOptions)
                            .submit().get()
                }.await()
            }
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }

suspend fun getBitmapFromUriString(context: Context, coroutineScope: CoroutineScope, uriString: String): Bitmap? =
        try {
            getBitmapFromUri(context, coroutineScope, Uri.parse(uriString))
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }
