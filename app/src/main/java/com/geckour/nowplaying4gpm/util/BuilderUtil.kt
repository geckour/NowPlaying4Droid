package com.geckour.nowplaying4gpm.util

import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.View
import android.widget.RemoteViews
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider

fun getContentQuerySelection(title: String?, artist: String?, album: String?): String =
        "${MediaStore.Audio.Media.TITLE}='${title?.escapeSql()}' and ${MediaStore.Audio.Media.ARTIST}='${artist?.escapeSql()}' and ${MediaStore.Audio.Media.ALBUM}='${album?.escapeSql()}'"

suspend fun getShareWidgetViews(context: Context, summary: String?, artworkUri: Uri?): RemoteViews {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    return RemoteViews(context.packageName, R.layout.widget_share).apply {
        setTextViewText(R.id.widget_summary_share,
                summary
                        ?: context.getString(R.string.dialog_message_alert_no_metadata))

        if (sharedPreferences.getWhetherShowArtworkInWidget()) {
            val artwork = getBitmapFromUri(context, artworkUri)
            if (artwork != null) {
                setImageViewBitmap(R.id.artwork, artwork)
            } else {
                setImageViewResource(R.id.artwork, R.drawable.ic_notification)
            }
            setViewVisibility(R.id.artwork, View.VISIBLE)
        } else {
            setViewVisibility(R.id.artwork, View.GONE)
        }

        setOnClickPendingIntent(R.id.widget_share_root,
                ShareWidgetProvider.getPendingIntent(context,
                        ShareWidgetProvider.Action.SHARE))

        setOnClickPendingIntent(R.id.widget_button_setting,
                ShareWidgetProvider.getPendingIntent(context,
                        ShareWidgetProvider.Action.OPEN_SETTING))
    }
}

suspend fun getShareWidgetViews(context: Context, trackCoreElement: TrackCoreElement, artworkUri: Uri?): RemoteViews {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val summary =
            if (trackCoreElement.isAllNonNull) {
                if (trackCoreElement.isAllNonNull) {
                    sharedPreferences.getFormatPattern(context)
                            .getSharingText(trackCoreElement)
                } else {
                    sharedPreferences.getSharingText(context)
                }
            } else null

    return getShareWidgetViews(context, summary, artworkUri)
}