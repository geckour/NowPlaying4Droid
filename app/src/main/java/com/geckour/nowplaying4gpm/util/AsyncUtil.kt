package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.bumptech.glide.Glide
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.api.model.Image
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
            try { block() }
            catch (e: Exception) {
                Timber.e(e)
                onError(e)
            }
        }.apply { managerList.add(this) }

fun defLaunch(managerList: ArrayList<Job>, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit) =
        launch {
            try { block() }
            catch (e: Exception) {
                Timber.e(e)
                onError(e)
            }
        }.apply { managerList.add(this) }

suspend fun getAlbumIdFromDevice(context: Context, title: String?, artist: String?, album: String?): Long? =
        if (title == null || artist == null || album == null) null
        else {
            async {
                val cursor = context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                        getContentQuerySelection(title, artist, album),
                        null,
                        null
                )

                return@async (if (cursor.moveToNext()) {
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                } else null).apply { cursor.close() }
            }.await()
        }

suspend fun getArtworkUriFromDevice(context: Context, albumId: Long?): Uri? =
        albumId?.let { async {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it).let {
                try {
                    context.contentResolver.openInputStream(it).close()
                    it
                } catch (e: Throwable) {
                    Timber.e(e)
                    null
                }
            }
        }.await() }

suspend fun getArtworkUrlFromLastFmApi(client: LastFmApiClient, album: String?, artist: String?, size: Image.Size = Image.Size.EX_LARGE): String? =
        client.searchAlbum(album, artist)?.artworks?.let { it.find { it.size == size.rawStr } ?: it.lastOrNull() }?.url

suspend fun getAlbumArtUriFromBitmap(context: Context, bitmap: Bitmap): Uri? =
            Uri.parse(async {
                MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        "${context.getString(R.string.app_name)}_temp",
                        null
                )
            }.await())

suspend fun getBitmapFromUrl(context: Context, url: String?): Bitmap? =
        url?.let {
            try { async { Glide.with(context).asBitmap().load(it).submit().get() }.await() }
            catch (e: Throwable) {
                Timber.e(e)
                null
            }
        }