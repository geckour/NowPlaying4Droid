package com.geckour.nowplaying4gpm.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import com.android.vending.billing.IInAppBillingService
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.geckour.nowplaying4gpm.App
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.MastodonInstancesApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.api.model.PurchaseResult
import com.geckour.nowplaying4gpm.databinding.*
import com.geckour.nowplaying4gpm.domain.model.MastodonUserInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.adapter.ArtworkResolveMethodListAdapter
import com.geckour.nowplaying4gpm.util.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException
import com.sys1yagi.mastodon4j.api.method.Accounts
import com.sys1yagi.mastodon4j.api.method.Apps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber

@RuntimePermissions
class SettingsActivity : ScopedActivity() {

    enum class RequestCode {
        GRANT_NOTIFICATION_LISTENER,
        BILLING
    }

    companion object {
        const val STATE_KEY_MASTODON_REGISTRATION_INFO = "state_key_mastodon_registration_info"

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
    private lateinit var serviceConnection: ServiceConnection
    private var billingService: IInAppBillingService? = null

    private val twitterApiClient =
            TwitterApiClient(BuildConfig.TWITTER_CONSUMER_KEY, BuildConfig.TWITTER_CONSUMER_SECRET)


    private val mastodonScope = Scope(Scope.Name.ALL)
    private var mastodonRegistrationInfo: AppRegistration? = null

    private var showingNotificationServicePermissionDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        binding.toolbar.title =
                "${getString(R.string.activity_title_settings)} - ${getString(R.string.app_name)}"

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

        binding.itemSwitchUseApi.also { binding ->
            binding.maskInactiveDonate.visibility =
                    if (sharedPreferences.getDonateBillingState())
                        View.GONE
                    else View.VISIBLE

            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }

            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_USE_API) { state, summary ->
                    binding.summary = summary
                    requestUpdate()
                })
            }
        }

        binding.itemChangeArtworkResolveOrder.also { binding ->
            binding.maskInactiveDonate.visibility =
                    if (sharedPreferences.getDonateBillingState())
                        View.GONE
                    else View.VISIBLE

            binding.root.setOnClickListener { onClickChangeArtworkResolveOrder() }
        }

        binding.itemPatternFormat.root.setOnClickListener { onClickItemPatternFormat() }

        binding.itemChooseColor.root.setOnClickListener { onClickItemChooseColor() }

        binding.itemSwitchStrictMatchPattern.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE) { _, summary ->
                    binding.summary = summary

                    requestUpdate()
                })
            }
        }

        binding.itemSwitchReside.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_RESIDE) { state, summary ->
                    binding.summary = summary

                    if (state) requestUpdate()
                    else destroyNotification()
                })
            }
        }

        binding.itemSwitchShowArtworkInNotification.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION) { _, summary ->
                    binding.summary = summary

                    requestUpdate()
                })
            }
        }

        binding.itemSwitchBundleArtwork.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK) { _, summary ->
                    binding.summary = summary

                    requestUpdate()
                })
            }
        }

        binding.itemSwitchCopyIntoClipboard.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD) { _, summary ->
                    binding.summary = summary

                    requestUpdate()
                })
            }
        }

        binding.itemSwitchAutoPostMastodon.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON) { checkState, summary ->
                    binding.summary = summary
                    this@SettingsActivity.binding.itemAuthMastodon.maskInactive.visibility =
                            if (checkState) View.GONE else View.VISIBLE
                    this@SettingsActivity.binding.itemDelayMastodon.maskInactive.visibility =
                            if (checkState) View.GONE else View.VISIBLE
                    this@SettingsActivity.binding.itemVisibilityMastodon.maskInactive.visibility =
                            if (checkState) View.GONE else View.VISIBLE
                })
            }
        }

        binding.itemAuthMastodon.also { binding ->
            binding.maskInactive.visibility =
                    if (sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON))
                        View.GONE
                    else View.VISIBLE

            val userInfo = sharedPreferences.getMastodonUserInfo()
            if (userInfo != null) {
                binding.summary =
                        getString(R.string.pref_item_summary_auth_mastodon,
                                userInfo.userName, userInfo.instanceName)
            }
            binding.root.setOnClickListener { onClickAuthMastodon() }
        }

        binding.itemDelayMastodon.also { binding ->
            binding.maskInactive.visibility =
                    if (sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON))
                        View.GONE
                    else View.VISIBLE

            binding.summary = getString(R.string.pref_item_summary_delay_mastodon,
                    sharedPreferences.getDelayDurationPostMastodon())
            binding.root.setOnClickListener { onClickDelayMastodon() }
        }

        binding.itemVisibilityMastodon.also { binding ->
            binding.maskInactive.visibility =
                    if (sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON))
                        View.GONE
                    else View.VISIBLE

            binding.summary = getString(sharedPreferences.getVisibilityMastodon().getSummaryResId())
            binding.root.setOnClickListener { onClickVisibilityMastodon() }
        }

        binding.itemSwitchColorizeNotificationBg.also { binding ->
            if (Build.VERSION.SDK_INT < 26) binding.root.visibility = View.GONE
            else {
                binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
                binding.extra.apply {
                    visibility = View.VISIBLE
                    addView(getSwitch(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG) { _, summary ->
                        binding.summary = summary

                        requestUpdate()
                    })
                }
            }
        }

        binding.itemSwitchShowArtworkInWidget.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET) { _, summary ->
                    binding.summary = summary

                    launch(Dispatchers.IO) { updateWidget() }
                })
            }
        }

        binding.itemSwitchLaunchGpmOnClickWidgetArtwork.also { binding ->
            binding.root.setOnClickListener { onClickItemWithSwitch(binding.extra) }
            binding.extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK) { _, summary ->
                    binding.summary = summary

                    launch(Dispatchers.IO) { updateWidget() }
                })
            }
        }

        binding.itemAuthTwitter.also { binding ->
            val accessToken = sharedPreferences.getTwitterAccessToken()
            if (accessToken != null) binding.summary = accessToken.screenName
            binding.root.setOnClickListener { onClickAuthTwitter() }
        }

        binding.itemDonate.also { binding ->
            if (sharedPreferences.getDonateBillingState()) binding.root.visibility = View.GONE
            else binding.root.setOnClickListener {
                launch { startBillingTransaction(BuildConfig.SKU_KEY_DONATE) }
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putString(STATE_KEY_MASTODON_REGISTRATION_INFO,
                mastodonRegistrationInfo?.let { Gson().toJson(it) })
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState.containsKey(STATE_KEY_MASTODON_REGISTRATION_INFO)) {
            try {
                this.mastodonRegistrationInfo =
                        Gson().fromJson(savedInstanceState
                                .getString(STATE_KEY_MASTODON_REGISTRATION_INFO),
                                AppRegistration::class.java)
            } catch (t: Throwable) {
                Timber.e(t)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        reflectDonation()

        requestNotificationListenerPermission {
            requestUpdate()
        }

        if (sharedPreferences.getAlertTwitterAuthFlag()) {
            showErrorDialog(
                    R.string.dialog_title_alert_must_auth_twitter,
                    R.string.dialog_message_alert_must_auth_twitter) {
                sharedPreferences.setAlertTwitterAuthFlag(false)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
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
            onAuthTwitterCallback(intent)
        if (intent?.data?.toString()?.startsWith(App.MASTODON_CALLBACK) == true)
            onAuthMastodonCallback(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RequestCode.GRANT_NOTIFICATION_LISTENER.ordinal -> {
                requestNotificationListenerPermission {
                    requestUpdate()
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
            launch(Dispatchers.IO) {
                val accessToken = twitterApiClient.getAccessToken(verifier)

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
        showErrorDialog(R.string.dialog_title_alert_failure_auth_twitter,
                R.string.dialog_message_alert_failure_auth_twitter)
    }

    private fun onAuthMastodonCallback(intent: Intent) {
        mastodonRegistrationInfo?.apply {
            val token = intent.data?.let {
                val queryName = "code"

                if (it.queryParameterNames.contains(queryName))
                    it.getQueryParameter(queryName)
                else null
            }

            if (token == null) onAuthMastodonError()
            else {
                val mastodonApiClientBuilder = MastodonClient.Builder(this@apply.instanceName,
                        OkHttpClient.Builder().apply {
                            if (BuildConfig.DEBUG) {
                                addNetworkInterceptor(
                                        HttpLoggingInterceptor()
                                                .setLevel(HttpLoggingInterceptor.Level.BODY))
                                addNetworkInterceptor(StethoInterceptor())
                            }
                        }, Gson())
                launch {
                    val accessToken = try {
                        Apps(mastodonApiClientBuilder.build())
                                .getAccessToken(
                                        this@apply.clientId,
                                        this@apply.clientSecret,
                                        App.MASTODON_CALLBACK,
                                        token)
                                .toJob(this@SettingsActivity + Dispatchers.IO)
                                .await()
                    } catch (e: Mastodon4jRequestException) {
                        Timber.e(e)
                        null
                    }

                    if (accessToken == null) onAuthMastodonError()
                    else {
                        val userName = Accounts(mastodonApiClientBuilder
                                .accessToken(accessToken.accessToken)
                                .build())
                                .getVerifyCredentials()
                                .toJob(this@SettingsActivity + Dispatchers.IO)
                                .await()
                                ?.userName ?: run {
                            onAuthMastodonError()
                            return@launch
                        }
                        val userInfo =
                                MastodonUserInfo(accessToken, this@apply.instanceName, userName)
                        sharedPreferences.storeMastodonUserInfo(userInfo)

                        binding.itemAuthMastodon.summary =
                                getString(R.string.pref_item_summary_auth_mastodon,
                                        userInfo.userName, userInfo.instanceName)
                        Snackbar.make(binding.root,
                                R.string.snackbar_text_success_auth_mastodon,
                                Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun onAuthMastodonError() {
        showErrorDialog(R.string.dialog_title_alert_failure_auth_mastodon,
                R.string.dialog_message_alert_failure_auth_mastodon)
    }

    private fun requestUpdate() {
        if (showingNotificationServicePermissionDialog.not()) {
            invokeUpdateWithPermissionCheck()
        }
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun invokeUpdate() {
        binding.maskInactiveApp.visibility = View.GONE

        NotificationService.sendRequestInvokeUpdate(this,
                sharedPreferences.getCurrentTrackInfo())
    }

    @OnNeverAskAgain(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun onNeverAskPermissionAgain() {
        binding.maskInactiveApp.visibility = View.VISIBLE
    }

    private fun destroyNotification() =
            sendBroadcast(Intent().apply {
                action = NotificationService.ACTION_DESTROY_NOTIFICATION
            })

    private suspend fun updateWidget() {
        val trackInfo = sharedPreferences.getCurrentTrackInfo() ?: return

        AppWidgetManager.getInstance(this).apply {
            val ids = getAppWidgetIds(ComponentName(this@SettingsActivity,
                    ShareWidgetProvider::class.java))

            ids.forEach { id ->
                val widgetOptions = this.getAppWidgetOptions(id)
                updateAppWidget(
                        id,
                        getShareWidgetViews(this@SettingsActivity, this@SettingsActivity,
                                ShareWidgetProvider.isMin(widgetOptions), trackInfo)
                )
            }
        }
    }

    private fun requestNotificationListenerPermission(onGranted: () -> Unit = {}) {
        if (NotificationManagerCompat.getEnabledListenerPackages(this)
                        .contains(packageName).not()) {
            if (showingNotificationServicePermissionDialog.not()) {
                showingNotificationServicePermissionDialog = true
                AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_alert_grant_notification_listener)
                        .setMessage(R.string.dialog_message_alert_grant_notification_listener)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
                            startActivityForResult(
                                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
                                    RequestCode.GRANT_NOTIFICATION_LISTENER.ordinal)
                            dialog.dismiss()
                            showingNotificationServicePermissionDialog = false
                        }.show()
            }
        } else onGranted()
    }

    private fun onClickChangeArtworkResolveOrder() {
        val adapter = ArtworkResolveMethodListAdapter(sharedPreferences
                .getArtworkResolveOrder()
                .toMutableList())
        val artworkResolveMethodDialogBinding = DialogArtworkMethodBinding.inflate(
                LayoutInflater.from(this),
                null,
                false
        ).apply {
            recyclerView.adapter = adapter
            adapter.itemTouchHolder.attachToRecyclerView(recyclerView)
        }

        AlertDialog.Builder(this).generate(
                artworkResolveMethodDialogBinding.root,
                getString(R.string.dialog_title_artwork_resolve_order),
                getString(R.string.dialog_message_artwork_resolve_order)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val order = adapter.items
                    sharedPreferences.setArtworkResolveOrder(order)
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickAuthTwitter() {
        launch(Dispatchers.IO) {
            val uri = twitterApiClient.getRequestOAuthUri() ?: run {
                Snackbar.make(binding.root, R.string.snackbar_text_failure_auth_twitter, Snackbar.LENGTH_SHORT)
                return@launch
            }

            CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(getColor(R.color.colorPrimary))
                    .build()
                    .launchUrl(this@SettingsActivity, uri)
        }
    }

    private fun onClickAuthMastodon() {
        val instanceNameInputDialogBinding = DialogAutoCompleteEditTextBinding.inflate(
                LayoutInflater.from(this),
                null,
                false
        ).apply {
            hint = getString(R.string.dialog_hint_mastodon_instance)
            editText.setText(sharedPreferences.getMastodonUserInfo()?.instanceName)
            editText.setSelection(editText.text.length)

            launch {
                val instances = MastodonInstancesApiClient().getList()
                editText.setAdapter(ArrayAdapter<String>(this@SettingsActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        instances.map { it.name }))
            }
        }

        AlertDialog.Builder(this).generate(
                instanceNameInputDialogBinding.root,
                getString(R.string.dialog_title_mastodon_instance)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val instance = instanceNameInputDialogBinding.editText.text.toString()
                    if (instance.isNotBlank()) {
                        launch {
                            val mastodonApiClient =
                                    MastodonClient.Builder(instance, OkHttpClient.Builder().apply {
                                        if (BuildConfig.DEBUG) {
                                            addNetworkInterceptor(
                                                    HttpLoggingInterceptor()
                                                            .setLevel(HttpLoggingInterceptor.Level.BODY))
                                            addNetworkInterceptor(StethoInterceptor())
                                        }
                                    }, Gson())
                                            .build()
                            val registrationInfo = try {
                                Apps(mastodonApiClient).createApp(App.MASTODON_CLIENT_NAME,
                                        App.MASTODON_CALLBACK,
                                        mastodonScope,
                                        App.MASTODON_WEB_URL)
                                        .toJob(this@SettingsActivity + Dispatchers.IO)
                                        .await()
                            } catch (e: Mastodon4jRequestException) {
                                Timber.e(e)
                                null
                            } ?: run {
                                onAuthMastodonError()
                                return@launch
                            }
                            this@SettingsActivity.mastodonRegistrationInfo = registrationInfo

                            val authUrl = Apps(mastodonApiClient)
                                    .getOAuthUrl(registrationInfo.clientId,
                                            mastodonScope, App.MASTODON_CALLBACK)

                            CustomTabsIntent.Builder()
                                    .setShowTitle(true)
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

    private fun onClickDelayMastodon() {
        val delayTimeInputDialogBinding = DialogEditTextBinding.inflate(
                LayoutInflater.from(this),
                null,
                false
        ).apply {
            hint = getString(R.string.dialog_hint_mastodon_delay)
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            editText.setText(sharedPreferences.getDelayDurationPostMastodon().toString())
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(this).generate(
                delayTimeInputDialogBinding.root,
                getString(R.string.dialog_title_mastodon_delay)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val duration = try {
                        delayTimeInputDialogBinding.editText.text.toString().toLong()
                    } catch (t: Throwable) {
                        Timber.e(t)
                        null
                    }
                    if (duration != null && duration in (500..60000)) {
                        sharedPreferences.storeDelayDurationPostMastodon(duration)
                        binding.itemDelayMastodon.summary =
                                getString(R.string.pref_item_summary_delay_mastodon, duration)
                    } else {
                        showErrorDialog(R.string.dialog_title_alert_invalid_duration_value,
                                R.string.dialog_message_alert_invalid_duration_value)
                    }
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickVisibilityMastodon() {
        val chooseVisibilityBinding = DataBindingUtil.inflate<DialogSpinnerBinding>(
                LayoutInflater.from(this),
                R.layout.dialog_spinner,
                null,
                false
        ).apply {
            val arrayAdapter =
                    object : ArrayAdapter<String>(
                            this@SettingsActivity,
                            android.R.layout.simple_spinner_item,
                            Visibility.values().map { getString(it.getSummaryResId()) }) {
                        override fun getDropDownView(position: Int,
                                                     convertView: View?, parent: ViewGroup): View =
                                super.getDropDownView(position, convertView, parent).apply {
                                    if (position == spinner.selectedItemPosition) {
                                        (this as TextView).setTextColor(
                                                getColor(R.color.colorPrimaryDark))
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
                getString(R.string.dialog_message_mastodon_visibility)) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val visibilityIndex = chooseVisibilityBinding.spinner.selectedItemPosition
                    sharedPreferences.edit()
                            .putInt(PrefKey.PREF_KEY_CHOSEN_MASTODON_VISIBILITY.name, visibilityIndex)
                            .apply()
                    binding.itemVisibilityMastodon.summary =
                            getString(Visibility.getFromIndex(visibilityIndex).getSummaryResId())
                    requestUpdate()
                }
            }
            dialog.dismiss()
        }.show()
    }

    private suspend fun startBillingTransaction(skuName: String) {
        billingService?.let {
            BillingApiClient(this, it).apply {
                val sku =
                        getSkuDetails(this@SettingsActivity, skuName).firstOrNull()
                                ?: run {
                                    showErrorDialog(R.string.dialog_title_alert_failure_purchase,
                                            R.string.dialog_message_alert_on_start_purchase)
                                    return
                                }

                if (getPurchasedItems(this@SettingsActivity).contains(sku.productId)) {
                    showErrorDialog(R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_already_purchase)
                    reflectDonation(true)
                    return
                }
            }

            startIntentSenderForResult(
                    BillingApiClient(this, it)
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
        val patternFormatDialogBinding = DialogEditTextBinding.inflate(
                LayoutInflater.from(this),
                null,
                false
        ).apply {
            hint = getString(R.string.dialog_hint_pattern_format)
            editText.setText(sharedPreferences.getFormatPattern(this@SettingsActivity))
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(this).generate(
                patternFormatDialogBinding.root,
                getString(R.string.dialog_title_pattern_format),
                getString(R.string.dialog_message_pattern_format)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val pattern = patternFormatDialogBinding.editText.text.toString()
                    sharedPreferences.edit()
                            .putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, pattern)
                            .apply()
                    binding.itemPatternFormat.summary = pattern
                }
            }
            requestUpdate()
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
                        override fun getDropDownView(position: Int,
                                                     convertView: View?, parent: ViewGroup): View =
                                super.getDropDownView(position, convertView, parent).apply {
                                    if (position == spinner.selectedItemPosition) {
                                        (this as TextView).setTextColor(
                                                getColor(R.color.colorPrimaryDark))
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
                chooseColorBinding.root,
                getString(R.string.dialog_title_choose_color),
                getString(R.string.dialog_message_choose_color)) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val paletteIndex = chooseColorBinding.spinner.selectedItemPosition
                    sharedPreferences.edit()
                            .putInt(PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name, paletteIndex)
                            .apply()
                    binding.itemChooseColor.summary =
                            getString(PaletteColor.getFromIndex(paletteIndex).getSummaryResId())
                    requestUpdate()
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickItemWithSwitch(extra: FrameLayout?) = (extra?.getChildAt(0) as? Switch)?.performClick()

    private fun getSwitch(prefKey: PrefKey,
                          onCheckStateChanged: (checkState: Boolean, summary: String) -> Unit = { _, _ -> }): Switch {
        return Switch(this@SettingsActivity).apply {
            fun getSummary(): String = getString(
                    if (isChecked) R.string.pref_item_summary_switch_on
                    else R.string.pref_item_summary_switch_off)
            setOnClickListener {
                sharedPreferences.edit()
                        .putBoolean(prefKey.name, isChecked)
                        .apply()

                onCheckStateChanged(isChecked,
                        getSummary())
            }
            isChecked = sharedPreferences.getSwitchState(prefKey)
            onCheckStateChanged(isChecked, getSummary())
        }
    }

    private fun showErrorDialog(titleResId: Int, messageResId: Int, onDismiss: () -> Unit = {}) =
            AlertDialog.Builder(this)
                    .setTitle(titleResId)
                    .setMessage(messageResId)
                    .setPositiveButton(R.string.dialog_button_ok) { dialog, _ -> dialog.dismiss() }
                    .setOnDismissListener { onDismiss() }
                    .show()

    private fun reflectDonation(state: Boolean? = null) {
        val s = state ?: sharedPreferences.getDonateBillingState()

        sharedPreferences.edit().putBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, s).apply()
        binding.itemDonate.root.visibility = if (s) View.GONE else View.VISIBLE
        binding.itemSwitchUseApi.maskInactiveDonate.visibility =
                if (s) View.GONE else View.VISIBLE
        binding.itemChangeArtworkResolveOrder.maskInactiveDonate.visibility =
                if (s) View.GONE else View.VISIBLE
    }
}