package com.geckour.nowplaying4gpm.activity

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ShareCompat
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.util.getTempArtworkUri
import com.geckour.nowplaying4gpm.util.getWhetherBundleArtwork
import com.geckour.nowplaying4gpm.util.ui
import kotlinx.coroutines.experimental.Job

class SharingActivity: Activity() {

    enum class IntentRequestCode {
        SHARE,
        CALLBACK
    }

    enum class ArgKey {
        TEXT
    }

    companion object {
        fun getIntent(context: Context, text: String): Intent =
                Intent(context, SharingActivity::class.java).apply {
                    putExtra(ArgKey.TEXT.name, text)
                }
    }

    private val jobs: ArrayList<Job> = ArrayList()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.apply {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@SharingActivity)

            val sharingText: String =
                    if (hasExtra(ArgKey.TEXT.name)) getStringExtra(ArgKey.TEXT.name)
                    else return
            val artworkUri =
                    if (sharedPreferences.getWhetherBundleArtwork())
                        sharedPreferences.getTempArtworkUri()
                    else null

            ui(jobs) { startShare(sharingText, artworkUri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
        finish()
    }

    private fun startShare(text: String, stream: Uri?) =
            ShareCompat.IntentBuilder.from(this@SharingActivity)
                    .setChooserTitle(R.string.share_title)
                    .setText(text)
                    .also {
                        stream?.apply { it.setStream(this).setType("image/jpeg") }
                                ?: it.setType("text/plain")
                    }
                    .createChooserIntent()
                    .apply {
                        PendingIntent.getActivity(
                                this@SharingActivity,
                                IntentRequestCode.SHARE.ordinal,
                                this@apply,
                                PendingIntent.FLAG_UPDATE_CURRENT).send()
                    }
}