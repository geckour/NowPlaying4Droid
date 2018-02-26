package com.geckour.nowplaying4gpm.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ShareCompat
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.LastFmApiClient
import com.geckour.nowplaying4gpm.util.*
import com.google.gson.Gson
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class SharingActivity: Activity() {

    enum class IntentRequestCode {
        SHARE,
        CALLBACK
    }

    enum class ArgKey {
        TRACK,
        ARTIST,
        ALBUM
    }

    companion object {
        fun createIntent(context: Context, track: String?, artist: String?, album: String?): Intent =
                Intent(context, SharingActivity::class.java).apply {
                    if (track != null) putExtra(ArgKey.TRACK.name, track)
                    if (artist != null) putExtra(ArgKey.ARTIST.name, artist)
                    if (album != null) putExtra(ArgKey.ALBUM.name, album)
                }
    }

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }
    private val jobs: ArrayList<Job> = ArrayList()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.let {
            val track: String? = if (it.hasExtra(ArgKey.TRACK.name)) it.getStringExtra(ArgKey.TRACK.name) else null
            val artist: String? = if (it.hasExtra(ArgKey.ARTIST.name)) it.getStringExtra(ArgKey.ARTIST.name) else null
            val album: String? = if (it.hasExtra(ArgKey.ALBUM.name)) it.getStringExtra(ArgKey.ALBUM.name) else null
            if (track == null || artist == null || album == null) return

            val sharingText =
                    sharedPreferences.getString(
                            SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                            getString(R.string.default_sharing_text_pattern))
                            .getSharingText(track, artist, album)

            ui(jobs) {
                val artworkUri =
                        getArtworkUriFromDevice(getAlbumIdFromDevice(this@SharingActivity, track, artist, album)).let {
                            try {
                                contentResolver.openInputStream(it).close()
                                it
                            } catch (e: Throwable) {
                                Timber.e(e)

                                getBitmapFromUrl(
                                        this@SharingActivity,
                                        getArtworkUrlFromLastFmApi(LastFmApiClient(), album, artist)
                                )?.let {
                                    if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name)) {
                                        try {
                                            val tempUri: Uri = Gson().fromJson(sharedPreferences.getString(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name, ""), Uri::class.java)
                                            async { contentResolver.delete(tempUri, null, null) }.await()
                                        } catch (e: Exception) { Timber.e(e) }
                                    }
                                    val uri = getAlbumArtUriFromBitmap(this@SharingActivity, it)
                                    uri?.apply { sharedPreferences.edit().putString(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name, Gson().toJson(this)).apply() }
                                }
                            }
                        }

                ShareCompat.IntentBuilder.from(this@SharingActivity)
                        .setChooserTitle(R.string.share_title)
                        .setText(sharingText)
                        .also {
                            artworkUri?.apply {
                                it.setStream(this)
                                it.setType("image/jpeg")
                            } ?: run { it.setText("text/plain") }
                        }.createChooserIntent()
                        .apply { startActivityForResult(this, IntentRequestCode.SHARE.ordinal) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            IntentRequestCode.SHARE.ordinal -> finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        jobs.cancelAll()
    }
}