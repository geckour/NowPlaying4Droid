package com.geckour.nowplaying4gpm.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v7.graphics.Palette
import android.view.View
import android.widget.RemoteViews
import com.geckour.nowplaying4gpm.App
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.service.NotificationService

fun getContentQuerySelection(title: String?, artist: String?, album: String?): String =
        "${MediaStore.Audio.Media.TITLE}='${title?.escapeSql()}' and ${MediaStore.Audio.Media.ARTIST}='${artist?.escapeSql()}' and ${MediaStore.Audio.Media.ALBUM}='${album?.escapeSql()}'"

private suspend fun getShareWidgetViews(context: Context, id: Int?, summary: String?, artworkUri: Uri?): RemoteViews {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    return RemoteViews(context.packageName, R.layout.widget_share).apply {
        setTextViewText(R.id.widget_summary_share,
                summary
                        ?: context.getString(R.string.dialog_message_alert_no_metadata))

        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET) && (id == null || sharedPreferences.getWidgetState(id) != WidgetState.MIN)) {
            val artwork = getBitmapFromUri(context, artworkUri)
            if (summary != null && artwork != null) {
                setImageViewBitmap(R.id.artwork, artwork)
            } else {
                setImageViewResource(R.id.artwork, R.drawable.ic_placeholder)
            }
            setViewVisibility(R.id.artwork, View.VISIBLE)
        } else {
            setViewVisibility(R.id.artwork, View.GONE)
        }

        setOnClickPendingIntent(R.id.widget_share_root,
                ShareWidgetProvider.getPendingIntent(context,
                        ShareWidgetProvider.Action.SHARE))


        setOnClickPendingIntent(R.id.artwork,
                if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK))
                    PendingIntent.getActivity(context,
                            0,
                            context.packageManager.getLaunchIntentForPackage(App.PACKAGE_NAME_GPM),
                            PendingIntent.FLAG_UPDATE_CURRENT)
                else
                    ShareWidgetProvider.getPendingIntent(context,
                            ShareWidgetProvider.Action.SHARE)
        )

        setOnClickPendingIntent(R.id.widget_button_setting,
                ShareWidgetProvider.getPendingIntent(context,
                        ShareWidgetProvider.Action.OPEN_SETTING))
    }
}

suspend fun getShareWidgetViews(context: Context, id: Int?, trackCoreElement: TrackCoreElement, artworkUri: Uri?): RemoteViews {
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

    return getShareWidgetViews(context, id, summary, artworkUri)
}

suspend fun getNotification(context: Context, trackInfo: TrackInfo): Notification? {
    if (trackInfo.coreElement.isAllNonNull.not()) return null

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val notificationBuilder =
            if (Build.VERSION.SDK_INT >= 26)
                Notification.Builder(context, NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name)
            else Notification.Builder(context)

    return notificationBuilder.apply {
        val actionOpenSetting =
                PendingIntent.getActivity(
                        context,
                        0,
                        SettingsActivity.getIntent(context),
                        PendingIntent.FLAG_CANCEL_CURRENT
                ).let {
                    Notification.Action.Builder(
                            Icon.createWithResource(context,
                                    R.drawable.ic_settings_black_24px),
                            context.getString(R.string.action_open_pref),
                            it
                    ).build()
                }
        val notificationText =
                sharedPreferences.getString(
                        PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                        context.getString(R.string.default_sharing_text_pattern))
                        .getSharingText(trackInfo.coreElement)

        val thumb =
                trackInfo.artworkUriString?.let {
                    getBitmapFromUriString(context, it)
                }

        setSmallIcon(R.drawable.ic_notification)
        setLargeIcon(thumb)
        setContentTitle(context.getString(R.string.notification_title))
        setContentText(notificationText)
        setContentIntent(
                PendingIntent.getActivity(
                        context,
                        0,
                        SharingActivity.getIntent(context),
                        PendingIntent.FLAG_CANCEL_CURRENT
                )
        )
        setOngoing(true)
        if (Build.VERSION.SDK_INT >= 24) {
            setStyle(Notification.DecoratedMediaCustomViewStyle())
            addAction(actionOpenSetting)
        }
        thumb?.apply {
            if (Build.VERSION.SDK_INT >= 26
                    && sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG)) {
                setColorized(true)
            }

            val color =
                    Palette.from(this)
                            .maximumColorCount(12)
                            .generate()
                            .getOptimizedColor(context)
            setColor(color)
        }
    }.build()
}