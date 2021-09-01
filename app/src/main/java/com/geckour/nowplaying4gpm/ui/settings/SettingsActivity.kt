package com.geckour.nowplaying4gpm.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geckour.nowplaying4gpm.App
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.MastodonInstancesApiClient
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.domain.model.MastodonUserInfo
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.compose.CleanBlue
import com.geckour.nowplaying4gpm.ui.compose.DarkRed
import com.geckour.nowplaying4gpm.ui.compose.DeepBlue
import com.geckour.nowplaying4gpm.ui.compose.DeepRed
import com.geckour.nowplaying4gpm.ui.compose.InkBlackWeak
import com.geckour.nowplaying4gpm.ui.compose.LightRed
import com.geckour.nowplaying4gpm.ui.compose.MilkBlack
import com.geckour.nowplaying4gpm.ui.compose.MilkWhite
import com.geckour.nowplaying4gpm.ui.compose.SettingsTheme
import com.geckour.nowplaying4gpm.ui.compose.SmokeBlack
import com.geckour.nowplaying4gpm.ui.compose.SmokeWhite
import com.geckour.nowplaying4gpm.ui.compose.TarBlack
import com.geckour.nowplaying4gpm.ui.license.LicensesActivity
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.ui.widget.adapter.ArtworkResolveMethodListAdapter
import com.geckour.nowplaying4gpm.util.PaletteColor
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.Visibility
import com.geckour.nowplaying4gpm.util.checkStoragePermission
import com.geckour.nowplaying4gpm.util.clearSpotifyUserInfoImmediately
import com.geckour.nowplaying4gpm.util.executeCatching
import com.geckour.nowplaying4gpm.util.getAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.getArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.getChosePaletteColor
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getDonateBillingState
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.getPackageStateListPostMastodon
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import com.geckour.nowplaying4gpm.util.readyForShare
import com.geckour.nowplaying4gpm.util.setAlertTwitterAuthFlag
import com.geckour.nowplaying4gpm.util.setArtworkResolveOrder
import com.geckour.nowplaying4gpm.util.setFormatPatternModifiers
import com.geckour.nowplaying4gpm.util.storeDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.storeMastodonUserInfo
import com.geckour.nowplaying4gpm.util.storePackageStatePostMastodon
import com.geckour.nowplaying4gpm.util.storeTwitterAccessToken
import com.geckour.nowplaying4gpm.util.withCatching
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

        viewModel.patternFormatSummary.value = sharedPreferences.getFormatPattern(this)

        setContent {
            SettingsTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Content(viewModel.settingsVisible) {
                        startActivity(LicensesActivity.getIntent(this@SettingsActivity))
                    }
                }
            }
        }

        billingApiClient = BillingApiClient(this, onError = {
            viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
                R.string.dialog_title_alert_failure_purchase,
                R.string.dialog_message_alert_on_start_purchase
            )
        }) {
            lifecycleScope.launchWhenResumed {
                when (it) {
                    BillingApiClient.BillingResult.SUCCESS -> {
                        reflectDonation(viewModel.donated, true)
                    }
                    BillingApiClient.BillingResult.DUPLICATED -> {
                        viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_already_purchase
                        )
                        reflectDonation(viewModel.donated, true)
                    }
                    BillingApiClient.BillingResult.CANCELLED -> {
                        viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_on_cancel_purchase
                        )
                    }
                    BillingApiClient.BillingResult.FAILURE -> {
                        viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_failure_purchase
                        )
                    }
                }
            }
        }

        showIgnoreBatteryOptimizationDialog()

        requestNotificationListenerPermission {
            requestUpdate.launch()
        }
    }

    override fun onResume() {
        super.onResume()

        checkStoragePermission(
            onNotGranted = { viewModel.settingsVisible.value = false },
            onGranted = { viewModel.settingsVisible.value = true }
        )

        reflectDonation(viewModel.donated)

        if (sharedPreferences.getAlertTwitterAuthFlag()) {
            lifecycleScope.launchWhenResumed {
                viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
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
                    viewModel.authTwitterSummary
                )
            }
            uriString?.startsWith(App.MASTODON_CALLBACK) == true -> {
                onAuthMastodonCallback(
                    intent,
                    viewModel.authMastodonSummary
                )
            }
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

        NotificationService.sendRequestInvokeUpdate(this)
    }

    private fun onPermissionDenied() {
        viewModel.settingsVisible.value = false
    }

    private fun onNeverAskPermissionAgain() {
        viewModel.settingsVisible.value = false
    }

    @SuppressLint("BatteryLife")
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
                    sharedPreferences.edit {
                        putBoolean(PrefKey.PREF_KEY_DENIED_IGNORE_BATTERY_OPTIMIZATION.name, true)
                    }
                    dialog.dismiss()
                }
                .setOnCancelListener {
                    sharedPreferences.edit {
                        putBoolean(PrefKey.PREF_KEY_DENIED_IGNORE_BATTERY_OPTIMIZATION.name, true)
                    }
                }
                .show()
        }
    }

    private fun reflectDonation(state: MutableState<Boolean>, donated: Boolean? = null) {
        (donated ?: sharedPreferences.getDonateBillingState()).let {
            sharedPreferences.edit { putBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, it) }
            state.value = it
        }
        billingApiClient.requestUpdate()
    }

    private fun onAuthSpotifyError() {
        viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
            R.string.dialog_title_alert_failure_auth_spotify,
            R.string.dialog_message_alert_failure_auth_spotify
        )
    }

    private fun onAuthMastodonError() {
        viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
            R.string.dialog_title_alert_failure_auth_mastodon,
            R.string.dialog_message_alert_failure_auth_mastodon
        )
    }

    private fun onAuthTwitterError() {
        viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
            R.string.dialog_title_alert_failure_auth_twitter,
            R.string.dialog_message_alert_failure_auth_twitter
        )
    }

    private fun onClickAuthSpotify() {
        sharedPreferences.clearSpotifyUserInfoImmediately()
        CustomTabsIntent.Builder().setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(getColor(R.color.colorPrimary))
                    .build()
            )
            .build()
            .launchUrl(this, Uri.parse(SpotifyApiClient.OAUTH_URL))
    }

    private fun onClickAuthTwitter() {
        lifecycleScope.launchWhenResumed {
            val uri = twitterApiClient.getRequestOAuthUri() ?: run {
                viewModel.snackbarHostState.value
                    .showSnackbar(message = getString(R.string.snackbar_text_failure_auth_twitter))
                return@launchWhenResumed
            }

            CustomTabsIntent.Builder().setShowTitle(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(getColor(R.color.colorPrimary))
                        .build()
                )
                .build()
                .launchUrl(this@SettingsActivity, uri)
        }
    }

    private fun onClickFab() {
        if (sharedPreferences.readyForShare(this).not()) {
            lifecycleScope.launchWhenResumed {
                viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
                    R.string.dialog_title_alert_no_metadata,
                    R.string.dialog_message_alert_no_metadata
                )
            }
            return
        }

        startActivity(SharingActivity.getIntent(this))
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
    }

    private fun onAuthTwitterCallback(
        intent: Intent,
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

        lifecycleScope.launchWhenResumed {
            val accessToken = twitterApiClient.getAccessToken(verifier)

            if (accessToken == null) {
                onAuthTwitterError()
                return@launchWhenResumed
            }

            sharedPreferences.storeTwitterAccessToken(accessToken)

            summary.value = accessToken.screenName
            invokeUpdate()
            viewModel.snackbarHostState.value
                .showSnackbar(getString(R.string.snackbar_text_success_auth_twitter))
        }
    }

    private fun onAuthMastodonCallback(
        intent: Intent,
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
                invokeUpdate()
                viewModel.snackbarHostState.value
                    .showSnackbar(getString(R.string.snackbar_text_success_auth_mastodon))
            }
        }
    }

    @Composable
    fun Content(
        settingsVisible: MutableState<Boolean>,
        onEasterEggActivate: () -> Unit
    ) {
        val lazyListState = rememberLazyListState()

        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                SettingTopBar(onEasterEggActivate)
                Settings(Modifier.weight(1f), lazyListState)
            }

            if (lazyListState.isScrollInProgress || lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index != lazyListState.layoutInfo.totalItemsCount - 1) {
                FloatingActionButton(
                    onClick = { onClickFab() },
                    modifier = Modifier
                        .align(BottomEnd)
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

            SettingsSnackbar(this)

            Dialogs()
        }
    }

    @Composable
    fun SettingsSnackbar(scope: BoxScope) {
        with(scope) {
            SnackbarHost(
                hostState = viewModel.snackbarHostState.value,
                modifier = Modifier.align(BottomCenter)
            ) {
                Snackbar(
                    snackbarData = it,
                    backgroundColor = MaterialTheme.colors.onBackground,
                    contentColor = MaterialTheme.colors.background,
                )
            }
        }
    }

    @Composable
    fun Dialogs() {
        Box(modifier = Modifier.fillMaxSize()) {
            if (viewModel.openChangeArtworkResolveOrderDialog.value) {
                ChangeArtworkResolveOrderDialog()
            }
            if (viewModel.openChangePatternFormatDialog.value) {
                ChangePatternFormatDialog()
            }
            if (viewModel.openAuthMastodonDialog.value) {
                AuthMastodonDialog()
            }
            if (viewModel.openEditPatternModifierDialog.value) {
                EditPatternModifierDialog()
            }
            if (viewModel.openSetMastodonPostDelayDialog.value) {
                SetMastodonPostDelayDialog()
            }
            if (viewModel.openSetMastodonPostVisibilityDialog.value) {
                SetMastodonPostVisibilityDialog()
            }
            if (viewModel.openSelectPlayerPostMastodonDialog.value) {
                SelectPlayerPostMastodonDialog()
            }
            if (viewModel.openSelectNotificationColorDialog.value) {
                SelectNotificationColorDialog()
            }
            viewModel.errorDialogData.value?.let {
                ErrorDialog(it)
            }
        }
    }

    @Composable
    fun ErrorDialog(errorDialogData: SettingsViewModel.ErrorDialogData) {
        AlertDialog(
            onDismissRequest = { viewModel.errorDialogData.value = null },
            confirmButton = {
                TextButton(onClick = { viewModel.errorDialogData.value = null }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            title = {
                Text(text = stringResource(id = errorDialogData.titleRes))
            },
            text = {
                Text(text = stringResource(id = errorDialogData.textRes))
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }

    @Composable
    fun ChangeArtworkResolveOrderDialog() {
        val adapter = ArtworkResolveMethodListAdapter(
            sharedPreferences.getArtworkResolveOrder().toMutableList()
        )
        AlertDialog(
            onDismissRequest = { viewModel.openChangeArtworkResolveOrderDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    sharedPreferences.setArtworkResolveOrder(adapter.items)
                    viewModel.openChangeArtworkResolveOrderDialog.value = false
                    invokeUpdate()
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.openChangeArtworkResolveOrderDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_artwork_resolve_order))
            },
            text = {
                Column {
                    Text(text = stringResource(id = R.string.dialog_message_artwork_resolve_order))
                    AndroidView(factory = {
                        FrameLayout(it).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                            )
                            addView(
                                RecyclerView(it).apply {
                                    layoutParams = RecyclerView.LayoutParams(
                                        RecyclerView.LayoutParams.MATCH_PARENT,
                                        RecyclerView.LayoutParams.WRAP_CONTENT
                                    )
                                    layoutManager =
                                        LinearLayoutManager(it, RecyclerView.VERTICAL, false)
                                    adapter.itemTouchHolder.attachToRecyclerView(this)
                                    this.adapter = adapter
                                }
                            )
                        }
                    })
                }
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }

    @Composable
    fun ChangePatternFormatDialog() {
        var text by remember {
            mutableStateOf(sharedPreferences.getFormatPattern(this@SettingsActivity))
        }
        AlertDialog(
            onDismissRequest = { viewModel.openChangePatternFormatDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    sharedPreferences.edit {
                        putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, text)
                    }
                    viewModel.patternFormatSummary.value = text
                    viewModel.openChangePatternFormatDialog.value = false
                    invokeUpdate()
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.openChangePatternFormatDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_pattern_format))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_message_pattern_format),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        maxLines = 1
                    )
                }
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }

    @Composable
    fun EditPatternModifierDialog() {
        val items = sharedPreferences.getFormatPatternModifiers().toMutableList()
        AlertDialog(
            onDismissRequest = { viewModel.openEditPatternModifierDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    sharedPreferences.edit {
                        sharedPreferences.setFormatPatternModifiers(items)
                        viewModel.openEditPatternModifierDialog.value = false
                        invokeUpdate()
                    }
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.openEditPatternModifierDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_format_pattern_modifier))
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_message_format_pattern_modifier),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn {
                        itemsIndexed(items) { index, item ->
                            var prefixText by remember { mutableStateOf("") }
                            var suffixText by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = CenterVertically
                            ) {
                                BasicTextField(
                                    value = prefixText,
                                    onValueChange = {
                                        prefixText = it
                                        items[index] = items[index].copy(prefix = it)
                                    },
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth()
                                        .weight(1f),
                                    decorationBox = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = BottomEnd
                                        ) {
                                            if (prefixText.isEmpty()) {
                                                Text(
                                                    text = stringResource(
                                                        id = R.string.dialog_hint_format_pattern_prefix
                                                    )
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .height(0.5.dp)
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colors.onBackground)
                                            )
                                        }
                                        it()
                                    },
                                    cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                                    textStyle = TextStyle.Default.copy(
                                        color = MaterialTheme.colors.onBackground,
                                        textAlign = TextAlign.End
                                    )
                                )
                                Text(
                                    text = item.key.value,
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                                    color = MaterialTheme.colors.secondary
                                )
                                BasicTextField(
                                    value = suffixText,
                                    onValueChange = {
                                        suffixText = it
                                        items[index] = items[index].copy(prefix = it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    decorationBox = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = BottomStart
                                        ) {
                                            if (suffixText.isEmpty()) {
                                                Text(
                                                    text = stringResource(
                                                        id = R.string.dialog_hint_format_pattern_suffix
                                                    )
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .height(0.5.dp)
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colors.onBackground)
                                            )
                                        }
                                        it()
                                    },
                                    cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                                    textStyle = TextStyle.Default.copy(
                                        color = MaterialTheme.colors.onBackground,
                                        textAlign = TextAlign.Start
                                    )
                                )
                            }
                        }
                    }
                }
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }

    @Composable
    fun AuthMastodonDialog() {
        var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var instances by remember { mutableStateOf(emptyList<String>()) }
        var showAutoCompleteList by remember { mutableStateOf(false) }

        LaunchedEffect(System.currentTimeMillis()) {
            instances = get<MastodonInstancesApiClient>().getList().mapNotNull { it.name }
        }
        AlertDialog(
            onDismissRequest = { viewModel.openAuthMastodonDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    if (textFieldValue.text.isNotBlank()) {
                        lifecycleScope.launch {
                            val mastodonApiClient =
                                MastodonClient.Builder(
                                    textFieldValue.text,
                                    OkHttpClient.Builder().apply {
                                        if (BuildConfig.DEBUG) {
                                            addNetworkInterceptor(
                                                HttpLoggingInterceptor().setLevel(
                                                    HttpLoggingInterceptor.Level.BODY
                                                )
                                            )
                                        }
                                    },
                                    Gson()
                                ).build()

                            val authUrl = withContext(Dispatchers.IO) {
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
                                    return@withContext null
                                }
                                mastodonRegistrationInfo = registrationInfo

                                Apps(mastodonApiClient).getOAuthUrl(
                                    registrationInfo.clientId, mastodonScope, App.MASTODON_CALLBACK
                                )
                            } ?: return@launch

                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setDefaultColorSchemeParams(
                                    CustomTabColorSchemeParams.Builder()
                                        .setToolbarColor(getColor(R.color.colorPrimary))
                                        .build()
                                )
                                .build()
                                .launchUrl(this@SettingsActivity, Uri.parse(authUrl))
                        }
                    }
                    viewModel.openAuthMastodonDialog.value = false
                    invokeUpdate()
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.openAuthMastodonDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_mastodon_instance))
            },
            text = {
                Column(modifier = Modifier.requiredHeight(200.dp)) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            showAutoCompleteList = true
                        },
                        singleLine = true,
                        maxLines = 1,
                        label = {
                            Text(text = stringResource(id = R.string.dialog_hint_mastodon_instance))
                        }
                    )
                    if (showAutoCompleteList && textFieldValue.text.isNotBlank()) {
                        Card(elevation = 4.dp, backgroundColor = MaterialTheme.colors.background) {
                            LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                                items(instances.filter { it.contains(textFieldValue.text) }) { item ->
                                    Text(
                                        text = item,
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .fillMaxWidth()
                                            .clickable {
                                                textFieldValue = textFieldValue.copy(
                                                    text = item,
                                                    selection = TextRange(item.length),
                                                    composition = null
                                                )
                                                showAutoCompleteList = false
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }

    @Composable
    fun SetMastodonPostDelayDialog() {
        var duration by remember {
            mutableStateOf(sharedPreferences.getDelayDurationPostMastodon().toString())
        }
        AlertDialog(
            onDismissRequest = { viewModel.openSetMastodonPostDelayDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    duration.toLongOrNull()?.let {
                        if (it in (500..60000)) {
                            sharedPreferences.storeDelayDurationPostMastodon(it)
                            viewModel.postMastodonDelaySummary.value = it
                            invokeUpdate()
                        } else {
                            null
                        }
                    } ?: run {
                        duration = sharedPreferences.getDelayDurationPostMastodon().toString()
                        lifecycleScope.launchWhenResumed {
                            viewModel.errorDialogData.value = SettingsViewModel.ErrorDialogData(
                                R.string.dialog_title_alert_invalid_duration_value,
                                R.string.dialog_message_alert_invalid_duration_value
                            )
                        }
                    }
                    viewModel.openSetMastodonPostDelayDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.openSetMastodonPostDelayDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_mastodon_delay))
            },
            text = {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = {
                        Text(text = stringResource(id = R.string.dialog_hint_mastodon_delay))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }


    @Composable
    fun SetMastodonPostVisibilityDialog() {
        var visibility by remember { mutableStateOf(sharedPreferences.getVisibilityMastodon()) }
        AlertDialog(
            onDismissRequest = { viewModel.openSetMastodonPostVisibilityDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    val visibilityIndex = Visibility.values().indexOf(visibility)
                    sharedPreferences.edit {
                        putInt(PrefKey.PREF_KEY_CHOSEN_MASTODON_VISIBILITY.name, visibilityIndex)
                    }
                    viewModel.postMastodonVisibilitySummary.value =
                        getString(visibility.getSummaryResId())
                    viewModel.openSetMastodonPostVisibilityDialog.value = false
                    invokeUpdate()
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    visibility = sharedPreferences.getVisibilityMastodon()
                    viewModel.openSetMastodonPostDelayDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_mastodon_visibility))
            },
            text = {
                var showCandidate by remember { mutableStateOf(false) }
                Column(modifier = Modifier.requiredHeight(200.dp)) {
                    Text(
                        text = stringResource(id = R.string.dialog_message_mastodon_visibility),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box {
                        OutlinedTextField(
                            value = stringResource(id = visibility.getSummaryResId()),
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .clickable { showCandidate = true }
                        )
                        if (showCandidate) {
                            Card(backgroundColor = MaterialTheme.colors.background) {
                                LazyColumn(modifier = Modifier.padding(vertical = 6.dp)) {
                                    items(Visibility.values()) {
                                        Text(
                                            text = stringResource(id = it.getSummaryResId()),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    visibility = it
                                                    showCandidate = false
                                                }
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }

    @Composable
    fun SelectPlayerPostMastodonDialog() {
        var packageStates by remember {
            mutableStateOf(sharedPreferences.getPackageStateListPostMastodon())
        }
        AlertDialog(
            onDismissRequest = { viewModel.openSelectPlayerPostMastodonDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    packageStates.forEach {
                        sharedPreferences.storePackageStatePostMastodon(it.packageName, it.state)
                    }
                    viewModel.openSelectPlayerPostMastodonDialog.value = false
                    invokeUpdate()
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.openSelectPlayerPostMastodonDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_player_package_mastodon))
            },
            text = {
                Column {
                    Text(text = stringResource(id = R.string.dialog_message_player_package_mastodon))
                    packageStates.forEachIndexed { index, packageState ->
                        val appName = packageManager?.let {
                            withCatching {
                                it.getApplicationLabel(
                                    it.getApplicationInfo(
                                        packageState.packageName,
                                        PackageManager.GET_META_DATA
                                    )
                                )
                            }
                        }?.toString() ?: return@forEachIndexed
                        Row(
                            modifier = Modifier
                                .clickable {
                                    packageStates = packageStates
                                        .toMutableList()
                                        .apply {
                                            this[index] = this[index].let {
                                                it.copy(state = it.state.not())
                                            }
                                        }
                                }
                                .padding(vertical = 4.dp, horizontal = 16.dp),
                            verticalAlignment = CenterVertically
                        ) {
                            Text(
                                text = appName,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .fillMaxWidth()
                                    .weight(1f),
                                color = MaterialTheme.colors.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Switch(
                                checked = packageState.state,
                                onCheckedChange = { checked ->
                                    packageStates = packageStates.toMutableList().apply {
                                        this[index] = this[index].copy(state = checked)
                                    }
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.75.dp)
                                .background(MaterialTheme.colors.secondary)
                        )
                    }
                }
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
    }

    @Composable
    fun SelectNotificationColorDialog() {
        var paletteColor by remember { mutableStateOf(sharedPreferences.getChosePaletteColor()) }
        AlertDialog(
            onDismissRequest = { viewModel.openSelectNotificationColorDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    val paletteIndex = PaletteColor.values().indexOf(paletteColor)
                    sharedPreferences.edit {
                        putInt(PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name, paletteIndex)
                    }
                    viewModel.chosePaletteColorSummary.value =
                        getString(paletteColor.getSummaryResId())
                    viewModel.openSelectNotificationColorDialog.value = false
                    invokeUpdate()
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    paletteColor = sharedPreferences.getChosePaletteColor()
                    viewModel.openSelectNotificationColorDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.dialog_button_ng))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.dialog_title_mastodon_visibility))
            },
            text = {
                var showCandidate by remember { mutableStateOf(false) }
                Column(modifier = Modifier.requiredHeight(200.dp)) {
                    Text(
                        text = stringResource(id = R.string.dialog_message_mastodon_visibility),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box {
                        OutlinedTextField(
                            value = stringResource(id = paletteColor.getSummaryResId()),
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .clickable { showCandidate = true }
                        )
                        if (showCandidate) {
                            Card(backgroundColor = MaterialTheme.colors.background) {
                                LazyColumn(modifier = Modifier.padding(vertical = 6.dp)) {
                                    items(PaletteColor.values()) {
                                        Text(
                                            text = stringResource(id = it.getSummaryResId()),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    paletteColor = it
                                                    showCandidate = false
                                                }
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        )
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
    fun Settings(modifier: Modifier, lazyListState: LazyListState) {
        val mastodonEnabledState = remember { mutableStateOf(false) }
        LazyColumn(modifier = modifier, state = lazyListState) {
            item { Category(R.string.pref_category_general) }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_change_artwork_resolve_order,
                            R.string.pref_item_desc_change_artwork_resolve_order
                        ) { viewModel.openChangeArtworkResolveOrderDialog.value = true }
                    }
                }
                Item(item = item)
            }

            item { Category(R.string.pref_category_share) }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_pattern,
                            R.string.pref_item_desc_pattern,
                            summary = viewModel.patternFormatSummary
                        ) { viewModel.openChangePatternFormatDialog.value = true }
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_pattern_modifiers,
                            R.string.pref_item_desc_pattern_modifiers
                        ) { viewModel.openEditPatternModifierDialog.value = true }
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_simplify_share,
                            R.string.pref_item_desc_simplify_share,
                            PrefKey.PREF_KEY_WHETHER_USE_SIMPLE_SHARE
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_auth_spotify,
                            R.string.pref_item_desc_auth_spotify,
                            summary = viewModel.authSpotifySummary
                        ) { onClickAuthSpotify() }
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_use_spotify_data,
                            R.string.pref_item_desc_use_spotify_data,
                            PrefKey.PREF_KEY_WHETHER_USE_SPOTIFY_DATA,
                            enabled = viewModel.spotifyEnabledState
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_strict_match_pattern,
                            R.string.pref_item_desc_strict_match_pattern,
                            PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_bundle_artwork,
                            R.string.pref_item_desc_switch_bundle_artwork,
                            PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_copy_into_clipboard,
                            R.string.pref_item_desc_switch_copy_into_clipboard,
                            PrefKey.PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_auto_post_mastodon,
                            R.string.pref_item_desc_switch_auto_post_mastodon,
                            PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON,
                            onSwitchCheckedChanged = { state ->
                                mastodonEnabledState.value = state
                                invokeUpdate()
                            }
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_auth_mastodon,
                            R.string.pref_item_desc_auth_mastodon,
                            summary = viewModel.authMastodonSummary,
                            enabled = mastodonEnabledState
                        ) { viewModel.openAuthMastodonDialog.value = true }
                    }
                }
                Item(item = item)
            }
            item {
                val summary = stringResource(
                    id = R.string.pref_item_summary_delay_mastodon,
                    viewModel.postMastodonDelaySummary.value
                )
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_delay_mastodon,
                            R.string.pref_item_desc_delay_mastodon,
                            summary = mutableStateOf(summary),
                            enabled = mastodonEnabledState
                        ) { viewModel.openSetMastodonPostDelayDialog.value = true }
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_visibility_mastodon,
                            R.string.pref_item_desc_visibility_mastodon,
                            summary = viewModel.postMastodonVisibilitySummary,
                            enabled = mastodonEnabledState
                        ) { viewModel.openSetMastodonPostVisibilityDialog.value = true }
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_player_package_mastodon,
                            R.string.pref_item_desc_player_package_mastodon,
                            enabled = mastodonEnabledState
                        ) { viewModel.openSelectPlayerPostMastodonDialog.value = true }
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_success_notification_mastodon,
                            R.string.pref_item_desc_switch_success_notification_mastodon,
                            PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON,
                            enabled = mastodonEnabledState
                        )
                    }
                }
                Item(item = item)
            }

            item { Category(titleRes = R.string.pref_category_notification) }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_reside_notification,
                            R.string.pref_item_desc_switch_reside_notification,
                            PrefKey.PREF_KEY_WHETHER_RESIDE
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_show_artwork_in_notification,
                            R.string.pref_item_desc_switch_show_artwork_in_notification,
                            PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_choose_color,
                            R.string.pref_item_desc_choose_color,
                            summary = viewModel.chosePaletteColorSummary
                        ) { viewModel.openSelectNotificationColorDialog.value = true }
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_colorize_notification_bg,
                            R.string.pref_item_desc_switch_colorize_notification_bg,
                            PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG
                        )
                    }
                }
                Item(item = item)
            }

            item { Category(titleRes = R.string.pref_category_widget) }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_show_artwork_in_widget,
                            R.string.pref_item_desc_switch_show_artwork_in_widget,
                            PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_launch_player_on_click_widget_artwork,
                            R.string.pref_item_desc_switch_launch_player_on_click_widget_artwork,
                            PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK
                        )
                    }
                }
                Item(item = item)
            }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_show_clear_button_in_widget,
                            R.string.pref_item_desc_switch_show_clear_button_in_widget,
                            PrefKey.PREF_KEY_WHETHER_SHOW_CLEAR_BUTTON_IN_WIDGET
                        )
                    }
                }
                Item(item = item)
            }

            item { Category(titleRes = R.string.pref_category_wear) }
            item {
                val item by remember {
                    derivedStateOf {
                        SettingsViewModel.Item(
                            sharedPreferences,
                            R.string.pref_item_title_auth_twitter,
                            R.string.pref_item_desc_auth_twitter,
                            summary = viewModel.authTwitterSummary
                        ) { onClickAuthTwitter() }
                    }
                }
                Item(item = item)
            }
            if (viewModel.donated.value.not()) {
                item { Category(titleRes = R.string.pref_category_others) }
                item {
                    val item by remember {
                        derivedStateOf {
                            SettingsViewModel.Item(
                                sharedPreferences,
                                R.string.pref_item_title_donate,
                                R.string.pref_item_desc_donate
                            ) {
                                lifecycleScope.launch {
                                    billingApiClient.startBilling(
                                        this@SettingsActivity,
                                        listOf(BuildConfig.SKU_KEY_DONATE)
                                    )
                                }
                            }
                        }
                    }
                    Item(item = item)
                }
            }
        }
    }

    @Composable
    fun Category(@StringRes titleRes: Int) {
        Text(
            text = stringResource(titleRes),
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = if (isSystemInDarkTheme()) CleanBlue else DeepBlue,
            modifier = Modifier.padding(8.dp)
        )
    }

    @Composable
    fun Item(item: SettingsViewModel.Item) {
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
                item.switchPrefKey?.let { prefKey ->
                    SettingSwitch(this, item.switchState, prefKey, item.summary) {
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
    fun SettingSwitch(
        scope: RowScope,
        switchState: MutableState<Boolean?>,
        prefKey: PrefKey,
        summary: MutableState<String?>,
        onCheckedChanged: ((Boolean) -> Unit)?
    ) {
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

    private data class EasterEggTag(
        val count: Int,
        val time: Long
    )
}