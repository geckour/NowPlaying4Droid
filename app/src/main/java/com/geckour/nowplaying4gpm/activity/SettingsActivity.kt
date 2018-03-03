package com.geckour.nowplaying4gpm.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import com.android.vending.billing.IInAppBillingService
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.BillingApiClient
import com.geckour.nowplaying4gpm.api.model.PurchaseResult
import com.geckour.nowplaying4gpm.databinding.ActivitySettingsBinding
import com.geckour.nowplaying4gpm.databinding.DialogEditTextBinding
import com.geckour.nowplaying4gpm.databinding.DialogSpinnerBinding
import com.geckour.nowplaying4gpm.service.NotifyMediaMetaDataService
import com.geckour.nowplaying4gpm.util.*
import com.google.gson.Gson
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class SettingsActivity : Activity() {

    enum class PrefKey {
        PREF_KEY_PATTERN_FORMAT_SHARE_TEXT,
        PREF_KEY_CHOSEN_COLOR_INDEX,
        PREF_KEY_WHETHER_RESIDE,
        PREF_KEY_WHETHER_USE_API,
        PREF_KEY_WHETHER_BUNDLE_ARTWORK,
        PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG,
        PREF_KEY_CURRENT_TITLE,
        PREF_KEY_CURRENT_ARTIST,
        PREF_KEY_CURRENT_ALBUM,
        PREF_KEY_TEMP_ALBUM_ART_URI,
        PREF_KEY_BILLING_DONATE
    }

    enum class PermissionRequestCode {
        EXTERNAL_STORAGE
    }

    enum class IntentSenderRequestCode {
        BILLING
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, SettingsActivity::class.java)

        val paletteArray: Array<Int> = arrayOf(
                R.string.palette_light_vibrant,
                R.string.palette_vibrant,
                R.string.palette_dark_vibrant,
                R.string.palette_light_muted,
                R.string.palette_muted,
                R.string.palette_dark_muted
        )
    }

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }
    private lateinit var binding: ActivitySettingsBinding
    private val jobs: ArrayList<Job> = ArrayList()
    private lateinit var serviceConnection: ServiceConnection
    private var billingService: IInAppBillingService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val donationState = sharedPreferences.getDonateBillingState()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        binding.toolbar.title = "設定 - ${getString(R.string.app_name)}"

        binding.summaryPattern = sharedPreferences.getFormatPattern(this)
        binding.summaryChooseColor = getString(paletteArray[sharedPreferences.getChoseColorIndex()])
        binding.summarySwitchReside = getString(sharedPreferences.getWhetherResideSummaryResId())
        binding.summarySwitchUseApi = getString(sharedPreferences.getWhetherUseApiSummaryResId())
        binding.summarySwitchBundleArtwork = getString(sharedPreferences.getWhetherBundleArtworkSummaryResId())
        binding.summarySwitchColorizeNotificationBg = getString(sharedPreferences.getWhetherColorizeNotificationBgSummaryResId())

        binding.fab.setOnClickListener { onClickFab() }

        binding.scrollView.apply {
            setOnScrollChangeListener { _, _, y, _, oldY ->
                if (y > oldY && getChildAt(0).measuredHeight <= measuredHeight + y) binding.fab.hide()
                if (y < oldY && binding.fab.isShown.not()) binding.fab.show()
            }
        }

        binding.itemSwitchUseApi?.apply {
            maskInactive.visibility = if (donationState) View.GONE else View.VISIBLE
            root.setOnClickListener { onClickItemWithSwitch(extra) }
            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_USE_API) { _, summary ->
                    binding.summarySwitchUseApi = summary
                    updateNotification()
                })
            }
        }

        binding.itemPatternFormat?.root?.setOnClickListener { onClickItemPatternFormat() }

        binding.itemChooseColor?.root?.setOnClickListener { onClickItemChooseColor() }

        binding.itemSwitchReside?.apply {
            root.setOnClickListener { onClickItemWithSwitch(extra) }
            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_RESIDE) { checkState, summary ->
                    binding.summarySwitchReside = summary

                    if (checkState) updateNotification()
                    else destroyNotification()
                })
            }
        }

        binding.itemSwitchBundleArtwork?.apply {
            root.setOnClickListener { onClickItemWithSwitch(extra) }
            extra.apply {
                visibility = View.VISIBLE
                addView(getSwitch(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK) { _, summary -> binding.summarySwitchBundleArtwork = summary })
            }
        }

        binding.itemSwitchColorizeNotificationBg?.apply {
            if (Build.VERSION.SDK_INT < 26) root.visibility = View.GONE
            else {
                root.setOnClickListener { onClickItemWithSwitch(extra) }
                extra.apply {
                    visibility = View.VISIBLE
                    addView(getSwitch(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG) { _, summary -> binding.summarySwitchColorizeNotificationBg = summary })
                }
            }
        }

        binding.itemDonate?.apply {
            if (donationState) root.visibility = View.GONE
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

        requestStoragePermission { startNotificationService() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionRequestCode.EXTERNAL_STORAGE.ordinal -> {
                if (grantResults?.all { it == PackageManager.PERMISSION_GRANTED } == true) {
                    startNotificationService()
                } else requestStoragePermission { startNotificationService() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        billingService?.apply { unbindService(serviceConnection) }
        jobs.cancelAll()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            IntentSenderRequestCode.BILLING.ordinal -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        data?.getStringExtra(BillingApiClient.BUNDLE_KEY_PURCHASE_DATA)?.apply {
                            var success = false
                            try {
                                val purchaseResult = Gson().fromJson(this, PurchaseResult::class.java)
                                if (purchaseResult.purchaseState == 0) {
                                    success = true
                                    sharedPreferences.edit().putBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, true).apply()
                                    binding.itemSwitchUseApi?.maskInactive?.visibility = View.GONE
                                }
                            } catch (e: Exception) {
                                Timber.e(e)
                            }

                            if (success.not())
                                showErrorDialog(
                                        R.string.dialog_title_alert_failure_purchase,
                                        R.string.dialog_message_alert_failure_purchase
                                )
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                    }
                }
            }
        }
    }

    private fun startNotificationService() =
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(NotifyMediaMetaDataService.getIntent(this))
            else startService(NotifyMediaMetaDataService.getIntent(this))

    private fun updateNotification() =
            requestStoragePermission {
                sendBroadcast(Intent().apply { action = NotifyMediaMetaDataService.ACTION_SHOW_NOTIFICATION })
            }

    private fun destroyNotification() =
            sendBroadcast(Intent().apply { action = NotifyMediaMetaDataService.ACTION_DESTROY_NOTIFICATION })

    private fun requestStoragePermission(onPermit: () -> Unit = {}) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            onPermit()
        } else requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), PermissionRequestCode.EXTERNAL_STORAGE.ordinal)
    }

    private suspend fun startBillingTransaction(skuName: String) =
            billingService?.let {
                BillingApiClient(it).apply {
                    val sku = getSkuDetails(this@SettingsActivity, skuName).firstOrNull() ?: run {
                        showErrorDialog(R.string.dialog_title_alert_failure_purchase, R.string.dialog_message_alert_on_start_purchase)
                        return@let
                    }
                    if (getPurchasedItems(this@SettingsActivity).contains(sku.productId)) {
                        showErrorDialog(R.string.dialog_title_alert_failure_purchase, R.string.dialog_message_alert_already_purchase)
                        return@let
                    }
                }
                startIntentSenderForResult(
                        BillingApiClient(it)
                                .getBuyIntent(this@SettingsActivity, skuName)
                                ?.intentSender,
                        IntentSenderRequestCode.BILLING.ordinal,
                        Intent(), 0, 0, 0
                )
            }

    private fun onClickFab() {
        val title =
                if (sharedPreferences.contains(PrefKey.PREF_KEY_CURRENT_TITLE.name)) sharedPreferences.getString(PrefKey.PREF_KEY_CURRENT_TITLE.name, null)
                else null
        val artist =
                if (sharedPreferences.contains(PrefKey.PREF_KEY_CURRENT_ARTIST.name)) sharedPreferences.getString(PrefKey.PREF_KEY_CURRENT_ARTIST.name, null)
                else null
        val album =
                if (sharedPreferences.contains(PrefKey.PREF_KEY_CURRENT_ALBUM.name)) sharedPreferences.getString(PrefKey.PREF_KEY_CURRENT_ALBUM.name, null)
                else null

        if (title == null || artist == null || album == null) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_alert_no_for_share)
                    .setMessage(R.string.dialog_message_alert_no_for_share)
                    .setPositiveButton(R.string.dialog_button_ok) { dialog, _ -> dialog.dismiss() }
                    .show()
        } else {
            val intent = SharingActivity.getIntent(this@SettingsActivity, title, artist, album)
            startActivity(intent)
        }
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
                            paletteArray.map { getString(it) }) {
                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
                            return super.getDropDownView(position, convertView, parent).apply {
                                if (position == spinner.selectedItemPosition) (this as TextView).setTextColor(getColor(R.color.colorPrimaryDark))
                            }
                        }
                    }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinner.apply {
                adapter = arrayAdapter
                setSelection(sharedPreferences.getChoseColorIndex())
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
                            .putInt(PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name, paletteIndex)
                            .apply()
                    binding.summaryChooseColor = getString(paletteArray[paletteIndex])
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

    private fun showErrorDialog(titleResId: Int, messageResId: Int) =
            AlertDialog.Builder(this)
                    .setTitle(titleResId)
                    .setMessage(messageResId)
                    .setPositiveButton(R.string.dialog_button_ok) { dialog, _ -> dialog.dismiss() }
                    .show()
}