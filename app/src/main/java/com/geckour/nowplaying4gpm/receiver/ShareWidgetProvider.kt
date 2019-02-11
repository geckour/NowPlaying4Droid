package com.geckour.nowplaying4gpm.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import com.geckour.nowplaying4gpm.ui.settings.SettingsActivity
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getShareWidgetViews
import kotlinx.coroutines.runBlocking

class ShareWidgetProvider : AppWidgetProvider() {

    enum class Action {
        SHARE,
        OPEN_SETTING
    }

    companion object {
        fun getPendingIntent(context: Context, action: Action): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ShareWidgetProvider::class.java).apply { setAction(action.name) },
                PendingIntent.FLAG_CANCEL_CURRENT
            )

        fun isMin(widgetOptions: Bundle?): Boolean {
            if (widgetOptions == null) return false
            val maxWidth = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            return maxWidth < 232
        }
    }

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (context == null || appWidgetIds == null) return

        updateWidget(context, *appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context == null || newOptions == null) return

        updateWidget(context, appWidgetId)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context == null || intent == null) return

        when (intent.action) {
            Action.SHARE.name -> context.startActivity(SharingActivity.getIntent(context))

            Action.OPEN_SETTING.name -> context.startActivity(SettingsActivity.getIntent(context))
        }
    }

    private fun updateWidget(context: Context, vararg ids: Int) =
        runBlocking {
            if (ids.isNotEmpty()) {
                val trackInfo = PreferenceManager.getDefaultSharedPreferences(context)
                    .getCurrentTrackInfo()

                AppWidgetManager.getInstance(context).apply {
                    ids.forEach { id ->
                        val widgetOptions = this.getAppWidgetOptions(id)
                        updateAppWidget(
                            id,
                            getShareWidgetViews(context, isMin(widgetOptions), trackInfo)
                        )
                    }
                }
            }
        }
}