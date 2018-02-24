package com.geckour.nowplaying4gpm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geckour.nowplaying4gpm.service.NotifyMediaMetaDataService

class LaunchReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) context?.startService(NotifyMediaMetaDataService.getIntent(context))
    }
}