package com.geckour.nowplaying4gpm.ui

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.app.ShareCompat
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch
import timber.log.Timber

class SharingActivity : ScopedActivity() {

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

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val trackInfo = sharedPreferences.getCurrentTrackInfo()
        if (sharedPreferences.readyForShare(this, trackInfo)) {
            launch { startShare(intent.requireUnlock(), trackInfo) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
        finish()
    }

    private fun startShare(requireUnlock: Boolean, trackInfo: TrackInfo?) {
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
            val sharingText: String =
                    sharedPreferences.getSharingText(this, trackInfo) ?: return
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
                        val copyIntoClipboard = sharedPreferences.getSwitchState(
                                PrefKey.PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD)
                        if (copyIntoClipboard) {
                            getSystemService(ClipboardManager::class.java).primaryClip =
                                    ClipData.newPlainText(packageName, sharingText)
                        }

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