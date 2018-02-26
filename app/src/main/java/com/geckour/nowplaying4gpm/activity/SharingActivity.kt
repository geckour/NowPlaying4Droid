package com.geckour.nowplaying4gpm.activity

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.ITunesApiClient
import com.geckour.nowplaying4gpm.util.*
import kotlinx.coroutines.experimental.Job
import timber.log.Timber
import java.io.FileNotFoundException

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

        var tempArtworkUri: Uri? = null
    }

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }
    private val jobs: ArrayList<Job> = ArrayList()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.let {
            ui(jobs) {
                val track: String? = if (it.hasExtra(ArgKey.TRACK.name)) it.getStringExtra(ArgKey.TRACK.name) else null
                val artist: String? = if (it.hasExtra(ArgKey.ARTIST.name)) it.getStringExtra(ArgKey.ARTIST.name) else null
                val album: String? = if (it.hasExtra(ArgKey.ALBUM.name)) it.getStringExtra(ArgKey.ALBUM.name) else null
                if (track == null || artist == null || album == null) return@ui

                val sharingText =
                        sharedPreferences.getString(
                                SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                                getString(R.string.default_sharing_text_pattern))
                                .getSharingText(track, artist, album)

                val artworkUri =
                        getAlbumArtUriFromDevice(getAlbumIdFromDevice(this@SharingActivity, track, artist, album)).let {
                            try {
                                contentResolver.openInputStream(it).close()
                                it
                            } catch (e: FileNotFoundException) {
                                Timber.e(e)

                                getBitmapFromUrl(
                                        this@SharingActivity,
                                        getAlbumArtUrlFromITunesApi(ITunesApiClient(), track, artist, album)
                                )?.let {
                                    tempArtworkUri?.apply { async { contentResolver.delete(this@apply, null, null) }.await() }
                                    tempArtworkUri = getAlbumArtUriFromBitmap(this@SharingActivity, it)
                                    tempArtworkUri
                                }
                            }
                        }

                Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_TITLE, getString(R.string.share_title))
                        .putExtra(Intent.EXTRA_TEXT, sharingText)
                        .also {
                            artworkUri?.apply {
                                it.putExtra(Intent.EXTRA_STREAM, artworkUri)
                                it.type = "image/jpeg"
                            } ?: run { it.type = "text/plain" }
                        }.apply {
                            PendingIntent.getActivity(
                                    this@SharingActivity,
                                    IntentRequestCode.SHARE.ordinal,
                                    Intent.createChooser(this, getString(R.string.share_title)),
                                    PendingIntent.FLAG_CANCEL_CURRENT
                            ).send()
                            finish()
                        }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        jobs.cancelAll()
    }
}