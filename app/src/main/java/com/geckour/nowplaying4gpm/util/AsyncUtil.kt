package com.geckour.nowplaying4gpm.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.bumptech.glide.Glide
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.ITunesApiClient

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

suspend fun getAlbumArtUriFromDevice(albumId: Long?): Uri? =
        async { albumId?.let { ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it) } }.await()

suspend fun getAlbumArtUrlFromITunesApi(client: ITunesApiClient, title: String?, artist: String?, album: String?): String? =
        client.searchAlbum(title, artist, album)
                .resultItems.firstOrNull()?.artworkUrl

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
        async { Glide.with(context).asBitmap().load(url).submit().get() }.await()