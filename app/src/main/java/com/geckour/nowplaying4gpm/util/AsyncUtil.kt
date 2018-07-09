package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.model.Image
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.experimental.CoroutineContext

fun <T> async(context: CoroutineContext = CommonPool,
              onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> T) =
        kotlinx.coroutines.experimental.async(context, block = {
            try {
                block()
            } catch (e: Exception) {
                Timber.e(e)
                onError(e)
                null
            }
        })

fun ui(managerList: ArrayList<Job>, block: suspend CoroutineScope.() -> Unit) =
        launch(UI) { block() }.apply { managerList.add(this) }

private suspend fun getAlbumIdFromDevice(context: Context, trackCoreElement: TrackCoreElement): Long? =
        async {
            if (trackCoreElement.isAllNonNull.not()) return@async null

            val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                    getContentQuerySelection(
                            requireNotNull(trackCoreElement.title),
                            requireNotNull(trackCoreElement.artist),
                            requireNotNull(trackCoreElement.album)),
                    null,
                    null
            )

            return@async (if (cursor.moveToNext()) {
                cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
            } else null).apply { cursor.close() }
        }.await()

private suspend fun getArtworkUriFromDevice(context: Context, albumId: Long?): Uri? =
        albumId?.let {
            async {
                ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), it
                ).let {
                    try {
                        context.contentResolver.openInputStream(it).close()
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

suspend fun getArtworkUriFromDevice(context: Context, trackCoreElement: TrackCoreElement): Uri? =
        getArtworkUriFromDevice(context, getAlbumIdFromDevice(context, trackCoreElement))

private suspend fun getArtworkUrlFromLastFmApi(client: LastFmApiClient,
                                               trackCoreElement: TrackCoreElement,
                                               size: Image.Size = Image.Size.MEGA): String? =
        if (trackCoreElement.album == null && trackCoreElement.artist == null) null
        else client.searchAlbum(
                trackCoreElement.album,
                trackCoreElement.artist)?.artworks?.let {
            it.find { it.size == size.rawStr } ?: it.lastOrNull()
        }?.url

suspend fun refreshArtworkUriFromLastFmApi(context: Context, client: LastFmApiClient,
                                           trackCoreElement: TrackCoreElement): Uri? {
    val url = getArtworkUrlFromLastFmApi(client, trackCoreElement) ?: return null
    val bitmap = getBitmapFromUrl(context, url)?.let { it.copy(it.config, false) } ?: return null
    val uri = refreshArtworkUriFromBitmap(context, bitmap)
    bitmap.recycle()

    return uri
}

suspend fun refreshArtworkUriFromBitmap(context: Context, bitmap: Bitmap, checkSimilarity: Boolean = false): Uri? =
        async {
            if (bitmap.isRecycled) {
                Timber.e(IllegalStateException("Bitmap is recycled"))
                return@async null
            }

            val placeholderBitmap =
                    (context.getDrawable(R.mipmap.bg_default_album_art) as BitmapDrawable).bitmap
            if (checkSimilarity && (bitmap.similarity(placeholderBitmap).await() ?: 1f) > 0.9) return@async null

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val dirName = "images"
            val fileName = "temp_artwork.jpg"
            val dir = context.filesDir.let { File(it, dirName) }
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

private suspend fun getBitmapFromUrl(context: Context, url: String?): Bitmap? =
        url?.let {
            try {
                async {
                    Glide.with(context)
                            .asBitmap().load(it)
                            .submit().get()
                }.await()
            } catch (e: Throwable) {
                Timber.e(e)
                null
            }
        }

suspend fun getBitmapFromUri(context: Context, uri: Uri?): Bitmap? =
        try {
            val glideOptions =
                    RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .signature { System.currentTimeMillis().toString() }
            uri?.let {
                async {
                    Glide.with(context)
                            .asBitmap().load(uri).apply(glideOptions)
                            .submit().get()
                }.await()
            }
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }

suspend fun getBitmapFromUriString(context: Context, uriString: String): Bitmap? =
        try {
            Uri.parse(uriString)
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }?.let { getBitmapFromUri(context, it) }
