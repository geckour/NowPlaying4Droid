package com.geckour.nowplaying4gpm.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.ui.settings.SettingsActivity
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getShareWidgetViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class ShareWidgetProvider : AppWidgetProvider(), CoroutineScope {

    companion object {

        fun getShareIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context.applicationContext,
                0,
                SharingActivity.getIntent(context.applicationContext),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        fun getSettingsIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context.applicationContext,
                1,
                SettingsActivity.getIntent(context.applicationContext),
                PendingIntent.FLAG_UPDATE_CURRENT
            )


        fun blockCount(widgetOptions: Bundle?): Int {
            if (widgetOptions == null) return 0
            val maxWidth = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            Timber.d("np4d max width: $maxWidth")
            return maxWidth / 113
        }
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        job = Job()
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        job.cancel()
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (context == null || appWidgetIds == null || appWidgetManager == null) return

        updateWidget(context, appWidgetManager, null, *appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context == null || newOptions == null || appWidgetManager == null) return

        updateWidget(context, appWidgetManager, newOptions, appWidgetId)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        newOptions: Bundle? = null,
        vararg ids: Int
    ) = launch {
        if (ids.isEmpty()) return@launch

        val trackInfo = PreferenceManager.getDefaultSharedPreferences(context)
            .getCurrentTrackInfo()

        ids.forEach { id ->
            val widgetOptions = newOptions ?: appWidgetManager.getAppWidgetOptions(id)
            val widget =
                getShareWidgetViews(context, blockCount(widgetOptions), trackInfo)
            withContext(Dispatchers.Main) { appWidgetManager.updateAppWidget(id, widget) }
        }
    }
}