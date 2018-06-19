package com.geckour.nowplaying4gpm.ui

import android.app.Activity
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ShareCompat
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class SharingActivity : Activity() {

    enum class IntentRequestCode {
        SHARE
    }

    companion object {
        private const val ARGS_KEY_REQUIRE_UNLOCK = "args_key_require_unlock"

        fun getIntent(context: Context, requireUnlock: Boolean = true): Intent =
                Intent(context, SharingActivity::class.java).apply {
                    putExtra(ARGS_KEY_REQUIRE_UNLOCK, requireUnlock)
                }
    }

    private val jobs: ArrayList<Job> = ArrayList()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setCrashlytics()

        ui(jobs) { startShare(intent.requireUnlock()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
        finish()
    }

    private fun startShare(requireUnlock: Boolean) {
        FirebaseAnalytics.getInstance(application)
                .logEvent(
                        FirebaseAnalytics.Event.SELECT_CONTENT,
                        Bundle().apply {
                            putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked share action")
                        }
                )

        val keyguardManager =
                try {
                    getSystemService(KeyguardManager::class.java)
                } catch (t: Throwable) {
                    Timber.e(t)
                    null
                }

        if (requireUnlock.not() || keyguardManager?.isDeviceLocked != true) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

            val sharingText: String =
                    sharedPreferences.getSharingText(this) ?: return
            val artworkUri =
                    if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK))
                        sharedPreferences.getTempArtworkUri(this)
                    else null
            Timber.d("sharingText: $sharingText, artworkUri: $artworkUri")

            ShareCompat.IntentBuilder.from(this@SharingActivity)
                    .setChooserTitle(R.string.share_title)
                    .setText(sharingText)
                    .also {
                        artworkUri?.apply { it.setStream(this).setType("image/jpeg") }
                                ?: it.setType("text/plain")
                    }
                    .createChooserIntent()
                    .apply {
                        PendingIntent.getActivity(
                                this@SharingActivity,
                                IntentRequestCode.SHARE.ordinal,
                                this@apply,
                                PendingIntent.FLAG_CANCEL_CURRENT
                        ).send()
                    }
        }
    }

    private fun Intent?.requireUnlock(): Boolean {
        val default = true
        if (this == null) return default

        return if (this.hasExtra(ARGS_KEY_REQUIRE_UNLOCK))
            this.getBooleanExtra(ARGS_KEY_REQUIRE_UNLOCK, default)
        else default
    }
}