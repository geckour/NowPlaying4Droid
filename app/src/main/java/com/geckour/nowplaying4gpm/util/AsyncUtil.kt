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
import timber.log.Timber

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

suspend fun getArtworkUriFromDevice(albumId: Long?): Uri? =
        async { albumId?.let { ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it) } }.await()

suspend fun getArtworkUrlFromLastFmApi(client: LastFmApiClient, album: String?, artist: String?): String? =
        client.searchAlbum(album, artist)?.artworks?.let { it.find { it.size == Image.Size.EX_LARGE.rawStr } ?: it.lastOrNull() }?.url

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
        if (url == null) null
        else {
            try { async { Glide.with(context).asBitmap().load(url).submit().get() }.await() }
            catch (e: Throwable) {
                Timber.e(e)
                null
            }
        }