package com.geckour.nowplaying4gpm.ui.settings

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.android.vending.billing.IInAppBillingService
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.geckour.nowplaying4gpm.App
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.MastodonInstancesApiClient
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.api.model.PurchaseResult
import com.geckour.nowplaying4gpm.databinding.ActivitySettingsBinding
import com.geckour.nowplaying4gpm.databinding.DialogAutoCompleteEditTextBinding
import com.geckour.nowplaying4gpm.databinding.DialogEditTextBinding
import com.geckour.nowplaying4gpm.databinding.DialogFormatPatternModifiersBinding
import com.geckour.nowplaying4gpm.databinding.DialogRecyclerViewBinding
import com.geckour.nowplaying4gpm.databinding.DialogSpinnerBinding
import com.geckour.nowplaying4gpm.databinding.ItemPrefItemBinding
import com.geckour.nowplaying4gpm.domain.model.MastodonUserInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.WithCrashlyticsActivity
import com.geckour.nowplaying4gpm.ui.license.LicensesActivity
import com.geckour.nowplaying4gpm.ui.observe
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.ui.widget.adapter.ArtworkResolveMethodListAdapter
import com.geckour.nowplaying4gpm.ui.widget.adapter.FormatPatternModifierListAdapter
import com.geckour.nowplaying4gpm.ui.widget.adapter.PlayerPackageListAdapter
import com.geckour.nowplaying4gpm.util.PaletteColor
import com.geckour.nowplaying4gpm.util.PlayerPackageState
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.Visibility
import com.geckour.nowplaying4gpm.util.cleaerSpotifyUserInfoImmediately
import com.geckour.nowplaying4gpm.util.executeCatching
import com.geckour.nowplaying4gpm.util.generate
import com.geckour.nowplaying4gpm.util.getAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.getArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.getChosePaletteColor
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getDebugSpotifySearchFlag
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getDonateBillingState
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.getMastodonUserInfo
import com.geckour.nowplaying4gpm.util.getPackageStateListPostMastodon
import com.geckour.nowplaying4gpm.util.getShareWidgetViews
import com.geckour.nowplaying4gpm.util.getSpotifyUserInfo
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getTwitterAccessToken
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import com.geckour.nowplaying4gpm.util.json
import com.geckour.nowplaying4gpm.util.parseOrNull
import com.geckour.nowplaying4gpm.util.readyForShare
import com.geckour.nowplaying4gpm.util.setAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.setArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.setFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.storeDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.storeMastodonUserInfo
import com.geckour.nowplaying4gpm.util.storePackageStatePostMastodon
import com.geckour.nowplaying4gpm.util.storeTwitterAccessToken
import com.geckour.nowplaying4gpm.util.withCatching
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration
import com.sys1yagi.mastodon4j.api.method.Accounts
import com.sys1yagi.mastodon4j.api.method.Apps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber

@RuntimePermissions
class SettingsActivity : WithCrashlyticsActivity() {

    enum class RequestCode {
        GRANT_NOTIFICATION_LISTENER,
        BILLING
    }

    companion object {
        fun getIntent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java).apply {
                flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
            }
    }

    private data class EasterEggTag(
        val count: Int,
        val time: Long
    )

    private val viewModel: SettingsViewModel by viewModels()
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var serviceConnection: ServiceConnection
    private var billingService: IInAppBillingService? = null

    private lateinit var spotifyApiClient: SpotifyApiClient

    private val twitterApiClient =
        TwitterApiClient(BuildConfig.TWITTER_CONSUMER_KEY, BuildConfig.TWITTER_CONSUMER_SECRET)

    private val mastodonScope = Scope(Scope.Name.ALL)
    private var mastodonRegistrationInfo: AppRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyApiClient = SpotifyApiClient(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        binding.toolbarTitle =
            "${getString(R.string.activity_title_settings)} - ${getString(R.string.app_name)}"
        setSupportActionBar(binding.toolbar)

        binding.toolbar.apply {
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

        setupItems()

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
            Intent("com.android.vending.billing.InAppBillingService.BIND").apply {
                `package` = "com.android.vending"
            },
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        observeEvents()

        showIgnoreBatteryOptimizationDialog()
    }

    override fun onResume() {
        super.onResume()

        onReflectDonation()

        viewModel.requestNotificationListenerPermission(this) {
            onRequestUpdate()
        }

        if (sharedPreferences.getAlertTwitterAuthFlag()) {
            showErrorDialog(
                R.string.dialog_title_alert_must_auth_twitter,
                R.string.dialog_message_alert_must_auth_twitter
            ) {
                sharedPreferences.setAlertTwitterAuthFlag(false)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()

        billingService?.apply { unbindService(serviceConnection) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uriString = intent?.data?.toString()
        Timber.d("intent data: $uriString")
        when {
            uriString?.startsWith(SpotifyApiClient.SPOTIFY_CALLBACK) == true -> {
                onAuthSpotifyCallback(
                    intent,
                    binding.root,
                    binding.itemAuthSpotify
                )
            }
            uriString?.startsWith(TwitterApiClient.TWITTER_CALLBACK) == true -> {
                onAuthTwitterCallback(
                    intent,
                    sharedPreferences,
                    binding.root,
                    binding.itemAuthTwitter
                )
            }
            uriString?.startsWith(App.MASTODON_CALLBACK) == true -> {
                onAuthMastodonCallback(
                    intent,
                    sharedPreferences,
                    binding.root,
                    binding.itemAuthMastodon
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCode.GRANT_NOTIFICATION_LISTENER.ordinal -> {
                viewModel.requestNotificationListenerPermission(this) {
                    getSystemService(MediaSessionManager::class.java)?.addOnActiveSessionsChangedListener(
                        { controllers ->
                            controllers?.lastOrNull { it != null } ?: return@addOnActiveSessionsChangedListener

                            NotificationListenerService.requestRebind(
                                NotificationService.getComponentName(this)
                            )
                        },
                        NotificationService.getComponentName(this)
                    )
                    onRequestUpdate()
                }
            }

            RequestCode.BILLING.ordinal -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val purchaseResult: PurchaseResult? =
                            json.parseOrNull(
                                data?.getStringExtra(BillingApiClient.BUNDLE_KEY_PURCHASE_DATA)
                            )

                        if (purchaseResult?.purchaseState == 0) {
                            onReflectDonation(true)
                        } else {
                            showErrorDialog(
                                R.string.dialog_title_alert_failure_purchase,
                                R.string.dialog_message_alert_failure_purchase
                            )
                        }
                    }

                    Activity.RESULT_CANCELED -> {
                        showErrorDialog(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_on_cancel_purchase
                        )
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewModel.requestUpdate.observe(this) {
            onRequestUpdate()
        }
        viewModel.reflectDonation.observe(this) {
            it ?: return@observe
            onReflectDonation(it)
        }

        spotifyApiClient.refreshedUserInfo.observe(this) {
            if (sharedPreferences.getDebugSpotifySearchFlag()) {
                AlertDialog.Builder(this).setMessage("$it").show()
            }
        }
    }

    private fun setupItems() {
        binding.itemPatternFormat.summary = sharedPreferences.getFormatPattern(this)
        binding.itemChooseColor.summary =
            getString(sharedPreferences.getChosePaletteColor().getSummaryResId())

        binding.fab.setOnClickListener { onClickFab(sharedPreferences) }

        binding.scrollView.apply {
            setOnScrollChangeListener { _, _, y, _, oldY ->
                if (y > oldY && getChildAt(0).measuredHeight <= measuredHeight + y) binding.fab.hide()
                if (y < oldY && binding.fab.isShown.not()) binding.fab.show()
            }
        }

        binding.scrollView.getChildAt(0).addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val visible =
                (binding.scrollView.getChildAt(0) as? LinearLayout)?.let { itemsContainer ->
                    (0 until itemsContainer.childCount)
                        .mapNotNull {
                            DataBindingUtil.findBinding<ViewDataBinding>(
                                itemsContainer.getChildAt(it)
                            )
                        }
                        .filterIsInstance<ItemPrefItemBinding>()
                        .any {
                            it.root.visibility == View.VISIBLE &&
                                    it.categoryId == binding.categoryOthers.root.id
                        }
                } == true

            binding.categoryOthers.root.visibility = if (visible) View.VISIBLE else View.GONE
        }

        binding.itemChangeArtworkResolveOrder.also { b ->
            b.maskInactiveDonate.visibility =
                if (sharedPreferences.getDonateBillingState()) View.GONE
                else View.VISIBLE

            b.root.setOnClickListener {
                onClickChangeArtworkResolveOrder(sharedPreferences)
            }
        }

        binding.itemPatternFormat.root.setOnClickListener {
            onClickItemPatternFormat(sharedPreferences, binding.itemPatternFormat)
        }

        binding.itemFormatPatternModifiers.root.setOnClickListener {
            onClickFormatPatternModifiers(sharedPreferences)
        }

        binding.itemSwitchSimplifyShare.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_USE_SIMPLE_SHARE) { _, summary ->
                    b.summary = summary
                })
            }
        }

        binding.itemAuthSpotify.also { b ->
            val userInfo = sharedPreferences.getSpotifyUserInfo()
            if (userInfo != null) b.summary =
                getString(R.string.pref_item_summary_auth_spotify, userInfo.userName)
            b.root.setOnClickListener { onClickAuthSpotify() }
        }

        binding.itemSwitchStrictMatchPattern.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE) { _, summary ->
                    b.summary = summary

                    onRequestUpdate()
                })
            }
        }

        binding.itemSwitchBundleArtwork.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK) { _, summary ->
                    b.summary = summary

                    onRequestUpdate()
                })
            }
        }

        binding.itemSwitchCopyIntoClipboard.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD) { _, summary ->
                    b.summary = summary

                    onRequestUpdate()
                })
            }
        }

        binding.itemSwitchAutoPostMastodon.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON) { checkState, summary ->
                    b.summary = summary
                    binding.mastodonEnabled = checkState
                })
            }
        }

        binding.itemAuthMastodon.also { b ->
            val userInfo = sharedPreferences.getMastodonUserInfo()
            if (userInfo != null) {
                b.summary = getString(
                    R.string.pref_item_summary_auth_mastodon,
                    userInfo.userName,
                    userInfo.instanceName
                )
            }
            b.root.setOnClickListener { onClickAuthMastodon(sharedPreferences) }
        }

        binding.itemDelayMastodon.also { b ->
            b.summary = getString(
                R.string.pref_item_summary_delay_mastodon,
                sharedPreferences.getDelayDurationPostMastodon()
            )
            b.root.setOnClickListener {
                onClickDelayMastodon(sharedPreferences, b)
            }
        }

        binding.itemVisibilityMastodon.also { b ->
            b.summary = getString(sharedPreferences.getVisibilityMastodon().getSummaryResId())
            b.root.setOnClickListener {
                onClickVisibilityMastodon(sharedPreferences, b)
            }
        }

        binding.itemPlayerPackageMastodon.also { b ->
            b.root.setOnClickListener {
                onClickPlayerPackageMastodon(sharedPreferences)
            }
        }

        binding.itemSwitchSuccessNotificationMastodon.also { b ->
            b.maskInactive.visibility =
                if (sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                    )
                ) View.GONE
                else View.VISIBLE

            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON) { _, summary ->
                    b.summary = summary
                })
            }
        }

        binding.itemSwitchReside.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_RESIDE) { state, summary ->
                    b.summary = summary

                    if (state) onRequestUpdate()
                    else destroyNotification()
                })
            }
        }

        binding.itemSwitchShowArtworkInNotification.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION) { _, summary ->
                    b.summary = summary

                    onRequestUpdate()
                })
            }
        }

        binding.itemChooseColor.root.setOnClickListener {
            onClickItemChooseColor(sharedPreferences, binding.itemChooseColor)
        }

        binding.itemSwitchColorizeNotificationBg.also { b ->
            if (Build.VERSION.SDK_INT < 26) b.root.visibility = View.GONE
            else {
                b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
                b.extra.apply {
                    visibility = View.VISIBLE
                    addView(getSwitch(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG) { _, summary ->
                        b.summary = summary

                        onRequestUpdate()
                    })
                }
            }
        }

        binding.itemSwitchShowArtworkInWidget.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET) { _, summary ->
                    b.summary = summary

                    updateWidget(sharedPreferences)
                })
            }
        }

        binding.itemSwitchLaunchGpmOnClickWidgetArtwork.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK) { _, summary ->
                    b.summary = summary

                    updateWidget(sharedPreferences)
                })
            }
        }

        binding.itemSwitchShowClearButtonInWidget.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_SHOW_CLEAR_BUTTON_IN_WIDGET) { _, summary ->
                    b.summary = summary

                    updateWidget(sharedPreferences)
                })
            }
        }

        binding.itemAuthTwitter.also { b ->
            val accessToken = sharedPreferences.getTwitterAccessToken()
            if (accessToken != null) b.summary = accessToken.screenName
            b.root.setOnClickListener { onClickAuthTwitter(b.root) }
        }

        binding.itemDonate.also { b ->
            if (sharedPreferences.getDonateBillingState()) b.root.visibility = View.GONE
            else b.root.setOnClickListener {
                startBillingTransaction(this, billingService)
            }
        }
    }

    private fun onRequestUpdate() {
        invokeUpdateWithPermissionCheck()
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    internal fun invokeUpdate() {
        binding.maskInactiveApp.visibility = View.GONE

        NotificationService.sendRequestInvokeUpdate(this)
    }

    @OnNeverAskAgain(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    internal fun onNeverAskPermissionAgain() {
        binding.maskInactiveApp.visibility = View.VISIBLE
    }

    private fun showIgnoreBatteryOptimizationDialog() {
        if (sharedPreferences.getSwitchState(PrefKey.PREF_KEY_DENIED_IGNORE_BATTERY_OPTIMIZATION)
                .not() &&
            getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(packageName) == false
        ) {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_ignore_battery_optimization)
                .setMessage(R.string.dialog_message_ignore_battery_optimization)
                .setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    withCatching { startActivity(intent) }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_ng) { dialog, _ ->
                    sharedPreferences.edit()
                        .putBoolean(PrefKey.PREF_KEY_DENIED_IGNORE_BATTERY_OPTIMIZATION.name, true)
                        .apply()
                    dialog.dismiss()
                }
                .setOnCancelListener {
                    sharedPreferences.edit()
                        .putBoolean(PrefKey.PREF_KEY_DENIED_IGNORE_BATTERY_OPTIMIZATION.name, true)
                        .apply()
                }
                .show()
        }
    }

    private fun onClickItemWithSwitch(extra: FrameLayout?) =
        (extra?.getChildAt(0) as? Switch)?.performClick()

    private fun getSwitch(
        prefKey: PrefKey,
        onCheckStateChanged: (checkState: Boolean, summary: String) -> Unit = { _, _ -> }
    ): Switch {
        return Switch(this@SettingsActivity).apply {
            fun getSummary(): String = getString(
                if (isChecked) R.string.pref_item_summary_switch_on
                else R.string.pref_item_summary_switch_off
            )
            setOnClickListener {
                sharedPreferences.edit().putBoolean(prefKey.name, isChecked).apply()

                onCheckStateChanged(isChecked, getSummary())
            }
            isChecked = sharedPreferences.getSwitchState(prefKey)
            onCheckStateChanged(isChecked, getSummary())
        }
    }

    private fun onReflectDonation(state: Boolean? = null) {
        val s = state ?: sharedPreferences.getDonateBillingState()

        sharedPreferences.edit().putBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, s).apply()
        binding.donated = s
    }

    private fun onClickItemPatternFormat(
        sharedPreferences: SharedPreferences,
        patternFormatBinding: ItemPrefItemBinding
    ) {
        val dialogBinding = DialogEditTextBinding.inflate(
            LayoutInflater.from(this), null, false
        ).apply {
            hint = getString(R.string.dialog_hint_pattern_format)
            editText.setText(sharedPreferences.getFormatPattern(this@SettingsActivity))
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(this).generate(
            dialogBinding.root,
            getString(R.string.dialog_title_pattern_format),
            getString(R.string.dialog_message_pattern_format)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val pattern = dialogBinding.editText.text.toString()
                    sharedPreferences.edit()
                        .putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, pattern).apply()
                    patternFormatBinding.summary = pattern
                }
            }
            onRequestUpdate()
            dialog.dismiss()
        }.show()
    }

    private fun showErrorDialog(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        onDismiss: () -> Unit = {}
    ) = runOnUiThread {
        AlertDialog.Builder(this).setTitle(titleResId).setMessage(messageResId)
            .setPositiveButton(R.string.dialog_button_ok) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { onDismiss() }.show()
    }

    private fun onAuthSpotifyError() {
        showErrorDialog(
            R.string.dialog_title_alert_failure_auth_spotify,
            R.string.dialog_message_alert_failure_auth_spotify
        )
    }

    private fun onAuthMastodonError() {
        showErrorDialog(
            R.string.dialog_title_alert_failure_auth_mastodon,
            R.string.dialog_message_alert_failure_auth_mastodon
        )
    }

    private fun onAuthTwitterError() {
        showErrorDialog(
            R.string.dialog_title_alert_failure_auth_twitter,
            R.string.dialog_message_alert_failure_auth_twitter
        )
    }

    private fun destroyNotification() =
        sendBroadcast(Intent().apply { action = NotificationService.ACTION_DESTROY_NOTIFICATION })

    private fun updateWidget(sharedPreferences: SharedPreferences) {
        val trackInfo = sharedPreferences.getCurrentTrackInfo() ?: return

        AppWidgetManager.getInstance(this).apply {
            val ids = getAppWidgetIds(
                ComponentName(
                    this@SettingsActivity, ShareWidgetProvider::class.java
                )
            )

            ids.forEach { id ->
                val widgetOptions = this@apply.getAppWidgetOptions(id)
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    updateAppWidget(
                        id,
                        getShareWidgetViews(
                            this@SettingsActivity,
                            ShareWidgetProvider.blockCount(widgetOptions),
                            trackInfo
                        )
                    )
                }
            }
        }
    }

    private fun onClickChangeArtworkResolveOrder(sharedPreferences: SharedPreferences) {
        val adapter = ArtworkResolveMethodListAdapter(
            sharedPreferences.getArtworkResolveOrder().toMutableList()
        )
        val dialogRecyclerViewBinding = DialogRecyclerViewBinding.inflate(
            LayoutInflater.from(this), null, false
        ).apply {
            recyclerView.adapter = adapter
            adapter.itemTouchHolder.attachToRecyclerView(recyclerView)
        }

        AlertDialog.Builder(this).generate(
            dialogRecyclerViewBinding.root,
            getString(R.string.dialog_title_artwork_resolve_order),
            getString(R.string.dialog_message_artwork_resolve_order)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val order = adapter.items
                    sharedPreferences.setArtworkResolveOrder(order)
                }
            }
            onRequestUpdate()
            dialog.dismiss()
        }.show()
    }

    private fun onClickFormatPatternModifiers(sharedPreferences: SharedPreferences) {
        val adapter = FormatPatternModifierListAdapter(
            sharedPreferences.getFormatPatternModifiers().toMutableList()
        )
        val formatPatternModifiersDialogBinding = DialogFormatPatternModifiersBinding.inflate(
            LayoutInflater.from(this), null, false
        ).apply { recyclerView.adapter = adapter }

        val dialog = AlertDialog.Builder(this).generate(
            formatPatternModifiersDialogBinding.root,
            getString(R.string.dialog_title_format_pattern_modifier),
            getString(R.string.dialog_message_format_pattern_modifier)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val modifiers = adapter.items
                    sharedPreferences.setFormatPatternModifiers(modifiers)
                }
            }
            onRequestUpdate()
            dialog.dismiss()
        }
        dialog.show()
        dialog.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    private fun onClickAuthSpotify() {
        sharedPreferences.cleaerSpotifyUserInfoImmediately()
        CustomTabsIntent.Builder().setShowTitle(true)
            .setToolbarColor(getColor(R.color.colorPrimary))
            .build()
            .launchUrl(this, Uri.parse(SpotifyApiClient.OAUTH_URL))
    }

    private fun onClickAuthTwitter(rootView: View) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val uri = twitterApiClient.getRequestOAuthUri() ?: run {
                Snackbar.make(
                    rootView, R.string.snackbar_text_failure_auth_twitter, Snackbar.LENGTH_SHORT
                )
                return@launch
            }

            CustomTabsIntent.Builder().setShowTitle(true)
                .setToolbarColor(getColor(R.color.colorPrimary))
                .build()
                .launchUrl(this@SettingsActivity, uri)
        }
    }

    private fun onClickAuthMastodon(sharedPreferences: SharedPreferences) {
        val instanceNameInputDialogBinding = DialogAutoCompleteEditTextBinding.inflate(
            LayoutInflater.from(this), null, false
        ).apply {
            hint = getString(R.string.dialog_hint_mastodon_instance)
            editText.setText(sharedPreferences.getMastodonUserInfo()?.instanceName)
            editText.setSelection(editText.text.length)

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val instances = MastodonInstancesApiClient().getList()
                withContext(Dispatchers.Main) {
                    editText.setAdapter(
                        ArrayAdapter(this@SettingsActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            instances.mapNotNull { it.name })
                    )
                }
            }
        }

        AlertDialog.Builder(this).generate(
            instanceNameInputDialogBinding.root, getString(R.string.dialog_title_mastodon_instance)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val instance = instanceNameInputDialogBinding.editText.text.toString()
                    if (instance.isNotBlank()) {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            val mastodonApiClient =
                                MastodonClient.Builder(instance, OkHttpClient.Builder().apply {
                                    if (BuildConfig.DEBUG) {
                                        addNetworkInterceptor(
                                            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                                        )
                                        addNetworkInterceptor(StethoInterceptor())
                                    }
                                }, Gson()).build()
                            val registrationInfo = Apps(mastodonApiClient).createApp(
                                App.MASTODON_CLIENT_NAME,
                                App.MASTODON_CALLBACK,
                                mastodonScope,
                                App.MASTODON_WEB_URL
                            ).executeCatching {
                                Timber.e(it)
                                Crashlytics.logException(it)
                            } ?: run {
                                onAuthMastodonError()
                                return@launch
                            }
                            mastodonRegistrationInfo = registrationInfo

                            val authUrl = Apps(mastodonApiClient).getOAuthUrl(
                                registrationInfo.clientId, mastodonScope, App.MASTODON_CALLBACK
                            )

                            CustomTabsIntent.Builder().setShowTitle(true)
                                .setToolbarColor(getColor(R.color.colorPrimary))
                                .build()
                                .launchUrl(this@SettingsActivity, Uri.parse(authUrl))
                        }
                    }
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickDelayMastodon(
        sharedPreferences: SharedPreferences, itemDelayMastodonBinding: ItemPrefItemBinding
    ) {
        val delayTimeInputDialogBinding = DialogEditTextBinding.inflate(
            LayoutInflater.from(this), null, false
        ).apply {
            hint = getString(R.string.dialog_hint_mastodon_delay)
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            editText.setText(sharedPreferences.getDelayDurationPostMastodon().toString())
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(this).generate(
            delayTimeInputDialogBinding.root, getString(R.string.dialog_title_mastodon_delay)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val duration = withCatching {
                        delayTimeInputDialogBinding.editText.text.toString().toLong()
                    }
                    if (duration != null && duration in (500..60000)) {
                        sharedPreferences.storeDelayDurationPostMastodon(duration)
                        itemDelayMastodonBinding.summary =
                            getString(R.string.pref_item_summary_delay_mastodon, duration)
                    } else {
                        showErrorDialog(
                            R.string.dialog_title_alert_invalid_duration_value,
                            R.string.dialog_message_alert_invalid_duration_value
                        )
                    }
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickVisibilityMastodon(
        sharedPreferences: SharedPreferences, visibilityMastodonBinding: ItemPrefItemBinding
    ) {
        val chooseVisibilityBinding = DataBindingUtil.inflate<DialogSpinnerBinding>(
            LayoutInflater.from(this), R.layout.dialog_spinner, null, false
        ).apply {
            val arrayAdapter = object : ArrayAdapter<String>(
                this@SettingsActivity,
                android.R.layout.simple_spinner_item,
                Visibility.values().map {
                    getString(it.getSummaryResId())
                }) {
                override fun getDropDownView(
                    position: Int, convertView: View?, parent: ViewGroup
                ): View = super.getDropDownView(position, convertView, parent).apply {
                    if (position == spinner.selectedItemPosition) {
                        (this as TextView).setTextColor(
                            context.getColor(R.color.colorPrimaryVariant)
                        )
                    }
                }
            }.apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinner.apply {
                adapter = arrayAdapter
                setSelection(sharedPreferences.getVisibilityMastodon().ordinal)
            }
        }

        AlertDialog.Builder(this).generate(
            chooseVisibilityBinding.root,
            getString(R.string.dialog_title_mastodon_visibility),
            getString(R.string.dialog_message_mastodon_visibility)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val visibilityIndex = chooseVisibilityBinding.spinner.selectedItemPosition
                    sharedPreferences.edit()
                        .putInt(PrefKey.PREF_KEY_CHOSEN_MASTODON_VISIBILITY.name, visibilityIndex)
                        .apply()
                    visibilityMastodonBinding.summary =
                        getString(Visibility.getFromIndex(visibilityIndex).getSummaryResId())
                    onRequestUpdate()
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickPlayerPackageMastodon(sharedPreferences: SharedPreferences) {
        val adapter =
            PlayerPackageListAdapter(
                sharedPreferences.getPackageStateListPostMastodon().mapNotNull { packageState ->
                    val appName = packageManager?.let {
                        withCatching {
                            it.getApplicationLabel(
                                it.getApplicationInfo(
                                    packageState.packageName,
                                    PackageManager.GET_META_DATA
                                )
                            )
                        }
                    }?.toString()
                    if (appName == null) {
                        sharedPreferences.storePackageStatePostMastodon(
                            packageState.packageName, false
                        )
                        null
                    } else PlayerPackageState(packageState.packageName, appName, packageState.state)
                })
        val dialogRecyclerViewBinding = DialogRecyclerViewBinding.inflate(
            LayoutInflater.from(this), null, false
        ).apply {
            recyclerView.adapter = adapter
        }

        AlertDialog.Builder(this).generate(
            dialogRecyclerViewBinding.root,
            getString(R.string.dialog_title_player_package_mastodon),
            getString(R.string.dialog_message_player_package_mastodon)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    adapter.itemsDiff.forEach {
                        sharedPreferences.storePackageStatePostMastodon(it.packageName, it.state)
                    }
                }
            }
            onRequestUpdate()
            dialog.dismiss()
        }.show()
    }

    private fun onClickFab(sharedPreferences: SharedPreferences) {
        if (sharedPreferences.readyForShare(this).not()) {
            showErrorDialog(
                R.string.dialog_title_alert_no_metadata,
                R.string.dialog_message_alert_no_metadata
            )
            return
        }

        startActivity(SharingActivity.getIntent(this))
    }

    private fun startBillingTransaction(
        activity: Activity, billingService: IInAppBillingService?
    ) = viewModel.viewModelScope.launch(Dispatchers.IO) {
        billingService?.let {
            val skuName = BuildConfig.SKU_KEY_DONATE
            BillingApiClient(it).apply {
                val sku = getSkuDetails(activity, skuName).firstOrNull() ?: run {
                    showErrorDialog(
                        R.string.dialog_title_alert_failure_purchase,
                        R.string.dialog_message_alert_on_start_purchase
                    )
                    return@launch
                }

                if (getPurchasedItems(activity).contains(sku.productId)) {
                    showErrorDialog(
                        R.string.dialog_title_alert_failure_purchase,
                        R.string.dialog_message_alert_already_purchase
                    )
                    viewModel.reflectDonation.postValue(true)
                    return@launch
                }
            }

            activity.startIntentSenderForResult(
                BillingApiClient(it).getBuyIntent(activity, skuName)?.intentSender,
                RequestCode.BILLING.ordinal,
                Intent(),
                0,
                0,
                0
            )
        }
    }

    private fun onClickItemChooseColor(
        sharedPreferences: SharedPreferences, chooseColorBinding: ItemPrefItemBinding
    ) {
        val dialogBinding = DataBindingUtil.inflate<DialogSpinnerBinding>(
            LayoutInflater.from(this), R.layout.dialog_spinner, null, false
        ).apply {
            val arrayAdapter = object : ArrayAdapter<String>(
                this@SettingsActivity,
                android.R.layout.simple_spinner_item,
                PaletteColor.values().map {
                    getString(it.getSummaryResId())
                }
            ) {
                override fun getDropDownView(
                    position: Int, convertView: View?, parent: ViewGroup
                ): View = super.getDropDownView(position, convertView, parent).apply {
                    if (position == spinner.selectedItemPosition) {
                        (this as TextView).setTextColor(
                            context.getColor(R.color.colorPrimaryVariant)
                        )
                    }
                }
            }.apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinner.apply {
                adapter = arrayAdapter
                setSelection(sharedPreferences.getChosePaletteColor().ordinal)
            }
        }

        AlertDialog.Builder(this).generate(
            dialogBinding.root,
            getString(R.string.dialog_title_choose_color),
            getString(R.string.dialog_message_choose_color)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val paletteIndex = dialogBinding.spinner.selectedItemPosition
                    sharedPreferences.edit()
                        .putInt(PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name, paletteIndex).apply()
                    chooseColorBinding.summary = getString(
                        PaletteColor.getFromIndex(paletteIndex).getSummaryResId()
                    )
                    onRequestUpdate()
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onAuthSpotifyCallback(
        intent: Intent, rootView: View, authSpotifyBinding: ItemPrefItemBinding
    ) {
        val verifier = intent.data?.getQueryParameter("code")
        if (verifier == null) {
            onAuthSpotifyError()
            return
        }

        viewModel.viewModelScope.launch {
            spotifyApiClient.storeSpotifyUserInfo(verifier)

            val userInfo = sharedPreferences.getSpotifyUserInfo()
            if (userInfo == null) {
                onAuthSpotifyError()
                return@launch
            }

            authSpotifyBinding.summary = getString(
                R.string.pref_item_summary_auth_spotify,
                userInfo.userName
            )
            Snackbar.make(
                rootView,
                R.string.snackbar_text_success_auth_spotify,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun onAuthTwitterCallback(
        intent: Intent,
        sharedPreferences: SharedPreferences,
        rootView: View,
        authTwitterBinding: ItemPrefItemBinding
    ) {
        sharedPreferences.setAlertTwitterAuthFlag(false)

        val verifier = intent.data?.getQueryParameter("oauth_verifier")
        if (verifier == null) {
            onAuthTwitterError()
            return
        }

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val accessToken = twitterApiClient.getAccessToken(verifier)

            if (accessToken == null) {
                onAuthTwitterError()
                return@launch
            }

            sharedPreferences.storeTwitterAccessToken(accessToken)

            authTwitterBinding.summary = accessToken.screenName
            Snackbar.make(
                rootView,
                R.string.snackbar_text_success_auth_twitter,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun onAuthMastodonCallback(
        intent: Intent,
        sharedPreferences: SharedPreferences,
        rootView: View,
        authMastodonBinding: ItemPrefItemBinding
    ) {
        mastodonRegistrationInfo?.apply {
            val token = intent.data?.getQueryParameter("code")
            if (token == null) {
                onAuthMastodonError()
                return
            }

            val mastodonApiClientBuilder = MastodonClient.Builder(
                this@apply.instanceName, OkHttpClient.Builder().apply {
                    if (BuildConfig.DEBUG) {
                        addNetworkInterceptor(
                            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                        )
                        addNetworkInterceptor(StethoInterceptor())
                    }
                }, Gson()
            )
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val accessToken = Apps(mastodonApiClientBuilder.build()).getAccessToken(
                    this@apply.clientId,
                    this@apply.clientSecret,
                    App.MASTODON_CALLBACK,
                    token
                ).executeCatching()

                if (accessToken == null) {
                    onAuthMastodonError()
                    return@launch
                }

                val userName = Accounts(
                    mastodonApiClientBuilder.accessToken(accessToken.accessToken).build()
                ).getVerifyCredentials()
                    .executeCatching()
                    ?.userName
                    ?: run {
                        onAuthMastodonError()
                        return@launch
                    }
                val userInfo = MastodonUserInfo(accessToken, this@apply.instanceName, userName)
                sharedPreferences.storeMastodonUserInfo(userInfo)

                authMastodonBinding.summary = getString(
                    R.string.pref_item_summary_auth_mastodon,
                    userInfo.userName,
                    userInfo.instanceName
                )
                Snackbar.make(
                    rootView, R.string.snackbar_text_success_auth_mastodon, Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}