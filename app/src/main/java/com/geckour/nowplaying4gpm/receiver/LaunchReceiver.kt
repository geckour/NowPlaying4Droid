package com.geckour.nowplaying4gpm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geckour.nowplaying4gpm.service.NotifyMediaMetaDataService.Companion.launchService

class LaunchReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> launchService(context)
            Intent.ACTION_PACKAGE_ADDED -> launchService(context)
        }
    }
}