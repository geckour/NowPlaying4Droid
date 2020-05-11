package com.geckour.nowplaying4gpm.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.SpotifySearchResult
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.settings.SettingsActivity
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.sys1yagi.mastodon4j.api.entity.Status
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.PrintWriter
import java.io.StringWriter

val json: Json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true, isLenient = true))

const val contentQuerySelection: String =
    "${MediaStore.Audio.Media.TITLE}=? and ${MediaStore.Audio.Media.ARTIST}=? and ${MediaStore.Audio.Media.ALBUM}=?"

val TrackInfo.TrackCoreElement.contentQueryArgs: Array<String>
    get() =
        arrayOf(requireNotNull(title), requireNotNull(artist), requireNotNull(album))

suspend fun getShareWidgetViews(
    context: Context,
    blockCount: Int = 0,
    trackInfo: TrackInfo? = null
): RemoteViews = RemoteViews(context.packageName, R.layout.widget_share).apply {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val info = trackInfo ?: sharedPreferences.getCurrentTrackInfo()
    val summary =
        sharedPreferences.getFormatPattern(context)
            .getSharingText(info, sharedPreferences.getFormatPatternModifiers())
            ?.foldBreak()

    setTextViewText(
        R.id.widget_summary_share,
        summary ?: context.getString(R.string.dialog_message_alert_no_metadata)
    )

    if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET)
        && blockCount > 1
    ) {
        val artwork = info?.artworkUriString?.let {
            context.getBitmapFromUriString(it, maxHeight = 500)
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

    setViewVisibility(
        R.id.widget_button_clear,
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_SHOW_CLEAR_BUTTON_IN_WIDGET)
            && blockCount > 2
        ) View.VISIBLE
        else View.GONE
    )

    setOnClickPendingIntent(
        R.id.widget_share_root,
        ShareWidgetProvider.getPendingIntent(
            context,
            ShareWidgetProvider.Action.SHARE
        )
    )

    val packageName =
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK))
            info?.playerPackageName
        else null
    val launchIntent = packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
    setOnClickPendingIntent(
        R.id.artwork,
        if (launchIntent != null) {
            PendingIntent.getActivity(
                context, 0,
                launchIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        } else {
            ShareWidgetProvider.getPendingIntent(
                context,
                ShareWidgetProvider.Action.SHARE
            )
        }
    )

    setOnClickPendingIntent(
        R.id.widget_button_setting,
        ShareWidgetProvider.getPendingIntent(
            context,
            ShareWidgetProvider.Action.OPEN_SETTING
        )
    )

    setOnClickPendingIntent(
        R.id.widget_button_clear,
        NotificationService.getClearTrackInfoPendingIntent(context)
    )
}

suspend fun getNotification(context: Context, trackInfo: TrackInfo?): Notification? {
    trackInfo ?: return null

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val notificationBuilder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(
                context,
                NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name
            )
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
                    Icon.createWithResource(
                        context,
                        R.drawable.ic_settings
                    ),
                    context.getString(R.string.action_open_pref),
                    it
                ).build()
            }
        val actionClear =
            Notification.Action.Builder(
                Icon.createWithResource(
                    context,
                    R.drawable.ic_clear
                ),
                context.getString(R.string.action_clear_notification),
                NotificationService.getClearTrackInfoPendingIntent(context)
            ).build()
        val notificationText =
            sharedPreferences.getFormatPattern(context)
                .getSharingText(trackInfo, sharedPreferences.getFormatPatternModifiers())
                ?.foldBreak()

        val thumb =
            if (sharedPreferences.getSwitchState(
                    PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION
                )
            ) {
                trackInfo.artworkUriString?.let { context.getBitmapFromUriString(it) }
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
            addAction(actionClear)
        }
        thumb?.apply {
            if (Build.VERSION.SDK_INT >= 26
                && sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG)
            ) {
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
            Notification.Builder(
                context,
                NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name
            )
        else Notification.Builder(context)

    return notificationBuilder.apply {
        val notificationText = Html.fromHtml(status.content, Html.FROM_HTML_MODE_COMPACT).toString()

        val thumb =
            status.mediaAttachments.firstOrNull()?.url?.let { context.getBitmapFromUriString(it) }

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

fun getNotification(
    context: Context,
    spotifySearchResult: SpotifySearchResult
): Notification {
    val notificationBuilder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(
                context,
                NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name
            )
        else Notification.Builder(context)
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    return notificationBuilder
        .setSmallIcon(
            if (spotifySearchResult is SpotifySearchResult.Failure) R.drawable.ic_clear
            else R.drawable.ic_debug
        )
        .setStyle(Notification.BigTextStyle())
        .setContentTitle(spotifySearchResult.query)
        .setContentText(
            when (spotifySearchResult) {
                is SpotifySearchResult.Success -> spotifySearchResult.data.sharingUrl
                is SpotifySearchResult.Failure -> spotifySearchResult.cause.let { t ->
                    StringWriter().use {
                        "expiredAt: ${sharedPreferences.getSpotifyUserInfo()?.refreshTokenExpiredAt}\n${it.apply {
                            t.printStackTrace(
                                PrintWriter(this)
                            )
                        }}"
                    }
                }
            }
        )
        .build()
}