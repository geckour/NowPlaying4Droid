package com.geckour.nowplaying4gpm.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.*
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationManagerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.vending.billing.IInAppBillingService
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.api.model.PurchaseResult
import com.geckour.nowplaying4gpm.databinding.ActivitySettingsBinding
import com.geckour.nowplaying4gpm.databinding.DialogEditTextBinding
import com.geckour.nowplaying4gpm.databinding.DialogSpinnerBinding
import com.geckour.nowplaying4gpm.databinding.ItemPrefItemBinding
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.util.*
import com.google.gson.Gson
import kotlinx.coroutines.experimental.Job

class SettingsActivity : Activity() {

    enum class PermissionRequestCode {
        EXTERNAL_STORAGE
    }

    enum class RequestCode {
        GRANT_NOTIFICATION_LISTENER,
        BILLING
    }

    companion object {
        fun getIntent(context: Context): Intent =
                Intent(context, SettingsActivity::class.java)
    }

    private data class EasterEggTag(
            val count: Int,
            val time: Long
    )

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private lateinit var binding: ActivitySettingsBinding
    private val jobs: ArrayList<Job> = ArrayList()
    private lateinit var serviceConnection: ServiceConnection
    private var billingService: IInAppBillingService? = null

    private val twitterApiClient =
            TwitterApiClient(BuildConfig.TWITTER_CONSUMER_KEY, BuildConfig.TWITTER_CONSUMER_SECRET)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCrashlytics()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        binding.toolbar.title = "${getString(R.string.activity_title_settings)} - ${getString(R.string.app_name)}"

        binding.toolbarCover.apply {
            tag = EasterEggTag(0, -1L)

            setOnClickListener {
                val countLimit = 7
                val timeLimit = 300L
                val count = (tag as? EasterEggTag)?.count?.inc() ?: 1
                val time = (tag as? EasterEggTag)?.time ?: -1L
                val now = System.currentTimeMillis()
                tag = if (count < countLimit) {
                    if (time < 0 || now - time < timeLimit) EasterEggTag(count, now)
                    else EasterEggTag(0, -1L)
                } else {
                    startActivity(LicensesActivity.getIntent(this@SettingsActivity))
                    EasterEggTag(0, -1L)
                }
            }
        }

        binding.summaryPattern = sharedPreferences.getFormatPattern(this)
        binding.summaryChooseColor = getString(sharedPreferences.getChosePaletteColor().getSummaryResId())
        binding.summarySwitchReside = getString(sharedPreferences.getSwitchSummaryResId(PrefKey.PREF_KEY_WHETHER_RESIDE))
        binding.summarySwitchUseApi = getString(sharedPreferences.getSwitchSummaryResId(PrefKey.PREF_KEY_WHETHER_USE_API))
        binding.summarySwitchBundleArtwork = getString(sharedPreferences.getSwitchSummaryResId(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK))
        binding.summarySwitchColorizeNotificationBg = getString(sharedPreferences.getSwitchSummaryResId(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG))
        binding.summarySwitchShowArtworkInWidget = getString(sharedPreferences.getSwitchSummaryResId(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET))
        binding.summarySwitchLaunchGpmOnClickWidgetArtwork = getString(sharedPreferences.getSwitchSummaryResId(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK))

        binding.fab.setOnClickListener { onClickFab() }

        binding.scrollView.apply {
            setOnScrollChangeListener { _, _, y, _, oldY ->
                if (y > oldY
                        && getChildAt(0).measuredHeight <= measuredHeight + y)
                    binding.fab.hide()
                if (y < oldY && binding.fab.isShown.not())
                    binding.fab.show()
            }
        }

        binding.scrollView
                .getChildAt(0)
                .addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    var visibleCount = 0
                    (binding.scrollView.getChildAt(0) as? LinearLayout)?.apply {
                        (0 until this.childCount).forEach {
                            val itemBinding: ItemPrefItemBinding? = try {
                                DataBindingUtil.findBinding(this.getChildAt(it))
                            } catch (e: ClassCastException) {
                                return@forEach
                            }

                            if (itemBinding?.root?.visibility == View.VISIBLE
                                    && itemBinding.categoryId == binding.categoryOthers.root.id) {
                                visibleCount++
                            }
                        }
                    }

                    binding.categoryOthers.root.visibility =
                            if (visibleCount == 0) View.GONE
                            else View.VISIBLE
                }

        binding.itemSwitchUseApi.apply {
            maskInactive.visibility =
                    if (sharedPreferences.getDonateBillingState())
                        View.GONE
                    else View.VISIBLE

            root.setOnClickListener { onClickItemWithSwitch(extra) }

            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_USE_API) { _, summary ->
                    binding.summarySwitchUseApi = summary
                    updateNotification()
                })
            }
        }

        binding.itemPatternFormat.root.setOnClickListener { onClickItemPatternFormat() }

        binding.itemChooseColor.root.setOnClickListener { onClickItemChooseColor() }

        binding.itemSwitchReside.apply {
            root.setOnClickListener { onClickItemWithSwitch(extra) }
            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_RESIDE) { state, summary ->
                    binding.summarySwitchReside = summary

                    sharedPreferences.edit().putBoolean(PrefKey.PREF_KEY_WHETHER_RESIDE.name, state).apply()
                    if (state) updateNotification()
                    else destroyNotification()
                })
            }
        }

        binding.itemSwitchBundleArtwork.apply {
            root.setOnClickListener { onClickItemWithSwitch(extra) }
            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK) { _, summary ->
                    binding.summarySwitchBundleArtwork = summary

                    updateNotification()
                })
            }
        }

        binding.itemSwitchColorizeNotificationBg.apply {
            if (Build.VERSION.SDK_INT < 26) root.visibility = View.GONE
            else {
                root.setOnClickListener { onClickItemWithSwitch(extra) }
                extra.apply {
                    visibility = View.VISIBLE
                    addView(getSwitch(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG) { _, summary ->
                        binding.summarySwitchColorizeNotificationBg = summary

                        updateNotification()
                    })
                }
            }
        }

        binding.itemSwitchShowArtworkInWidget.apply {
            root.setOnClickListener { onClickItemWithSwitch(extra) }
            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET) { _, summary ->
                    binding.summarySwitchShowArtworkInWidget = summary

                    async { updateWidget() }
                })
            }
        }

        binding.itemSwitchLaunchGpmOnClickWidgetArtwork.apply {
            root.setOnClickListener { onClickItemWithSwitch(extra) }
            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK) { _, summary ->
                    binding.summarySwitchLaunchGpmOnClickWidgetArtwork = summary

                    async { updateWidget() }
                })
            }
        }

        binding.itemAuthTwitter.apply {
            val accessToken = sharedPreferences.getTwitterAccessToken()
            if (accessToken != null) summary = accessToken.screenName
            root.setOnClickListener { onClickAuthTwitter() }
        }

        binding.itemDonate.apply {
            if (sharedPreferences.getDonateBillingState()) root.visibility = View.GONE
            else root.setOnClickListener {
                ui(jobs) { startBillingTransaction(BuildConfig.SKU_KEY_DONATE) }
            }
        }

        serviceConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                billingService = null
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                IInAppBillingService.Stub.asInterface(service).apply {
                    billingService = IInAppBillingService.Stub.asInterface(service)
                }
            }
        }
        bindService(
                Intent("com.android.vending.billing.InAppBillingService.BIND").apply { `package` = "com.android.vending" },
                serviceConnection,
                Context.BIND_AUTO_CREATE
        )

        requestNotificationListenerPermission {
            updateNotification()
        }
    }

    override fun onResume() {
        super.onResume()

        reflectDonation()

        if (sharedPreferences.getAlertTwitterAuthFlag()) {
            showErrorDialog(
                    R.string.dialog_title_alert_must_auth_twitter,
                    R.string.dialog_message_alert_must_auth_twitter) {
                sharedPreferences.setAlertTwitterAuthFlag(false)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionRequestCode.EXTERNAL_STORAGE.ordinal -> {
                updateNotification()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        billingService?.apply { unbindService(serviceConnection) }
        jobs.cancelAll()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.data?.toString()?.startsWith(TwitterApiClient.TWITTER_CALLBACK) == true)
            onAuthTwitterCallback(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCode.GRANT_NOTIFICATION_LISTENER.ordinal -> {
                requestNotificationListenerPermission {
                    updateNotification()
                }
            }

            RequestCode.BILLING.ordinal -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        data?.getStringExtra(BillingApiClient.BUNDLE_KEY_PURCHASE_DATA)?.apply {
                            val purchaseResult: PurchaseResult? =
                                    Gson().fromJsonOrNull(this, PurchaseResult::class.java)

                            if (purchaseResult?.purchaseState == 0) {
                                reflectDonation(true)
                            } else {
                                showErrorDialog(
                                        R.string.dialog_title_alert_failure_purchase,
                                        R.string.dialog_message_alert_failure_purchase)
                            }
                        }
                    }

                    Activity.RESULT_CANCELED -> {
                        showErrorDialog(
                                R.string.dialog_title_alert_failure_purchase,
                                R.string.dialog_message_alert_on_cancel_purchase)
                    }
                }
            }
        }
    }

    private fun onAuthTwitterCallback(intent: Intent) {
        sharedPreferences.setAlertTwitterAuthFlag(false)

        val verifier = intent.data?.let {
            val queryName = "oauth_verifier"

            if (it.queryParameterNames.contains(queryName))
                it.getQueryParameter(queryName)
            else null
        }

        if (verifier == null) onAuthTwitterError()
        else {
            async {
                val accessToken = twitterApiClient.getAccessToken(verifier).await()

                if (accessToken == null) onAuthTwitterError()
                else {
                    sharedPreferences.storeTwitterAccessToken(accessToken)

                    binding.itemAuthTwitter.summary = accessToken.screenName
                    Snackbar.make(binding.root,
                            R.string.snackbar_text_success_auth_twitter,
                            Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun onAuthTwitterError() {
        showErrorDialog(
                R.string.dialog_title_alert_failure_auth_twitter,
                R.string.dialog_message_alert_failure_auth_twitter)
    }

    private fun updateNotification() =
            requestStoragePermission {
                NotificationService.sendRequestShowNotification(this, sharedPreferences.getCurrentTrackInfo())
            }

    private fun destroyNotification() =
            sendBroadcast(Intent().apply { action = NotificationService.ACTION_DESTROY_NOTIFICATION })

    private suspend fun updateWidget() {
        val trackInfo = sharedPreferences.getCurrentTrackInfo() ?: return

        AppWidgetManager.getInstance(this).apply {
            val ids = getAppWidgetIds(ComponentName(this@SettingsActivity, ShareWidgetProvider::class.java))

            ids.forEach { id ->
                val widgetOptions = this.getAppWidgetOptions(id)
                updateAppWidget(
                        id,
                        getShareWidgetViews(this@SettingsActivity,
                                ShareWidgetProvider.isMin(widgetOptions), trackInfo)
                )
            }
        }
    }

    private fun requestNotificationListenerPermission(onGranted: () -> Unit = {}) {
        if (NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName).not()) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_alert_grant_notification_listener)
                    .setMessage(R.string.dialog_message_alert_grant_notification_listener)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
                        startActivityForResult(
                                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
                                RequestCode.GRANT_NOTIFICATION_LISTENER.ordinal)
                        dialog.dismiss()
                    }.show()
        } else onGranted()
    }

    private fun requestStoragePermission(onGranted: () -> Unit = {}) {
        checkStoragePermission({
            requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PermissionRequestCode.EXTERNAL_STORAGE.ordinal)
        }) { onGranted() }
    }

    private fun onClickAuthTwitter() {
        async {
            val uri = twitterApiClient.getRequestOAuthUri().await() ?: return@async

            CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(getColor(R.color.colorPrimary))
                    .build()
                    .launchUrl(this@SettingsActivity, uri)
        }
    }

    private suspend fun startBillingTransaction(skuName: String) {
        billingService?.let {
            BillingApiClient(it).apply {
                val sku =
                        getSkuDetails(this@SettingsActivity, skuName).firstOrNull()
                                ?: run {
                                    showErrorDialog(R.string.dialog_title_alert_failure_purchase, R.string.dialog_message_alert_on_start_purchase)
                                    return
                                }

                if (getPurchasedItems(this@SettingsActivity).contains(sku.productId)) {
                    showErrorDialog(R.string.dialog_title_alert_failure_purchase, R.string.dialog_message_alert_already_purchase)
                    reflectDonation(true)
                    return
                }
            }

            startIntentSenderForResult(
                    BillingApiClient(it)
                            .getBuyIntent(this@SettingsActivity, skuName)
                            ?.intentSender,
                    RequestCode.BILLING.ordinal,
                    Intent(), 0, 0, 0
            )
        }
    }

    private fun onClickFab() {
        val text = sharedPreferences.getSharingText(this)

        if (text == null) {
            showErrorDialog(
                    R.string.dialog_title_alert_no_metadata,
                    R.string.dialog_message_alert_no_metadata)
            return
        }

        startActivity(SharingActivity.getIntent(this@SettingsActivity))
    }

    private fun onClickItemPatternFormat() {
        val patternFormatDialogBinding = DataBindingUtil.inflate<DialogEditTextBinding>(
                LayoutInflater.from(this),
                R.layout.dialog_edit_text,
                null,
                false
        ).apply {
            hint = getString(R.string.dialog_hint_pattern_format)
            editText.setText(sharedPreferences.getFormatPattern(this@SettingsActivity))
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(this).generate(
                getString(R.string.dialog_title_pattern_format),
                getString(R.string.dialog_message_pattern_format),
                patternFormatDialogBinding.root
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val pattern = patternFormatDialogBinding.editText.text.toString()
                    sharedPreferences.edit()
                            .putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, pattern)
                            .apply()
                    binding.summaryPattern = pattern
                }
            }
            updateNotification()
            dialog.dismiss()
        }.show()
    }

    private fun onClickItemChooseColor() {
        val chooseColorBinding = DataBindingUtil.inflate<DialogSpinnerBinding>(
                LayoutInflater.from(this),
                R.layout.dialog_spinner,
                null,
                false
        ).apply {
            val arrayAdapter =
                    object : ArrayAdapter<String>(
                            this@SettingsActivity,
                            android.R.layout.simple_spinner_item,
                            PaletteColor.values().map { getString(it.getSummaryResId()) }) {
                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
                            return super.getDropDownView(position, convertView, parent).apply {
                                if (position == spinner.selectedItemPosition) (this as TextView).setTextColor(getColor(R.color.colorPrimaryDark))
                            }
                        }
                    }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinner.apply {
                adapter = arrayAdapter
                setSelection(sharedPreferences.getChosePaletteColor().ordinal)
            }
        }

        AlertDialog.Builder(this).generate(
                getString(R.string.dialog_title_choose_color),
                getString(R.string.dialog_message_choose_color),
                chooseColorBinding.root) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val paletteIndex = chooseColorBinding.spinner.selectedItemPosition
                    sharedPreferences.edit()
                            .putInt(PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name, paletteIndex)
                            .apply()
                    binding.summaryChooseColor = getString(PaletteColor.getFromIndex(paletteIndex).getSummaryResId())
                    updateNotification()
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickItemWithSwitch(extra: FrameLayout?) = (extra?.getChildAt(0) as? Switch)?.performClick()

    private fun getSwitch(prefKey: PrefKey, onCheckStateChanged: (checkState: Boolean, summary: String) -> Unit = { _, _ -> }): Switch =
            Switch(this@SettingsActivity).apply {
                setOnClickListener {
                    sharedPreferences.edit()
                            .putBoolean(prefKey.name, isChecked)
                            .apply()

                    onCheckStateChanged(isChecked, getString(if (isChecked) R.string.pref_item_summary_switch_on else R.string.pref_item_summary_switch_off))
                }
                isChecked = sharedPreferences.getBoolean(prefKey.name, true)
            }

    private fun showErrorDialog(titleResId: Int, messageResId: Int, onDismiss: () -> Unit = {}) =
            AlertDialog.Builder(this)
                    .setTitle(titleResId)
                    .setMessage(messageResId)
                    .setPositiveButton(R.string.dialog_button_ok) { dialog, _ -> dialog.dismiss() }
                    .setOnDismissListener { onDismiss() }
                    .show()

    private fun reflectDonation(state: Boolean? = null) {
        state?.apply {
            sharedPreferences.edit().putBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, this).apply()
            binding.itemDonate.root.visibility = if (state) View.GONE else View.VISIBLE
            binding.itemSwitchUseApi.maskInactive.visibility = if (state) View.GONE else View.VISIBLE
        } ?: run {
            val s = sharedPreferences.getDonateBillingState()
            binding.itemDonate.root.visibility = if (s) View.GONE else View.VISIBLE
            binding.itemSwitchUseApi.maskInactive.visibility = if (s) View.GONE else View.VISIBLE
        }
    }
}