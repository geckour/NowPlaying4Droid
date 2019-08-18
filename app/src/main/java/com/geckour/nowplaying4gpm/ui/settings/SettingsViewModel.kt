package com.geckour.nowplaying4gpm.ui.settings

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.vending.billing.IInAppBillingService
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.geckour.nowplaying4gpm.App
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.MastodonInstancesApiClient
import com.geckour.nowplaying4gpm.api.TwitterApiClient
import com.geckour.nowplaying4gpm.databinding.*
import com.geckour.nowplaying4gpm.domain.model.MastodonUserInfo
import com.geckour.nowplaying4gpm.receiver.ShareWidgetProvider
import com.geckour.nowplaying4gpm.service.NotificationService
import com.geckour.nowplaying4gpm.ui.SingleLiveEvent
import com.geckour.nowplaying4gpm.ui.sharing.SharingActivity
import com.geckour.nowplaying4gpm.ui.widget.adapter.ArtworkResolveMethodListAdapter
import com.geckour.nowplaying4gpm.ui.widget.adapter.FormatPatternModifierListAdapter
import com.geckour.nowplaying4gpm.ui.widget.adapter.PlayerPackageListAdapter
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

class SettingsViewModel : ViewModel() {

    private val twitterApiClient =
        TwitterApiClient(BuildConfig.TWITTER_CONSUMER_KEY, BuildConfig.TWITTER_CONSUMER_SECRET)

    private val mastodonScope = Scope(Scope.Name.ALL)
    internal var mastodonRegistrationInfo: AppRegistration? = null

    private var showingNotificationServicePermissionDialog = false

    internal val requestUpdate = SingleLiveEvent<Unit>()
    internal val reflectDonation = SingleLiveEvent<Boolean>()

    internal fun showErrorDialog(context: Context, titleResId: Int, messageResId: Int, onDismiss: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle(titleResId)
            .setMessage(messageResId)
            .setPositiveButton(R.string.dialog_button_ok) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { onDismiss() }
            .show()
    }

    internal fun onAuthMastodonError(context: Context) {
        showErrorDialog(
            context,
            R.string.dialog_title_alert_failure_auth_mastodon,
            R.string.dialog_message_alert_failure_auth_mastodon
        )
    }

    internal fun onAuthTwitterError(context: Context) {
        showErrorDialog(
            context,
            R.string.dialog_title_alert_failure_auth_twitter,
            R.string.dialog_message_alert_failure_auth_twitter
        )
    }

    internal fun destroyNotification(context: Context) =
        context.sendBroadcast(Intent().apply {
            action = NotificationService.ACTION_DESTROY_NOTIFICATION
        })

    internal fun updateWidget(context: Context, sharedPreferences: SharedPreferences) {
        val trackInfo = sharedPreferences.getCurrentTrackInfo() ?: return

        AppWidgetManager.getInstance(context).apply {
            val ids = getAppWidgetIds(
                ComponentName(
                    context,
                    ShareWidgetProvider::class.java
                )
            )

            launch {
                ids.forEach { id ->
                    val widgetOptions = this@apply.getAppWidgetOptions(id)
                    updateAppWidget(
                        id,
                        getShareWidgetViews(
                            context,
                            ShareWidgetProvider.blockCount(widgetOptions), trackInfo
                        )
                    )
                }
            }
        }
    }

    internal fun requestNotificationListenerPermission(activity: Activity, onGranted: () -> Unit = {}) {
        if (NotificationManagerCompat.getEnabledListenerPackages(activity)
                .contains(activity.packageName).not()
        ) {
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

    internal fun onClickChangeArtworkResolveOrder(context: Context, sharedPreferences: SharedPreferences) {
        val adapter = ArtworkResolveMethodListAdapter(
            sharedPreferences
                .getArtworkResolveOrder()
                .toMutableList()
        )
        val dialogRecyclerViewBinding = DialogRecyclerViewBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            recyclerView.adapter = adapter
            adapter.itemTouchHolder.attachToRecyclerView(recyclerView)
        }

        AlertDialog.Builder(context).generate(
            dialogRecyclerViewBinding.root,
            context.getString(R.string.dialog_title_artwork_resolve_order),
            context.getString(R.string.dialog_message_artwork_resolve_order)
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

    internal fun onClickFormatPatternModifiers(context: Context, sharedPreferences: SharedPreferences) {
        val adapter = FormatPatternModifierListAdapter(
            sharedPreferences
                .getFormatPatternModifiers()
                .toMutableList()
        )
        val formatPatternModifiersDialogBinding = DialogFormatPatternModifiersBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply { recyclerView.adapter = adapter }

        val dialog = AlertDialog.Builder(context).generate(
            formatPatternModifiersDialogBinding.root,
            context.getString(R.string.dialog_title_format_pattern_modifier),
            context.getString(R.string.dialog_message_format_pattern_modifier)
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

    internal fun onClickAuthTwitter(context: Context, rootView: View) {
        launch(Dispatchers.IO) {
            val uri = twitterApiClient.getRequestOAuthUri() ?: run {
                Snackbar.make(rootView, R.string.snackbar_text_failure_auth_twitter, Snackbar.LENGTH_SHORT)
                return@launch
            }

            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(context.getColor(R.color.colorPrimary))
                .build()
                .launchUrl(context, uri)
        }
    }

    internal fun onClickAuthMastodon(context: Context, sharedPreferences: SharedPreferences) {
        val instanceNameInputDialogBinding = DialogAutoCompleteEditTextBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            hint = context.getString(R.string.dialog_hint_mastodon_instance)
            editText.setText(sharedPreferences.getMastodonUserInfo()?.instanceName)
            editText.setSelection(editText.text.length)

            launch {
                val instances = MastodonInstancesApiClient().getList()
                editText.setAdapter(
                    ArrayAdapter<String>(context,
                        android.R.layout.simple_dropdown_item_1line,
                        instances.map { it.name })
                )
            }
        }

        AlertDialog.Builder(context).generate(
            instanceNameInputDialogBinding.root,
            context.getString(R.string.dialog_title_mastodon_instance)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val instance = instanceNameInputDialogBinding.editText.text.toString()
                    if (instance.isNotBlank()) {
                        launch(Dispatchers.IO) {
                            val mastodonApiClient =
                                MastodonClient.Builder(instance, OkHttpClient.Builder().apply {
                                    if (BuildConfig.DEBUG) {
                                        addNetworkInterceptor(
                                            HttpLoggingInterceptor()
                                                .setLevel(HttpLoggingInterceptor.Level.BODY)
                                        )
                                        addNetworkInterceptor(StethoInterceptor())
                                    }
                                }, Gson())
                                    .build()
                            val registrationInfo = try {
                                Apps(mastodonApiClient)
                                    .createApp(
                                        App.MASTODON_CLIENT_NAME,
                                        App.MASTODON_CALLBACK,
                                        mastodonScope,
                                        App.MASTODON_WEB_URL
                                    )
                                    .executeCatching()
                            } catch (e: Mastodon4jRequestException) {
                                Timber.e(e)
                                Crashlytics.logException(e)
                                null
                            } ?: run {
                                onAuthMastodonError(context)
                                return@launch
                            }
                            mastodonRegistrationInfo = registrationInfo

                            val authUrl = Apps(mastodonApiClient)
                                .getOAuthUrl(
                                    registrationInfo.clientId,
                                    mastodonScope, App.MASTODON_CALLBACK
                                )

                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setToolbarColor(context.getColor(R.color.colorPrimary))
                                .build()
                                .launchUrl(context, Uri.parse(authUrl))
                        }
                    }
                }
            }
            dialog.dismiss()
        }.show()
    }

    internal fun onClickDelayMastodon(
        context: Context,
        sharedPreferences: SharedPreferences,
        itemDelayMastodonBinding: ItemPrefItemBinding
    ) {
        val delayTimeInputDialogBinding = DialogEditTextBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            hint = context.getString(R.string.dialog_hint_mastodon_delay)
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            editText.setText(sharedPreferences.getDelayDurationPostMastodon().toString())
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(context).generate(
            delayTimeInputDialogBinding.root,
            context.getString(R.string.dialog_title_mastodon_delay)
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
                        itemDelayMastodonBinding.summary =
                            context.getString(R.string.pref_item_summary_delay_mastodon, duration)
                    } else {
                        showErrorDialog(
                            context,
                            R.string.dialog_title_alert_invalid_duration_value,
                            R.string.dialog_message_alert_invalid_duration_value
                        )
                    }
                }
            }
            dialog.dismiss()
        }.show()
    }

    internal fun onClickVisibilityMastodon(
        context: Context,
        sharedPreferences: SharedPreferences,
        visibilityMastodonBinding: ItemPrefItemBinding
    ) {
        val chooseVisibilityBinding = DataBindingUtil.inflate<DialogSpinnerBinding>(
            LayoutInflater.from(context),
            R.layout.dialog_spinner,
            null,
            false
        ).apply {
            val arrayAdapter =
                object : ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    Visibility.values().map { context.getString(it.getSummaryResId()) }) {
                    override fun getDropDownView(
                        position: Int,
                        convertView: View?, parent: ViewGroup
                    ): View =
                        super.getDropDownView(position, convertView, parent).apply {
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

        AlertDialog.Builder(context).generate(
            chooseVisibilityBinding.root,
            context.getString(R.string.dialog_title_mastodon_visibility),
            context.getString(R.string.dialog_message_mastodon_visibility)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val visibilityIndex = chooseVisibilityBinding.spinner.selectedItemPosition
                    sharedPreferences.edit()
                        .putInt(PrefKey.PREF_KEY_CHOSEN_MASTODON_VISIBILITY.name, visibilityIndex)
                        .apply()
                    visibilityMastodonBinding.summary =
                        context.getString(Visibility.getFromIndex(visibilityIndex).getSummaryResId())
                    onRequestUpdate()
                }
            }
            dialog.dismiss()
        }.show()
    }

    internal fun onClickPlayerPackageMastodon(context: Context, sharedPreferences: SharedPreferences) {
        val adapter = PlayerPackageListAdapter(
            sharedPreferences
                .getPackageStateListPostMastodon()
                .mapNotNull { packageState ->
                    val appName = context.packageManager.let {
                        it.getApplicationLabel(
                            it.getApplicationInfo(
                                packageState.packageName,
                                PackageManager.GET_META_DATA
                            )
                        )
                    }?.toString()
                    if (appName == null) {
                        sharedPreferences.storePackageStatePostMastodon(packageState.packageName, false)
                        null
                    } else PlayerPackageState(packageState.packageName, appName, packageState.state)
                }
        )
        val dialogRecyclerViewBinding = DialogRecyclerViewBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            recyclerView.adapter = adapter
        }

        AlertDialog.Builder(context).generate(
            dialogRecyclerViewBinding.root,
            context.getString(R.string.dialog_title_player_package_mastodon),
            context.getString(R.string.dialog_message_player_package_mastodon)
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

    internal fun onClickFab(context: Context, sharedPreferences: SharedPreferences) {
        if (sharedPreferences.readyForShare(context).not()) {
            showErrorDialog(
                context,
                R.string.dialog_title_alert_no_metadata,
                R.string.dialog_message_alert_no_metadata
            )
            return
        }

        context.startActivity(SharingActivity.getIntent(context))
    }

    internal fun startBillingTransaction(activity: Activity, billingService: IInAppBillingService?, skuName: String) {
        launch {
            billingService?.let {
                BillingApiClient(viewModelScope, it).apply {
                    val sku =
                        getSkuDetails(activity, skuName).firstOrNull()
                            ?: run {
                                showErrorDialog(
                                    activity,
                                    R.string.dialog_title_alert_failure_purchase,
                                    R.string.dialog_message_alert_on_start_purchase
                                )
                                return@launch
                            }

                    if (getPurchasedItems(activity).contains(sku.productId)) {
                        showErrorDialog(
                            activity,
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_already_purchase
                        )
                        reflectDonation.postValue(true)
                        return@launch
                    }
                }

                activity.startIntentSenderForResult(
                    BillingApiClient(viewModelScope, it)
                        .getBuyIntent(activity, skuName)
                        ?.intentSender,
                    SettingsActivity.RequestCode.BILLING.ordinal,
                    Intent(), 0, 0, 0
                )
            }
        }
    }

    internal fun onClickItemPatternFormat(
        context: Context,
        sharedPreferences: SharedPreferences,
        patternFormatBinding: ItemPrefItemBinding
    ) {
        val dialogBinding = DialogEditTextBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            hint = context.getString(R.string.dialog_hint_pattern_format)
            editText.setText(sharedPreferences.getFormatPattern(context))
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(context).generate(
            dialogBinding.root,
            context.getString(R.string.dialog_title_pattern_format),
            context.getString(R.string.dialog_message_pattern_format)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val pattern = dialogBinding.editText.text.toString()
                    sharedPreferences.edit()
                        .putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, pattern)
                        .apply()
                    patternFormatBinding.summary = pattern
                }
            }
            onRequestUpdate()
            dialog.dismiss()
        }.show()
    }

    internal fun onClickItemChooseColor(
        context: Context,
        sharedPreferences: SharedPreferences,
        chooseColorBinding: ItemPrefItemBinding
    ) {
        val dialogBinding = DataBindingUtil.inflate<DialogSpinnerBinding>(
            LayoutInflater.from(context),
            R.layout.dialog_spinner,
            null,
            false
        ).apply {
            val arrayAdapter =
                object : ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    PaletteColor.values().map { context.getString(it.getSummaryResId()) }) {
                    override fun getDropDownView(
                        position: Int,
                        convertView: View?, parent: ViewGroup
                    ): View =
                        super.getDropDownView(position, convertView, parent).apply {
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

        AlertDialog.Builder(context).generate(
            dialogBinding.root,
            context.getString(R.string.dialog_title_choose_color),
            context.getString(R.string.dialog_message_choose_color)
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val paletteIndex = dialogBinding.spinner.selectedItemPosition
                    sharedPreferences.edit()
                        .putInt(PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name, paletteIndex)
                        .apply()
                    chooseColorBinding.summary =
                        context.getString(PaletteColor.getFromIndex(paletteIndex).getSummaryResId())
                    onRequestUpdate()
                }
            }
            dialog.dismiss()
        }.show()
    }

    internal fun onAuthTwitterCallback(
        intent: Intent,
        context: Context,
        sharedPreferences: SharedPreferences,
        rootView: View,
        authTwitterBinding: ItemPrefItemBinding
    ) {
        sharedPreferences.setAlertTwitterAuthFlag(false)

        val verifier = intent.data?.let {
            val queryName = "oauth_verifier"

            if (it.queryParameterNames.contains(queryName))
                it.getQueryParameter(queryName)
            else null
        }

        if (verifier == null) onAuthTwitterError(context)
        else {
            launch(Dispatchers.IO) {
                val accessToken = twitterApiClient.getAccessToken(verifier)

                if (accessToken == null) onAuthTwitterError(context)
                else {
                    sharedPreferences.storeTwitterAccessToken(accessToken)

                    authTwitterBinding.summary = accessToken.screenName
                    Snackbar.make(
                        rootView,
                        R.string.snackbar_text_success_auth_twitter,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    internal fun onAuthMastodonCallback(
        intent: Intent,
        context: Context,
        sharedPreferences: SharedPreferences,
        rootView: View,
        authMastodonBinding: ItemPrefItemBinding
    ) {
        mastodonRegistrationInfo?.apply {
            val token = intent.data?.let {
                val queryName = "code"

                if (it.queryParameterNames.contains(queryName))
                    it.getQueryParameter(queryName)
                else null
            }

            if (token == null) onAuthMastodonError(context)
            else {
                val mastodonApiClientBuilder = MastodonClient.Builder(
                    this@apply.instanceName,
                    OkHttpClient.Builder().apply {
                        if (BuildConfig.DEBUG) {
                            addNetworkInterceptor(
                                HttpLoggingInterceptor()
                                    .setLevel(HttpLoggingInterceptor.Level.BODY)
                            )
                            addNetworkInterceptor(StethoInterceptor())
                        }
                    }, Gson()
                )
                launch(Dispatchers.IO) {
                    val accessToken = try {
                        Apps(mastodonApiClientBuilder.build())
                            .getAccessToken(
                                this@apply.clientId,
                                this@apply.clientSecret,
                                App.MASTODON_CALLBACK,
                                token
                            )
                            .executeCatching()
                    } catch (e: Mastodon4jRequestException) {
                        Timber.e(e)
                        Crashlytics.logException(e)
                        null
                    }

                    if (accessToken == null) onAuthMastodonError(context)
                    else {
                        val userName = Accounts(
                            mastodonApiClientBuilder
                                .accessToken(accessToken.accessToken)
                                .build()
                        )
                            .getVerifyCredentials()
                            .executeCatching()
                            ?.userName ?: run {
                            onAuthMastodonError(context)
                            return@launch
                        }
                        val userInfo =
                            MastodonUserInfo(accessToken, this@apply.instanceName, userName)
                        sharedPreferences.storeMastodonUserInfo(userInfo)

                        authMastodonBinding.summary =
                            context.getString(
                                R.string.pref_item_summary_auth_mastodon,
                                userInfo.userName, userInfo.instanceName
                            )
                        Snackbar.make(
                            rootView,
                            R.string.snackbar_text_success_auth_mastodon,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun onRequestUpdate() {
        if (showingNotificationServicePermissionDialog.not()) requestUpdate.call()
    }
}