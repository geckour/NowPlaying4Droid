package com.geckour.nowplaying4gpm.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.util.getCurrentAlbum
import com.geckour.nowplaying4gpm.util.getCurrentArtist
import com.geckour.nowplaying4gpm.util.getCurrentTitle
import com.geckour.nowplaying4gpm.util.getSharingText

class ShareWidgetProvider: AppWidgetProvider() {

    enum class Action {
        SHARE,
        OPEN_SETTING
    }

    companion object {
        fun getPendingIntent(context: Context, action: Action): PendingIntent =
                PendingIntent.getBroadcast(context, 0, Intent(context, ShareWidgetProvider::class.java).apply { setAction(action.name) }, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context == null || intent == null) return

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val summary = sharedPreferences.getSharingText(context)
        when (intent.action) {
            Action.SHARE.name -> {
                summary?.let {
                    val sharingIntent =
                            SharingActivity.getIntent(context,
                                    sharedPreferences.getCurrentTitle(),
                                    sharedPreferences.getCurrentArtist(),
                                    sharedPreferences.getCurrentAlbum())
                    context.startActivity(sharingIntent)
                }
            }

            Action.OPEN_SETTING.name -> context.startActivity(SettingsActivity.getIntent(context))
        }
    }
}