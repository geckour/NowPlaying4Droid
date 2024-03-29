package com.geckour.nowplaying4droid.app.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.geckour.nowplaying4droid.app.service.NotificationService
import com.geckour.nowplaying4droid.app.ui.settings.SettingsActivity
import com.geckour.nowplaying4droid.app.ui.sharing.SharingActivity

fun getShareIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context.applicationContext,
        (1e+13.toLong() + System.currentTimeMillis()).hashCode(),
        SharingActivity.getIntent(context),
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

fun getSettingsIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context.applicationContext,
        (2e+13.toLong() + System.currentTimeMillis()).hashCode(),
        SettingsActivity.getIntent(context),
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

fun getClearTrackDetailPendingIntent(context: Context): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        (3e+13.toLong() + System.currentTimeMillis()).hashCode(),
        Intent(NotificationService.ACTION_CLEAR_TRACK_INFO),
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )