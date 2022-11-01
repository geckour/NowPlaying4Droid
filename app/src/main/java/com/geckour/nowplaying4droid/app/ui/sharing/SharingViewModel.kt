package com.geckour.nowplaying4droid.app.ui.sharing

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.domain.model.TrackInfo
import com.geckour.nowplaying4droid.app.util.PrefKey
import com.geckour.nowplaying4droid.app.util.getSharingText
import com.geckour.nowplaying4droid.app.util.getSwitchState
import com.geckour.nowplaying4droid.app.util.getTempArtworkUri
import com.geckour.nowplaying4droid.app.util.readyForShare
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber

class SharingViewModel : ViewModel() {

    fun startShare(
        context: Context,
        sharedPreferences: SharedPreferences,
        trackInfo: TrackInfo?
    ) {
        if (sharedPreferences.readyForShare(context, trackInfo).not()) return

        FirebaseAnalytics.getInstance(context.applicationContext)
            .logEvent(
                FirebaseAnalytics.Event.SELECT_CONTENT,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_NAME, "Invoked share action")
                }
            )

        val sharingText: String =
            sharedPreferences.getSharingText(context, trackInfo) ?: return
        val artworkUri =
            if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK))
                sharedPreferences.getTempArtworkUri(context)
            else null
        Timber.d("sharingText: $sharingText, artworkUri: $artworkUri")

        val copyIntoClipboard = sharedPreferences
            .getSwitchState(PrefKey.PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD)
        if (copyIntoClipboard) {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText(context.packageName, sharingText))
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, sharingText)
            artworkUri?.let {
                putExtra(Intent.EXTRA_STREAM, it)
                setType("image/png")
            } ?: run { setType("text/plain") }
        }.let {
            if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_USE_SIMPLE_SHARE)) it
            else Intent.createChooser(it, context.getString(R.string.share_title))
        }

        PendingIntent.getActivity(
            context,
            SharingActivity.IntentRequestCode.SHARE.ordinal,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ).send()
    }
}