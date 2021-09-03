package com.geckour.nowplaying4gpm.receiver

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
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.settings.SettingsActivity
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.foldBreak
import com.geckour.nowplaying4gpm.util.getBitmapFromUriString
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.getSharingText
import com.geckour.nowplaying4gpm.util.getSwitchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class ShareWidgetProvider : AppWidgetProvider(), CoroutineScope {

    companion object {

        private const val ACTION_UPDATE_WIDGET = "action_update_widget"
        private const val KEY_TRACK_INFO = "key_track_info"

        fun getUpdateIntent(context: Context, trackInfo: TrackInfo?): Intent =
            Intent(context, ShareWidgetProvider::class.java)
                .setAction(ACTION_UPDATE_WIDGET)
                .putExtra(KEY_TRACK_INFO, trackInfo)

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
            val trackInfo = intent.getSerializableExtra(KEY_TRACK_INFO) as TrackInfo?
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ShareWidgetProvider::class.java)
            )
            updateWidget(context, manager, null, *ids, trackInfo = trackInfo)
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
        trackInfo: TrackInfo? = PreferenceManager.getDefaultSharedPreferences(context)
            .getCurrentTrackInfo()
    ) = launch {
        if (ids.isEmpty()) return@launch

        ids.forEach { id ->
            val widgetOptions = newOptions ?: appWidgetManager.getAppWidgetOptions(id)
            val widget = getShareWidgetViews(context, blockCount(widgetOptions), trackInfo)
            withContext(Dispatchers.Main) { appWidgetManager.updateAppWidget(id, widget) }
        }
    }

    private suspend fun getShareWidgetViews(
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
            val artwork = info?.artworkUriString
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
            if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK))
                info?.playerPackageName
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
            NotificationService.getClearTrackInfoPendingIntent(context)
        )
    }

    private fun getShareIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context.applicationContext,
            Random(System.currentTimeMillis()).nextInt(1, Int.MAX_VALUE),
            SharingActivity.getIntent(context),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun getSettingsIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context.applicationContext,
            0,
            SettingsActivity.getIntent(context),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}