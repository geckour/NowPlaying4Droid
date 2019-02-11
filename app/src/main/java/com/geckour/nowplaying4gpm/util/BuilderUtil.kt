package com.geckour.nowplaying4gpm.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.settings.SettingsActivity
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.sys1yagi.mastodon4j.api.entity.Status

fun getContentQuerySelection(title: String?, artist: String?, album: String?): String =
        "${MediaStore.Audio.Media.TITLE}='${title?.escapeSql()}' and ${MediaStore.Audio.Media.ARTIST}='${artist?.escapeSql()}' and ${MediaStore.Audio.Media.ALBUM}='${album?.escapeSql()}'"

suspend fun getShareWidgetViews(context: Context, isMin: Boolean = false, trackInfo: TrackInfo? = null): RemoteViews {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    return RemoteViews(context.packageName, R.layout.widget_share).apply {
        val info = trackInfo ?: sharedPreferences.getCurrentTrackInfo()
        val summary = info?.let {
            sharedPreferences.getFormatPattern(context)
                    .getSharingText(info, sharedPreferences.getFormatPatternModifiers())
                    ?.foldBreak()
        }

        setTextViewText(R.id.widget_summary_share,
                summary ?: context.getString(R.string.dialog_message_alert_no_metadata))

        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET)
                && isMin.not()) {
            val artwork = getBitmapFromUri(context, info?.artworkUriString?.getUri())?.let {
                Bitmap.createScaledBitmap(it, 600, 600, false)
            }
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

        val packageName =
                if (sharedPreferences.getSwitchState(
                                PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK))
                    info?.playerPackageName
                else null
        val launchIntent = packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
        setOnClickPendingIntent(R.id.artwork,
                if (launchIntent != null) {
                    PendingIntent.getActivity(context, 0,
                            launchIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT)
                } else {
                    ShareWidgetProvider.getPendingIntent(context,
                            ShareWidgetProvider.Action.SHARE)
                }
        )

        setOnClickPendingIntent(R.id.widget_button_setting,
                ShareWidgetProvider.getPendingIntent(context,
                        ShareWidgetProvider.Action.OPEN_SETTING))
    }
}

suspend fun getNotification(context: Context, trackInfo: TrackInfo): Notification? {
    if (trackInfo == TrackInfo.empty) return null

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val notificationBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
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
                sharedPreferences.getFormatPattern(context)
                        .getSharingText(trackInfo, sharedPreferences.getFormatPatternModifiers())
                        ?.foldBreak()

        val thumb =
                if (sharedPreferences.getSwitchState(
                                PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION)) {
                    trackInfo.artworkUriString?.let {
                        getBitmapFromUriString(context, it)
                    }
                } else null

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
            style = Notification.DecoratedMediaCustomViewStyle()
            addAction(actionOpenSetting)
        }
        thumb?.apply {
            if (Build.VERSION.SDK_INT >= 26
                    && sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG)) {
                setColorized(true)
            }

            val color = Palette.from(this)
                    .maximumColorCount(12)
                    .generate()
                    .getOptimizedColor(context)
            setColor(color)
        }
    }.build()
}

suspend fun getNotification(context: Context, status: Status): Notification? {
    val notificationBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name)
            else Notification.Builder(context)

    return notificationBuilder.apply {
        val notificationText = Html.fromHtml(status.content, Html.FROM_HTML_MODE_COMPACT).toString()

        val thumb = getBitmapFromUrl(context, status.mediaAttachments.firstOrNull()?.url)

        setSmallIcon(R.drawable.ic_notification_notify)
        setLargeIcon(thumb)
        setContentTitle(context.getString(R.string.notification_title_notify_success_mastodon))
        setContentText(notificationText)
        if (Build.VERSION.SDK_INT >= 24) {
            style = Notification.DecoratedMediaCustomViewStyle()
        }
        thumb?.apply {
            val color = Palette.from(this)
                    .maximumColorCount(24)
                    .generate()
                    .getOptimizedColor(context)
            setColor(color)
        }
    }.build()
}