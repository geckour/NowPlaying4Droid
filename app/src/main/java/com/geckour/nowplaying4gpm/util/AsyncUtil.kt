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
import com.geckour.nowplaying4gpm.activity.SettingsActivity
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

suspend fun getAlbumIdFromDevice(context: Context, title: String, artist: String, album: String): Long? =
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

suspend fun getArtworkUri(context: Context, track: String?, artist: String?, album: String?): Uri? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    return if (sharedPreferences.getWhetherBundleArtwork() && track != null && artist != null && album != null) {
        getArtworkUriFromDevice(context, getAlbumIdFromDevice(context, track, artist, album))
                ?: run {
                    if (sharedPreferences.getWhetherUseApi()) {
                        getBitmapFromUrl(context, getArtworkUrlFromLastFmApi(LastFmApiClient(), album, artist))?.let {
                            if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name)) {
                                try {
                                    val uriString = sharedPreferences.getString(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name, "")
                                    if (uriString.isNotBlank()) context.contentResolver.delete(Uri.parse(uriString), null, null)
                                } catch (e: Exception) {
                                    Timber.e(e)
                                }
                            }

                            getArtworkUriFromBitmap(context, it)?.apply {
                                sharedPreferences.edit()
                                        .putString(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name, this.toString())
                                        .apply()
                            }
                        }
                    } else null
                }
    } else null
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

suspend fun getArtworkUrlFromLastFmApi(client: LastFmApiClient, album: String?, artist: String?, size: Image.Size = Image.Size.MEGA): String? =
        client.searchAlbum(album, artist)?.artworks?.let { it.find { it.size == size.rawStr } ?: it.lastOrNull() }?.url

suspend fun getArtworkUriFromBitmap(context: Context, bitmap: Bitmap): Uri? =
            Uri.parse(async {
                MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        "${context.getString(R.string.app_name)}_temp",
                        null
                )
            }.await())

suspend fun getArtworkBitmap(context: Context, title: String, artist: String, album: String): Bitmap? =
        getArtworkUriFromDevice(
                context, getAlbumIdFromDevice(context, title, artist, album)
        )?.let {
            context.contentResolver.openInputStream(it).let {
                BitmapFactory.decodeStream(it, null, null).apply { it.close() }
            }
        } ?: run {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (sharedPreferences.getWhetherUseApi()) {
                getBitmapFromUrl(context,
                        getArtworkUrlFromLastFmApi(LastFmApiClient(), album, artist, Image.Size.MEDIUM))
            } else null
        }


suspend fun getBitmapFromUrl(context: Context, url: String?): Bitmap? =
        url?.let {
            try { async { Glide.with(context).asBitmap().load(it).submit().get() }.await() }
            catch (e: Throwable) {
                Timber.e(e)
                null
            }
        }