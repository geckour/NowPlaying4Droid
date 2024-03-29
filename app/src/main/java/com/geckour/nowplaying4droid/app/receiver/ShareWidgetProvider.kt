package com.geckour.nowplaying4droid.app.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.util.PrefKey
import com.geckour.nowplaying4droid.app.util.foldBreaks
import com.geckour.nowplaying4droid.app.util.getBitmapFromUriString
import com.geckour.nowplaying4droid.app.util.getClearTrackDetailPendingIntent
import com.geckour.nowplaying4droid.app.util.getCurrentTrackDetail
import com.geckour.nowplaying4droid.app.util.getSettingsIntent
import com.geckour.nowplaying4droid.app.util.getShareIntent
import com.geckour.nowplaying4droid.app.util.getSharingText
import com.geckour.nowplaying4droid.app.util.getSwitchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class ShareWidgetProvider : AppWidgetProvider(), CoroutineScope {

    companion object {

        private const val ACTION_UPDATE_WIDGET = "action_update_widget"
        private const val KEY_TRACK_INFO = "key_track_info"

        fun getUpdateIntent(context: Context, trackDetail: TrackDetail?): Intent =
            Intent(context, ShareWidgetProvider::class.java)
                .setAction(ACTION_UPDATE_WIDGET)
                .putExtra(KEY_TRACK_INFO, trackDetail)

        fun blockCount(widgetOptions: Bundle?): Int {
            if (widgetOptions == null) return 0
            val maxWidth = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
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

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        context ?: return

        if (intent?.action == ACTION_UPDATE_WIDGET) {
            val manager = AppWidgetManager.getInstance(context)
            val trackDetail = intent.getSerializableExtra(KEY_TRACK_INFO) as TrackDetail?
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ShareWidgetProvider::class.java)
            )
            updateWidget(context, manager, null, *ids, trackDetail = trackDetail)
        }
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
        vararg ids: Int,
        trackDetail: TrackDetail? = PreferenceManager.getDefaultSharedPreferences(context)
            .getCurrentTrackDetail()
    ) = launch {
        if (ids.isEmpty()) return@launch

        ids.forEach { id ->
            val widgetOptions = newOptions ?: appWidgetManager.getAppWidgetOptions(id)
            val widget = getShareWidgetViews(context, blockCount(widgetOptions), trackDetail)
            withContext(Dispatchers.Main) { appWidgetManager.updateAppWidget(id, widget) }
        }
    }

    private suspend fun getShareWidgetViews(
        context: Context,
        blockCount: Int = 0,
        trackDetail: TrackDetail? = null
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_share).apply {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val detail = trackDetail ?: sharedPreferences.getCurrentTrackDetail()
        val summary = sharedPreferences.getSharingText(context, detail)?.foldBreaks()

        setTextViewText(
            R.id.widget_summary_share,
            summary ?: context.getString(R.string.dialog_message_alert_no_metadata)
        )

        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET)
            && blockCount > 1
        ) {
            val artwork = detail?.artworkUriString
                ?.let { context.getBitmapFromUriString(it, 500) }
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
            getShareIntent(context)
        )

        val packageName =
            if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_LAUNCH_PLAYER_WITH_WIDGET_ARTWORK))
                detail?.playerPackageName
            else null
        val launchIntent =
            packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
        setOnClickPendingIntent(
            R.id.artwork,
            if (launchIntent != null) {
                PendingIntent.getActivity(
                    context.applicationContext,
                    2,
                    launchIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                getSettingsIntent(context)
            }
        )

        setOnClickPendingIntent(
            R.id.widget_button_setting,
            getSettingsIntent(context)
        )

        setOnClickPendingIntent(
            R.id.widget_button_clear,
            getClearTrackDetailPendingIntent(context)
        )
    }
}