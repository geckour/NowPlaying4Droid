package com.geckour.nowplaying4gpm.ui.sharing

import android.app.Activity
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber

class SharingViewModel : ViewModel() {
    fun startShare(
        activity: Activity,
        sharedPreferences: SharedPreferences,
        requireUnlock: Boolean,
        trackInfo: TrackInfo?
    ) {

        if (sharedPreferences.readyForShare(activity, trackInfo).not()) return

        FirebaseAnalytics.getInstance(activity.applicationContext)
            .logEvent(
                FirebaseAnalytics.Event.SELECT_CONTENT,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked share action")
                }
            )

        val keyguardManager =
            try {
                activity.getSystemService(KeyguardManager::class.java)
            } catch (t: Throwable) {
                Timber.e(t)
                null
            }

        if (requireUnlock.not() || keyguardManager?.isDeviceLocked != true) {
            val sharingText: String =
                sharedPreferences.getSharingText(activity, trackInfo) ?: return
            val artworkUri =
                if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK))
                    sharedPreferences.getTempArtworkUri(activity)
                else null
            Timber.d("sharingText: $sharingText, artworkUri: $artworkUri")

            ShareCompat.IntentBuilder.from(activity)
                .setChooserTitle(R.string.share_title)
                .setText(sharingText)
                .also {
                    artworkUri?.apply { it.setStream(this).setType("image/jpeg") }
                        ?: it.setType("text/plain")
                }
                .createChooserIntent()
                .apply {
                    val copyIntoClipboard = sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD
                    )
                    if (copyIntoClipboard) {
                        activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                            ClipData.newPlainText(activity.packageName, sharingText)
                        )
                    }

                    PendingIntent.getActivity(
                        activity,
                        SharingActivity.IntentRequestCode.SHARE.ordinal,
                        this,
                        PendingIntent.FLAG_CANCEL_CURRENT
                    ).send()
                }
        }
    }
}