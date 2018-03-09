package com.geckour.nowplaying4gpm.activity

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ShareCompat
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.experimental.Job

class SharingActivity: Activity() {

    enum class IntentRequestCode {
        SHARE,
        CALLBACK
    }

    enum class ArgKey {
        TEXT,
        ARTWORK_URI
    }

    companion object {
        fun getIntent(context: Context, text: String, artworkUri: Uri?): Intent =
                Intent(context, SharingActivity::class.java).apply {
                    putExtra(ArgKey.TEXT.name, text)
                    if (artworkUri != null) putExtra(ArgKey.ARTWORK_URI.name, artworkUri)
                }
    }

    private lateinit var analytics: FirebaseAnalytics
    private val jobs: ArrayList<Job> = ArrayList()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.apply {
            val sharingText: String =
                    if (hasExtra(ArgKey.TEXT.name)) getStringExtra(ArgKey.TEXT.name)
                    else return
            val artworkUri: Uri? =
                    if (hasExtra(ArgKey.ARTWORK_URI.name)) getParcelableExtra(ArgKey.ARTWORK_URI.name)
                    else null

            ui(jobs) { startShare(sharingText, artworkUri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG.not()) Fabric.with(this, Crashlytics())
        analytics = FirebaseAnalytics.getInstance(this)


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