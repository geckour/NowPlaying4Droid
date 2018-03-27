package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import com.bumptech.glide.Glide
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.model.Image
import com.geckour.nowplaying4gpm.domain.model.ArtworkInfo
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import kotlin.coroutines.experimental.CoroutineContext

fun <T> async(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> T) =
        kotlinx.coroutines.experimental.async(context, block = block)

fun ui(managerList: ArrayList<Job>, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit) =
        launch(UI) {
            try {
                block()
            } catch (e: Exception) {
                Timber.e(e)
                onError(e)
            }
        }.apply { managerList.add(this) }

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

    suspend fun getArtworkUri(context: Context, client: LastFmApiClient, trackInfo: TrackInfo? = null): Uri? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val cacheInfo = sharedPreferences.getCurrentTrackInfo()

        try {
            if (trackInfo?.artwork?.artworkUriString != null)
                return Uri.parse(trackInfo.artwork.artworkUriString)
            else if (trackInfo?.coreElement?.isAllNonNull == true
                    && trackInfo.coreElement == cacheInfo?.artwork?.trackCoreElement)
                return Uri.parse(cacheInfo.artwork.artworkUriString)
        } catch (e: Exception) {
            Timber.e(e)
        }

        val coreElement =
                when {
                    trackInfo?.coreElement?.isAllNonNull == true -> trackInfo.coreElement
                    cacheInfo?.coreElement?.isAllNonNull == true -> cacheInfo.coreElement
                    else -> null
                } ?: return null

        var fromContentResolver = false

        return (getArtworkUriFromDevice(context, coreElement)?.apply { fromContentResolver = true }
                ?: if (sharedPreferences.getWhetherUseApi())
                    getArtworkUriFromLastFmApi(context, client, coreElement)
                else null).apply {
            sharedPreferences.setCurrentArtworkInfo(
                    ArtworkInfo(
                            this?.toString(),
                            coreElement,
                            fromContentResolver)
            )
        }
    }

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

    private suspend fun getArtworkUriFromDevice(context: Context, trackCoreElement: TrackCoreElement): Uri? =
            getArtworkUriFromDevice(context, getAlbumIdFromDevice(context, trackCoreElement))

    private suspend fun getArtworkUrlFromLastFmApi(client: LastFmApiClient, trackCoreElement: TrackCoreElement, size: Image.Size = Image.Size.MEGA): String? =
            if (trackCoreElement.album == null && trackCoreElement.artist == null) null
            else client.searchAlbum(
                    trackCoreElement.album,
                    trackCoreElement.artist)?.artworks?.let {
                it.find { it.size == size.rawStr } ?: it.lastOrNull()
            }?.url

    private suspend fun getArtworkUriFromLastFmApi(context: Context, client: LastFmApiClient, trackCoreElement: TrackCoreElement): Uri? =
            getBitmapFromUrl(context, getArtworkUrlFromLastFmApi(client, trackCoreElement))?.let {
                getArtworkUriFromBitmap(context, it)?.apply {
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .deleteTempArtwork(context)
                }
            }

    private suspend fun getArtworkUriFromBitmap(context: Context, bitmap: Bitmap): Uri? =
            Uri.parse(async {
                MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        "${context.getString(R.string.app_name)}_temp",
                        null
                )
            }.await())

    suspend fun getArtworkBitmap(context: Context, client: LastFmApiClient, trackCoreElement: TrackCoreElement): Bitmap? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val coreElement =
                (if (trackCoreElement.isAllNonNull.not()) sharedPreferences.getCurrentTrackInfo()?.coreElement
                else trackCoreElement)
                        ?: return null

        return getArtworkUriFromDevice(context, coreElement)?.let {
            context.contentResolver.openInputStream(it).let {
                BitmapFactory.decodeStream(it, null, null).apply { it.close() }
            }
        } ?: run {
            if (sharedPreferences.getWhetherUseApi()) {
                getBitmapFromUrl(context, getArtworkUrlFromLastFmApi(client, coreElement, Image.Size.MEDIUM))
            } else null
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
}