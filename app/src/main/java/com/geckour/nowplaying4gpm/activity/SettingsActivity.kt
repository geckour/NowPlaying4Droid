package com.geckour.nowplaying4gpm.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.databinding.ActivitySettingsBinding
import com.geckour.nowplaying4gpm.databinding.DialogEditTextBinding
import com.geckour.nowplaying4gpm.databinding.DialogSpinnerBinding
import com.geckour.nowplaying4gpm.service.NotifyMediaMetaDataService
import com.geckour.nowplaying4gpm.util.generate
import com.geckour.nowplaying4gpm.util.getChoseColorIndexFromPreference
import com.geckour.nowplaying4gpm.util.getFormatPatternFromPreference

class SettingsActivity : Activity() {

    enum class PrefKey {
        PREF_KEY_PATTERN_FORMAT_SHARE_TEXT,
        PREF_KEY_CHOSE_COLOR_INDEX
    }

    enum class PermissionRequestCode {
        READ_EXTERNAL_STORAGE
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        binding.toolbar.title = "設定 - ${getString(R.string.app_name)}"
        binding.summaryPattern = sharedPreferences.getFormatPatternFromPreference(this)
        binding.summaryChooseColor = getString(paletteArray[sharedPreferences.getChoseColorIndexFromPreference()])
        binding.itemPatternFormat?.root?.setOnClickListener { onClickItemPatternFormat() }
        binding.itemChooseColor?.root?.setOnClickListener {
            onClickItemChooseColor()
        }

        requestStoragePermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionRequestCode.READ_EXTERNAL_STORAGE.ordinal -> {
                if (grantResults?.all { it == PackageManager.PERMISSION_GRANTED } == true) {
                    startNotificationService()
                } else requestStoragePermission()
            }
        }
    }

    private fun startNotificationService() =
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(NotifyMediaMetaDataService.getIntent(this))
            else startService(NotifyMediaMetaDataService.getIntent(this))

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startNotificationService()
        } else requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PermissionRequestCode.READ_EXTERNAL_STORAGE.ordinal)
    }

    private fun onClickItemPatternFormat() {
        val patternFormatDialogBinding = DataBindingUtil.inflate<DialogEditTextBinding>(
                LayoutInflater.from(this),
                R.layout.dialog_edit_text,
                null,
                false
        ).apply {
            hint = getString(R.string.dialog_hint_pattern_format)
            editText.setText(sharedPreferences.getFormatPatternFromPreference(this@SettingsActivity))
            editText.setSelection(editText.text.length)
        }

        AlertDialog.Builder(this).generate(
                getString(R.string.dialog_title_pattern_format),
                getString(R.string.dialog_message_pattern_format),
                patternFormatDialogBinding.root) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val pattern = patternFormatDialogBinding.editText.text.toString()
                    sharedPreferences.edit()
                            .putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, pattern)
                            .apply()
                    binding.summaryPattern = pattern
                }
            }
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
                    object: ArrayAdapter<String>(
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
                setSelection(sharedPreferences.getChoseColorIndexFromPreference())
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
                            .putInt(PrefKey.PREF_KEY_CHOSE_COLOR_INDEX.name, paletteIndex)
                            .apply()
                    binding.summaryChooseColor = getString(paletteArray[paletteIndex])
                }
            }
            dialog.dismiss()
        }.show()
    }
}