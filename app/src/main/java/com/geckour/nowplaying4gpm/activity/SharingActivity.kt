package com.geckour.nowplaying4gpm.activity

import android.app.Activity
import android.app.PendingIntent
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
                val artworkUri = getArtworkUri(track, artist, album)

                startShare(sharingText, artworkUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
        finish()
    }

    private suspend fun getArtworkUri(track: String?, artist: String?, album: String?): Uri? =
            if (sharedPreferences.getWhetherBundleArtwork()) {
                getArtworkUriFromDevice(this@SharingActivity, getAlbumIdFromDevice(this@SharingActivity, track, artist, album))
                        ?: getBitmapFromUrl(this@SharingActivity, getArtworkUrlFromLastFmApi(LastFmApiClient(), album, artist))?.let {
                            if (sharedPreferences.contains(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name)) {
                                try {
                                    val uriString = sharedPreferences.getString(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name, "")
                                    if (uriString.isNotBlank()) contentResolver.delete(Uri.parse(uriString), null, null)
                                } catch (e: Exception) { Timber.e(e) }
                            }

                            getAlbumArtUriFromBitmap(this@SharingActivity, it)?.apply {
                                sharedPreferences.edit()
                                        .putString(SettingsActivity.PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI.name, this.toString())
                                        .apply()
                            }
                        }
            } else null

    private fun startShare(text: String, stream: Uri?) =
            ShareCompat.IntentBuilder.from(this@SharingActivity)
                    .setChooserTitle(R.string.share_title)
                    .setText(text).also { stream?.apply { it.setStream(this).setType("image/jpeg") } ?: run { it.setType("text/plain") } }
                    .createChooserIntent()
                    .apply {
                        PendingIntent.getActivity(
                                this@SharingActivity,
                                IntentRequestCode.SHARE.ordinal,
                                this@apply,
                                PendingIntent.FLAG_UPDATE_CURRENT).send()
                    }
}