package com.geckour.nowplaying4gpm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geckour.nowplaying4gpm.App.Companion.launchMetaDataService

class LaunchReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> launchMetaDataService(context)
            Intent.ACTION_PACKAGE_ADDED -> launchMetaDataService(context)
        }
    }
}