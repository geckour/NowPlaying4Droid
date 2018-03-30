package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import com.bumptech.glide.Glide
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
        val cacheInfo = PreferenceManager.getDefaultSharedPreferences(context).getCurrentTrackInfo()
        return if (trackCoreElement.isAllNonNull && trackCoreElement == cacheInfo?.coreElement && cacheInfo.artworkUriString != null) {
            try {
                Uri.parse(cacheInfo.artworkUriString)
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        } else {
            getBitmapFromUrl(context, getArtworkUrlFromLastFmApi(client, trackCoreElement))?.let {
                getArtworkUriFromBitmap(context, it)
            }
        }
    }

    fun getArtworkUriFromBitmap(context: Context, bitmap: Bitmap): Uri =
            context.contentResolver
                    .insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            ContentValues().apply {
                                put(MediaStore.Images.Media.TITLE, "${context.getString(R.string.app_name)}_temp")
                            }
                    ).apply {
                        context.contentResolver.openOutputStream(this).apply {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
                            this.close()
                        }
                    }


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
            uriString.let {
                try {
                    async {
                        Glide.with(context)
                                .asBitmap().load(Uri.parse(it))
                                .submit().get()
                    }.await()
                } catch (e: Throwable) {
                    Timber.e(e)
                    null
                }
            }
}