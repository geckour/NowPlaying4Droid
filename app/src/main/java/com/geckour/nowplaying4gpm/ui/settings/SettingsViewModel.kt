package com.geckour.nowplaying4gpm.ui.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.ui.SingleLiveEvent

class SettingsViewModel : ViewModel() {

    private var showingNotificationServicePermissionDialog = false

    internal val requestUpdate = SingleLiveEvent<Unit>()
    internal val reflectDonation = SingleLiveEvent<Boolean>()

    internal fun requestNotificationListenerPermission(
        activity: Activity, onGranted: () -> Unit = {}
    ) {
        if (NotificationManagerCompat.getEnabledListenerPackages(activity).contains(activity.packageName).not()) {
            if (showingNotificationServicePermissionDialog.not()) {
                showingNotificationServicePermissionDialog = true
                AlertDialog.Builder(activity)
                    .setTitle(R.string.dialog_title_alert_grant_notification_listener)
                    .setMessage(R.string.dialog_message_alert_grant_notification_listener)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
                        activity.startActivityForResult(
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
                            SettingsActivity.RequestCode.GRANT_NOTIFICATION_LISTENER.ordinal
                        )
                        dialog.dismiss()
                        showingNotificationServicePermissionDialog = false
                    }.show()
            }
        } else onGranted()
    }

    internal fun onRequestUpdate() {
        if (showingNotificationServicePermissionDialog.not()) requestUpdate.call()
    }
}