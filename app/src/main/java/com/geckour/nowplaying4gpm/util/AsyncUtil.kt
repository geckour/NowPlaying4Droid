package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import com.bumptech.glide.Glide
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

fun <T> async(context: CoroutineContext = CommonPool, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> T) =
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

object AsyncUtil {

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
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it).let {
                        try {
                            context.contentResolver.openInputStream(it).close()
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

    private suspend fun getArtworkUrlFromLastFmApi(client: LastFmApiClient, trackCoreElement: TrackCoreElement, size: Image.Size = Image.Size.MEGA): String? =
            if (trackCoreElement.album == null && trackCoreElement.artist == null) null
            else client.searchAlbum(
                    trackCoreElement.album,
                    trackCoreElement.artist)?.artworks?.let {
                it.find { it.size == size.rawStr } ?: it.lastOrNull()
            }?.url

    suspend fun getArtworkUriFromLastFmApi(context: Context, client: LastFmApiClient, trackCoreElement: TrackCoreElement): Uri? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val cacheInfo = sharedPreferences.getCurrentTrackInfo()
        return if (trackCoreElement.isAllNonNull
                && cacheInfo?.artworkUriString != null
                && trackCoreElement == cacheInfo.coreElement) {
            sharedPreferences.getTempArtworkUri(context)
        } else {
            getBitmapFromUrl(context, getArtworkUrlFromLastFmApi(client, trackCoreElement))?.let {
                refreshArtworkUriFromBitmap(context, it)
            }
        }
    }

    private suspend fun refreshArtworkUriFromBitmap(context: Context, bitmap: Bitmap): Uri? =
            async {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val file = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).let { File(it, "temp_artwork.jpg") }

                if (file.exists()) file.delete()

                FileOutputStream(file).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }

                return@async context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ContentValues().apply {
                            put(MediaStore.Images.Media.TITLE, file.name)
                            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                            put(MediaStore.Images.Media.DESCRIPTION, "The temp artwork for share")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                            put(MediaStore.Images.Media.DATA, file.absolutePath)
                        }
                ).apply {
                    Timber.d("inserted content uri: $this")
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

    suspend fun getBitmapFromUriString(context: Context, uriString: String): Bitmap? =
            try {
                async {
                    Glide.with(context)
                            .asBitmap().load(Uri.parse(uriString))
                            .submit().get()
                }.await()
            } catch (e: Throwable) {
                Timber.e(e)
                null
            }
}