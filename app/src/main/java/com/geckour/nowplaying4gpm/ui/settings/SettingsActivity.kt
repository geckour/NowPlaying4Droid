package com.geckour.nowplaying4gpm.ui.settings

import android.Manifest
import android.app.Activity
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.android.vending.billing.IInAppBillingService
import com.geckour.nowplaying4gpm.App
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.api.model.PurchaseResult
import com.geckour.nowplaying4gpm.databinding.ActivitySettingsBinding
import com.geckour.nowplaying4gpm.databinding.ItemPrefItemBinding
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.WithCrashlyticsActivity
import com.geckour.nowplaying4gpm.ui.license.LicensesActivity
import com.geckour.nowplaying4gpm.ui.observe
import com.geckour.nowplaying4gpm.util.*
import com.google.gson.Gson
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class SettingsActivity : WithCrashlyticsActivity() {

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

    private val viewModel: SettingsViewModel by lazy {
        ViewModelProviders.of(this)[SettingsViewModel::class.java]
    }
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var serviceConnection: ServiceConnection
    private var billingService: IInAppBillingService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        binding.toolbarTitle = "${getString(R.string.activity_title_settings)} - ${getString(R.string.app_name)}"
        setSupportActionBar(binding.toolbar)

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

        binding.itemPatternFormat.summary = sharedPreferences.getFormatPattern(this)
        binding.itemChooseColor.summary =
            getString(sharedPreferences.getChosePaletteColor().getSummaryResId())

        binding.fab.setOnClickListener { viewModel.onClickFab(this, sharedPreferences) }

        binding.scrollView.apply {
            setOnScrollChangeListener { _, _, y, _, oldY ->
                if (y > oldY
                    && getChildAt(0).measuredHeight <= measuredHeight + y
                )
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
                            && itemBinding.categoryId == binding.categoryOthers.root.id
                        ) {
                            visibleCount++
                        }
                    }
                }

                binding.categoryOthers.root.visibility =
                    if (visibleCount == 0) View.GONE
                    else View.VISIBLE
            }

        binding.itemSwitchUseApi.also { b ->
            b.maskInactiveDonate.visibility =
                if (sharedPreferences.getDonateBillingState())
                    View.GONE
                else View.VISIBLE

            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }

            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_USE_API) { _, summary ->
                    b.summary = summary
                    onRequestUpdate()
                })
            }
        }

        binding.itemChangeArtworkResolveOrder.also { b ->
            b.maskInactiveDonate.visibility =
                if (sharedPreferences.getDonateBillingState())
                    View.GONE
                else View.VISIBLE

            b.root.setOnClickListener {
                viewModel.onClickChangeArtworkResolveOrder(this, sharedPreferences)
            }
        }

        binding.itemPatternFormat.root.setOnClickListener {
            viewModel.onClickItemPatternFormat(this, sharedPreferences, binding.itemPatternFormat)
        }

        binding.itemFormatPatternModifiers.root.setOnClickListener {
            viewModel.onClickFormatPatternModifiers(this, sharedPreferences)
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
                    this@SettingsActivity.binding.itemAuthMastodon.maskInactive.visibility =
                        if (checkState) View.GONE else View.VISIBLE
                    this@SettingsActivity.binding.itemDelayMastodon.maskInactive.visibility =
                        if (checkState) View.GONE else View.VISIBLE
                    this@SettingsActivity.binding.itemVisibilityMastodon.maskInactive.visibility =
                        if (checkState) View.GONE else View.VISIBLE
                    this@SettingsActivity.binding.itemSwitchSuccessNotificationMastodon.maskInactive.visibility =
                        if (checkState) View.GONE else View.VISIBLE
                })
            }
        }

        binding.itemAuthMastodon.also { b ->
            b.maskInactive.visibility =
                if (sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                    )
                )
                    View.GONE
                else View.VISIBLE

            val userInfo = sharedPreferences.getMastodonUserInfo()
            if (userInfo != null) {
                b.summary =
                    getString(
                        R.string.pref_item_summary_auth_mastodon,
                        userInfo.userName, userInfo.instanceName
                    )
            }
            b.root.setOnClickListener { viewModel.onClickAuthMastodon(this, sharedPreferences) }
        }

        binding.itemDelayMastodon.also { b ->
            b.maskInactive.visibility =
                if (sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                    )
                )
                    View.GONE
                else View.VISIBLE

            b.summary = getString(
                R.string.pref_item_summary_delay_mastodon,
                sharedPreferences.getDelayDurationPostMastodon()
            )
            b.root.setOnClickListener {
                viewModel.onClickDelayMastodon(this, sharedPreferences, binding.itemDelayMastodon)
            }
        }

        binding.itemVisibilityMastodon.also { b ->
            b.maskInactive.visibility =
                if (sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                    )
                )
                    View.GONE
                else View.VISIBLE

            b.summary = getString(sharedPreferences.getVisibilityMastodon().getSummaryResId())
            b.root.setOnClickListener {
                viewModel.onClickVisibilityMastodon(this, sharedPreferences, binding.itemVisibilityMastodon)
            }
        }

        binding.itemSwitchSuccessNotificationMastodon.also { b ->
            b.maskInactive.visibility =
                if (sharedPreferences.getSwitchState(
                        PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                    )
                )
                    View.GONE
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
                    else viewModel.destroyNotification(this@SettingsActivity)
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
            viewModel.onClickItemChooseColor(this, sharedPreferences, binding.itemChooseColor)
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

                    viewModel.updateWidget(this@SettingsActivity, sharedPreferences)
                })
            }
        }

        binding.itemSwitchLaunchGpmOnClickWidgetArtwork.also { b ->
            b.root.setOnClickListener { onClickItemWithSwitch(b.extra) }
            b.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK) { _, summary ->
                    b.summary = summary

                    viewModel.updateWidget(this@SettingsActivity, sharedPreferences)
                })
            }
        }

        binding.itemAuthTwitter.also { b ->
            val accessToken = sharedPreferences.getTwitterAccessToken()
            if (accessToken != null) b.summary = accessToken.screenName
            b.root.setOnClickListener { viewModel.onClickAuthTwitter(this, b.root) }
        }

        binding.itemDonate.also { b ->
            if (sharedPreferences.getDonateBillingState()) b.root.visibility = View.GONE
            else b.root.setOnClickListener {
                viewModel.startBillingTransaction(this, billingService, BuildConfig.SKU_KEY_DONATE)
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
            Intent("com.android.vending.billing.InAppBillingService.BIND").apply {
                `package` = "com.android.vending"
            },
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()

        onReflectDonation()

        viewModel.requestNotificationListenerPermission(this) {
            onRequestUpdate()
        }

        if (sharedPreferences.getAlertTwitterAuthFlag()) {
            viewModel.showErrorDialog(
                this,
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

        if (intent?.data?.toString()?.startsWith(TwitterApiClient.TWITTER_CALLBACK) == true)
            viewModel.onAuthTwitterCallback(
                intent, this, sharedPreferences,
                binding.root, binding.itemAuthTwitter
            )
        if (intent?.data?.toString()?.startsWith(App.MASTODON_CALLBACK) == true)
            viewModel.onAuthMastodonCallback(
                intent, this, sharedPreferences,
                binding.root, binding.itemAuthMastodon
            )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCode.GRANT_NOTIFICATION_LISTENER.ordinal -> {
                viewModel.requestNotificationListenerPermission(this) {
                    onRequestUpdate()
                }
            }

            RequestCode.BILLING.ordinal -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        data?.getStringExtra(BillingApiClient.BUNDLE_KEY_PURCHASE_DATA)?.apply {
                            val purchaseResult: PurchaseResult? =
                                Gson().fromJsonOrNull(this, PurchaseResult::class.java)

                            if (purchaseResult?.purchaseState == 0) {
                                onReflectDonation(true)
                            } else {
                                viewModel.showErrorDialog(
                                    this@SettingsActivity,
                                    R.string.dialog_title_alert_failure_purchase,
                                    R.string.dialog_message_alert_failure_purchase
                                )
                            }
                        }
                    }

                    Activity.RESULT_CANCELED -> {
                        viewModel.showErrorDialog(
                            this,
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

    private fun onClickItemWithSwitch(extra: FrameLayout?) = (extra?.getChildAt(0) as? Switch)?.performClick()

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
                sharedPreferences.edit()
                    .putBoolean(prefKey.name, isChecked)
                    .apply()

                onCheckStateChanged(
                    isChecked,
                    getSummary()
                )
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
}