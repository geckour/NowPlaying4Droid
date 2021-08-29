package com.geckour.nowplaying4gpm.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.geckour.nowplaying4gpm.App
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.MastodonInstancesApiClient
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
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
import com.geckour.nowplaying4gpm.ui.compose.CleanBlue
import com.geckour.nowplaying4gpm.ui.compose.DarkRed
import com.geckour.nowplaying4gpm.ui.compose.DeepBlue
import com.geckour.nowplaying4gpm.ui.compose.DeepRed
import com.geckour.nowplaying4gpm.ui.compose.InkBlackWeak
import com.geckour.nowplaying4gpm.ui.compose.LightRed
import com.geckour.nowplaying4gpm.ui.compose.SmokeWhite
import com.geckour.nowplaying4gpm.ui.compose.SettingsTheme
import com.geckour.nowplaying4gpm.ui.compose.SmokeBlack
import com.geckour.nowplaying4gpm.ui.license.LicensesActivity
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.ui.widget.adapter.ArtworkResolveMethodListAdapter
import com.geckour.nowplaying4gpm.ui.widget.adapter.FormatPatternModifierListAdapter
import com.geckour.nowplaying4gpm.ui.widget.adapter.PlayerPackageListAdapter
import com.geckour.nowplaying4gpm.util.PaletteColor
import com.geckour.nowplaying4gpm.util.PlayerPackageState
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.Visibility
import com.geckour.nowplaying4gpm.util.checkStoragePermission
import com.geckour.nowplaying4gpm.util.cleaerSpotifyUserInfoImmediately
import com.geckour.nowplaying4gpm.util.executeCatching
import com.geckour.nowplaying4gpm.util.generate
import com.geckour.nowplaying4gpm.util.getAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.getArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.getChosePaletteColor
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getDonateBillingState
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.getMastodonUserInfo
import com.geckour.nowplaying4gpm.util.getPackageStateListPostMastodon
import com.geckour.nowplaying4gpm.util.getSpotifyUserInfo
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getTwitterAccessToken
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import com.geckour.nowplaying4gpm.util.readyForShare
import com.geckour.nowplaying4gpm.util.setAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.setArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.setFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.showErrorDialog
import com.geckour.nowplaying4gpm.util.storeDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.storeMastodonUserInfo
import com.geckour.nowplaying4gpm.util.storePackageStatePostMastodon
import com.geckour.nowplaying4gpm.util.storeTwitterAccessToken
import com.geckour.nowplaying4gpm.util.withCatching
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration
import com.sys1yagi.mastodon4j.api.method.Accounts
import com.sys1yagi.mastodon4j.api.method.Apps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest
import timber.log.Timber

class SettingsActivity : AppCompatActivity() {

    companion object {
        fun getIntent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    private val viewModel: SettingsViewModel by viewModel()
    private val sharedPreferences: SharedPreferences = get()

    private lateinit var billingApiClient: BillingApiClient

    private val twitterApiClient =
        TwitterApiClient(BuildConfig.TWITTER_CONSUMER_KEY, BuildConfig.TWITTER_CONSUMER_SECRET)

    private val mastodonScope = Scope(Scope.Name.ALL)
    private var mastodonRegistrationInfo: AppRegistration? = null

    private lateinit var requestUpdate: PermissionsRequester

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            requestNotificationListenerPermission { requestUpdate.launch() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestUpdate = constructPermissionsRequest(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            onPermissionDenied = ::onPermissionDenied,
            onNeverAskAgain = ::onNeverAskPermissionAgain,
            requiresPermission = ::invokeUpdate
        )

        setContent {
            SettingsTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Content(viewModel.setting.value, viewModel.settingsVisible) {
                        startActivity(LicensesActivity.getIntent(this@SettingsActivity))
                    }
                }
            }
        }

        billingApiClient = BillingApiClient(this) {
            lifecycleScope.launchWhenResumed {
                when (it) {
                    BillingApiClient.BillingResult.SUCCESS -> {
                        reflectDonation(viewModel.donated, true)
                    }
                    BillingApiClient.BillingResult.DUPLICATED -> {
                        showErrorDialog(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_already_purchase
                        )
                    }
                    BillingApiClient.BillingResult.CANCELLED -> {
                        showErrorDialog(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_on_cancel_purchase
                        )
                    }
                    BillingApiClient.BillingResult.FAILURE -> {
                        showErrorDialog(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_failure_purchase
                        )
                    }
                }
            }
        }

        observeEvents()

        showIgnoreBatteryOptimizationDialog()

        requestNotificationListenerPermission {
            requestUpdate.launch()
        }
    }

    override fun onResume() {
        super.onResume()

        checkStoragePermission(
            onNotGranted = { viewModel.settingsVisible.value = false },
            onGranted =  { viewModel.settingsVisible.value = true }
        )

        reflectDonation(viewModel.donated)

        if (sharedPreferences.getAlertTwitterAuthFlag()) {
            lifecycleScope.launchWhenResumed {
                showErrorDialog(
                    R.string.dialog_title_alert_must_auth_twitter,
                    R.string.dialog_message_alert_must_auth_twitter
                ) {
                    sharedPreferences.setAlertTwitterAuthFlag(false)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uriString = intent?.data?.toString()
        Timber.d("intent data: $uriString")
        when {
            uriString?.startsWith(SpotifyApiClient.SPOTIFY_CALLBACK) == true -> {
                onAuthSpotifyCallback(intent)
            }
            uriString?.startsWith(TwitterApiClient.TWITTER_CALLBACK) == true -> {
                onAuthTwitterCallback(
                    intent,
                    window.decorView,
                    viewModel.authTwitterSummary
                )
            }
            uriString?.startsWith(App.MASTODON_CALLBACK) == true -> {
                onAuthMastodonCallback(
                    intent,
                    window.decorView,
                    viewModel.authMastodonSummary
                )
            }
        }
    }

    private fun observeEvents() {
        viewModel.event.onEach {
            when (it) {
                is SettingsViewModel.Event.ChangeArtworkResolveOrder -> {
                    onClickChangeArtworkResolveOrder()
                }
                is SettingsViewModel.Event.ChangePatternFormat -> {
                    onClickItemPatternFormat(it.summary)
                }
                is SettingsViewModel.Event.EditPatternModifier -> {
                    onClickFormatPatternModifiers()
                }
                is SettingsViewModel.Event.AuthSpotify -> {
                    onClickAuthSpotify()
                }
                is SettingsViewModel.Event.AuthMastodon -> {
                    onClickAuthMastodon()
                }
                is SettingsViewModel.Event.SetMastodonPostDelay -> {
                    onClickDelayMastodon(it.summary)
                }
                is SettingsViewModel.Event.SetMastodonPostVisibility -> {
                    onClickVisibilityMastodon(it.summary)
                }
                is SettingsViewModel.Event.SelectPlayerPostMastodon -> {
                    onClickPlayerPackageMastodon()
                }
                is SettingsViewModel.Event.SelectNotificationColor -> {
                    onClickItemChooseColor(it.summary)
                }
                is SettingsViewModel.Event.AuthTwitter -> {
                    onClickAuthTwitter(window.decorView)
                }
                is SettingsViewModel.Event.Donate -> {
                    lifecycleScope.launch {
                        billingApiClient.startBilling(
                            this@SettingsActivity,
                            listOf(BuildConfig.SKU_KEY_DONATE)
                        )
                    }
                }
            }
        }.launchIn(lifecycleScope)

        viewModel.spotifyUserInfo.observe(this) { userInfo ->
            if (userInfo == null) {
                lifecycleScope.launchWhenResumed {
                    onAuthSpotifyError()
                }
                return@observe
            }

            viewModel.authSpotifySummary.value = getString(
                R.string.pref_item_summary_auth_spotify,
                userInfo.userName
            )
            viewModel.spotifyEnabledStates.forEach {
                it.value = true
            }
            Snackbar.make(
                window.decorView,
                R.string.snackbar_text_success_auth_spotify,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun requestNotificationListenerPermission(onGranted: () -> Unit = {}) {
        val notificationListenerNotEnabled =
            NotificationManagerCompat.getEnabledListenerPackages(this)
                .contains(packageName)
                .not()
        if (notificationListenerNotEnabled) {
            if (viewModel.showingNotificationServicePermissionDialog.not()) {
                viewModel.showingNotificationServicePermissionDialog = true
                AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_alert_grant_notification_listener)
                    .setMessage(R.string.dialog_message_alert_grant_notification_listener)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
                        activityResultLauncher.launch(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        dialog.dismiss()
                        viewModel.showingNotificationServicePermissionDialog = false
                    }.show()
            }
        } else onGranted()
    }

    private fun invokeUpdate() {
        viewModel.settingsVisible.value = true
        viewModel.reflectSetting()

        NotificationService.sendRequestInvokeUpdate(this)
    }

    private fun onPermissionDenied() {
        viewModel.settingsVisible.value = false
    }

    private fun onNeverAskPermissionAgain() {
        viewModel.settingsVisible.value = false
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

    private fun reflectDonation(state: MutableState<Boolean>, donated: Boolean? = null) {
        (donated ?: sharedPreferences.getDonateBillingState()).let {
            sharedPreferences.edit().putBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, it).apply()
            state.value = it
        }
        billingApiClient.requestUpdate()
    }

    private fun onClickItemPatternFormat(summary: MutableState<String?>) {
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
                    summary.value = pattern
                }
            }
            requestUpdate.launch()
            dialog.dismiss()
        }.show()
    }

    private suspend fun onAuthSpotifyError() {
        showErrorDialog(
            R.string.dialog_title_alert_failure_auth_spotify,
            R.string.dialog_message_alert_failure_auth_spotify
        )
    }

    private suspend fun onAuthMastodonError() {
        showErrorDialog(
            R.string.dialog_title_alert_failure_auth_mastodon,
            R.string.dialog_message_alert_failure_auth_mastodon
        )
    }

    private suspend fun onAuthTwitterError() {
        showErrorDialog(
            R.string.dialog_title_alert_failure_auth_twitter,
            R.string.dialog_message_alert_failure_auth_twitter
        )
    }

    private fun onClickChangeArtworkResolveOrder() {
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
            requestUpdate.launch()
            dialog.dismiss()
        }.show()
    }

    private fun onClickFormatPatternModifiers() {
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
            requestUpdate.launch()
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

    private fun onClickAuthMastodon() {
        val instanceNameInputDialogBinding = DialogAutoCompleteEditTextBinding.inflate(
            LayoutInflater.from(this), null, false
        ).apply {
            hint = getString(R.string.dialog_hint_mastodon_instance)
            editText.setText(sharedPreferences.getMastodonUserInfo()?.instanceName)
            editText.setSelection(editText.text.length)

            lifecycleScope.launch(Dispatchers.IO) {
                val instances = get<MastodonInstancesApiClient>().getList()
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
                                    }
                                }, Gson()).build()
                            val registrationInfo = Apps(mastodonApiClient).createApp(
                                App.MASTODON_CLIENT_NAME,
                                App.MASTODON_CALLBACK,
                                mastodonScope,
                                App.MASTODON_WEB_URL
                            ).executeCatching {
                                Timber.e(it)
                                FirebaseCrashlytics.getInstance().recordException(it)
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

    private fun onClickDelayMastodon(summary: MutableState<String?>) {
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
                        summary.value =
                            getString(R.string.pref_item_summary_delay_mastodon, duration)
                    } else {
                        lifecycleScope.launchWhenResumed {
                            showErrorDialog(
                                R.string.dialog_title_alert_invalid_duration_value,
                                R.string.dialog_message_alert_invalid_duration_value
                            )
                        }
                    }
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickVisibilityMastodon(summary: MutableState<String?>) {
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
                    summary.value =
                        getString(Visibility.getFromIndex(visibilityIndex).getSummaryResId())
                    requestUpdate.launch()
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onClickPlayerPackageMastodon() {
        val adapter =
            PlayerPackageListAdapter(
                sharedPreferences.getPackageStateListPostMastodon()
                    .mapNotNull { packageState ->
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
                        } else PlayerPackageState(
                            packageState.packageName,
                            appName,
                            packageState.state
                        )
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
            requestUpdate.launch()
            dialog.dismiss()
        }.show()
    }

    private fun onClickFab(sharedPreferences: SharedPreferences) {
        if (sharedPreferences.readyForShare(this).not()) {
            lifecycleScope.launchWhenResumed {
                showErrorDialog(
                    R.string.dialog_title_alert_no_metadata,
                    R.string.dialog_message_alert_no_metadata
                )
            }
            return
        }

        startActivity(SharingActivity.getIntent(this))
    }

    private fun onClickItemChooseColor(summary: MutableState<String?>) {
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
                    summary.value = getString(
                        PaletteColor.getFromIndex(paletteIndex).getSummaryResId()
                    )
                    requestUpdate.launch()
                }
            }
            dialog.dismiss()
        }.show()
    }

    private fun onAuthSpotifyCallback(
        intent: Intent
    ) {
        val verifier = intent.data?.getQueryParameter("code")
        if (verifier == null) {
            lifecycleScope.launchWhenResumed {
                onAuthSpotifyError()
            }
            return
        }

        viewModel.storeSpotifyUserInfo(verifier)
        viewModel.spotifyAuthenticated.value = true
    }

    private fun onAuthTwitterCallback(
        intent: Intent,
        rootView: View,
        summary: MutableState<String?>
    ) {
        sharedPreferences.setAlertTwitterAuthFlag(false)

        val verifier = intent.data?.getQueryParameter("oauth_verifier")
        if (verifier == null) {
            lifecycleScope.launchWhenResumed {
                onAuthTwitterError()
            }
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val accessToken = twitterApiClient.getAccessToken(verifier)

            if (accessToken == null) {
                onAuthTwitterError()
                return@launch
            }

            sharedPreferences.storeTwitterAccessToken(accessToken)

            summary.value = accessToken.screenName
            viewModel.reflectSetting()
            Snackbar.make(
                rootView,
                R.string.snackbar_text_success_auth_twitter,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun onAuthMastodonCallback(
        intent: Intent,
        rootView: View,
        summary: MutableState<String?>
    ) {
        mastodonRegistrationInfo?.apply {
            val token = intent.data?.getQueryParameter("code")
            if (token == null) {
                lifecycleScope.launchWhenResumed {
                    onAuthMastodonError()
                }
                return
            }

            val mastodonApiClientBuilder = MastodonClient.Builder(
                this@apply.instanceName, OkHttpClient.Builder().apply {
                    if (BuildConfig.DEBUG) {
                        addNetworkInterceptor(
                            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                        )
                    }
                }, Gson()
            )
            lifecycleScope.launch(Dispatchers.IO) {
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

                summary.value = getString(
                    R.string.pref_item_summary_auth_mastodon,
                    userInfo.userName,
                    userInfo.instanceName
                )
                viewModel.reflectSetting()
                Snackbar.make(
                    rootView, R.string.snackbar_text_success_auth_mastodon, Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private data class EasterEggTag(
        val count: Int,
        val time: Long
    )

    @Composable
    fun Content(setting: SettingsViewModel.Setting, settingsVisible: MutableState<Boolean>, onEasterEggActivate: () -> Unit) {
        Box {
            Column {
                SettingTopBar(onEasterEggActivate)
                Settings(Modifier.weight(1f), setting.categories)
            }
            FloatingActionButton(
                onClick = {

                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                backgroundColor = if (isSystemInDarkTheme()) DarkRed else LightRed,
                contentColor = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            if (settingsVisible.value.not()) {
                Box(
                    modifier = Modifier
                        .background(if (isSystemInDarkTheme()) SmokeBlack else SmokeWhite)
                        .fillMaxSize()
                        .clickable {}
                ) {
                    Text(
                        text = stringResource(id = R.string.pref_mask_inactive_app_desc),
                        modifier = Modifier.align(Center)
                    )
                }
            }
        }
    }

    @Composable
    fun SettingTopBar(onEasterEggActivate: () -> Unit) {
        val tag = remember { mutableStateOf(EasterEggTag(0, -1L)) }
        val countLimit = 7
        TopAppBar(
            backgroundColor = if (isSystemInDarkTheme()) DeepRed else LightRed,
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.clickable {
                val timeLimit = 300L
                val count = tag.value.count + 1
                val time = tag.value.time
                val now = System.currentTimeMillis()
                tag.value = if (count < countLimit) {
                    if (time < 0 || now - time < timeLimit) EasterEggTag(count, now)
                    else EasterEggTag(0, -1L)
                } else {
                    onEasterEggActivate()
                    EasterEggTag(0, -1L)
                }
            }
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(
                    text = "${stringResource(R.string.activity_title_settings)} - ${stringResource(R.string.app_name)}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    fun Settings(modifier: Modifier, categories: List<SettingsViewModel.Setting.Category>) {
        LazyColumn(modifier = modifier) {
            items(categories) {
                if (it.items.any { it.visible }) Category(it)
            }
        }
    }

    @Composable
    fun Category(category: SettingsViewModel.Setting.Category) {
        Column {
            Text(
                text = stringResource(category.nameStringRes),
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isSystemInDarkTheme()) CleanBlue else DeepBlue,
                modifier = Modifier.padding(8.dp)
            )
            category.items.forEach {
                if (it.visible) Item(it)
            }
        }
    }

    @Composable
    fun Item(item: SettingsViewModel.Setting.Category.Item) {
        Box(modifier = Modifier.height(IntrinsicSize.Max)) {
            Row(modifier = Modifier.clickable {
                item.switchPrefKey?.let {
                    item.switchState.value = item.switchState.value?.not()
                }
                item.onClick(item)
            }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 24.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(item.titleStringRes),
                        maxLines = 2,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) LightRed else DarkRed,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Text(
                        text = stringResource(item.descStringRes),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                    )
                    item.summary.value?.let {
                        Text(
                            text = it,
                            maxLines = 1,
                            fontSize = 12.sp,
                            color = if (isSystemInDarkTheme()) SmokeWhite else InkBlackWeak,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                item.switchPrefKey?.let {
                    SettingSwitch(this, item.switchState, it, item.summary) {
                        item.onSwitchCheckedChanged(it)
                    }
                }
            }
            if (item.enabled.value.not()) {
                Box(
                    modifier = Modifier
                        .background(if (isSystemInDarkTheme()) SmokeBlack else SmokeWhite)
                        .fillMaxSize()
                        .clickable {}
                )
            }
        }
    }

    @Composable
    fun SettingSwitch(scope: RowScope, switchState: MutableState<Boolean?>, prefKey: PrefKey, summary: MutableState<String?>, onCheckedChanged: ((Boolean) -> Unit)?) {
        with(scope) {
            switchState.value?.let {
                sharedPreferences.edit { putBoolean(prefKey.name, it) }
                onCheckedChanged?.invoke(it)
            }
            summary.value = switchState.value?.switchSummary
            Switch(
                checked = switchState.value ?: false,
                onCheckedChange = {
                    sharedPreferences.edit { putBoolean(prefKey.name, it) }
                    switchState.value = it
                    summary.value = it.switchSummary
                    onCheckedChanged?.invoke(it)
                },
                modifier = Modifier
                    .align(CenterVertically)
                    .padding(8.dp)
            )
        }
    }

    private val Boolean.switchSummary
        get() = getString(
            if (this) R.string.pref_item_summary_switch_on
            else R.string.pref_item_summary_switch_off
        )
}