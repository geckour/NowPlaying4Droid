package com.geckour.nowplaying4gpm.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.util.*
import kotlinx.coroutines.experimental.Job

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
                        PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val jobs: ArrayList<Job> = ArrayList()

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (context == null) return

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val summary = sharedPreferences.getSharingText(context)
        val artworkUri = sharedPreferences.getTempArtworkUri(context)

        updateWidget(context, summary, artworkUri)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context == null || intent == null) return

        when (intent.action) {
            Action.SHARE.name -> {
                ui(jobs) {
                    context.startActivity(SharingActivity.getIntent(context))
                }
            }

            Action.OPEN_SETTING.name -> context.startActivity(SettingsActivity.getIntent(context))
        }
    }

    private fun updateWidget(context: Context, summary: String?, artworkUri: Uri?) =
            async {
                AppWidgetManager.getInstance(context).apply {
                    val ids =
                            getAppWidgetIds(ComponentName(context, ShareWidgetProvider::class.java))
                                    .firstOrNull() ?: return@apply

                    updateAppWidget(
                            ids,
                            getShareWidgetViews(context, summary, artworkUri)
                    )
                }
            }
}