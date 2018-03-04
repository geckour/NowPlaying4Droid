package com.geckour.nowplaying4gpm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.geckour.nowplaying4gpm.service.NotifyMediaMetaDataService

class LaunchReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> launchService(context)
            Intent.ACTION_PACKAGE_ADDED -> launchService(context)
        }
    }

    private fun launchService(context: Context?) {
        if (Build.VERSION.SDK_INT >= 26) context?.startForegroundService(NotifyMediaMetaDataService.getIntent(context))
        else context?.startService(NotifyMediaMetaDataService.getIntent(context))
    }
}