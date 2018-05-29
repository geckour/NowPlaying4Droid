package com.geckour.nowplaying4gpm.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ShareCompat
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.experimental.Job

class SharingActivity : Activity() {

    enum class IntentRequestCode {
        SHARE,
        CALLBACK
    }

    companion object {
        fun getIntent(context: Context): Intent =
                Intent(context, SharingActivity::class.java)
    }

    private val jobs: ArrayList<Job> = ArrayList()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setCrashlytics()

        intent?.apply {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@SharingActivity)

            val sharingText: String =
                    sharedPreferences.getSharingText(this@SharingActivity) ?: return
            val artworkUri =
                    if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK))
                        sharedPreferences.getTempArtworkUri(this@SharingActivity)
                    else null

            ui(jobs) { startShare(sharingText, artworkUri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
        finish()
    }

    private fun startShare(text: String, stream: Uri?) {
        FirebaseAnalytics.getInstance(application)
                .logEvent(
                        FirebaseAnalytics.Event.SELECT_CONTENT,
                        Bundle().apply {
                            putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked share action")
                        }
                )

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
}