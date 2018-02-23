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
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.databinding.ActivitySettingsBinding
import com.geckour.nowplaying4gpm.databinding.DialogEditTextBinding
import com.geckour.nowplaying4gpm.service.NotifyMediaMetaDataService
import com.geckour.nowplaying4gpm.util.generate

class SettingsActivity : Activity() {

    enum class PrefKey {
        PREF_KEY_PATTERN_FORMAT_SHARE_TEXT
    }

    enum class PermissionRequestCode {
        READ_EXTERNAL_STORAGE
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, SettingsActivity::class.java)
    }

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        binding.toolbar.title = "設定 - ${getString(R.string.app_name)}"
        binding.summaryPattern = sharedPreferences.getString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, getString(R.string.default_sharing_text_pattern))
        binding.itemPatternFormat?.root?.setOnClickListener { onClickItemPatternFormat() }

        requestStoragePermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionRequestCode.READ_EXTERNAL_STORAGE.ordinal -> {
                if (grantResults?.all { it == PackageManager.PERMISSION_GRANTED } == true) {
                    startService(NotifyMediaMetaDataService.getIntent(this))
                } else requestStoragePermission()
            }
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startService(NotifyMediaMetaDataService.getIntent(this))
        } else requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PermissionRequestCode.READ_EXTERNAL_STORAGE.ordinal)
    }

    private fun onClickItemPatternFormat() {
        val patternFormatDialogBinding = DataBindingUtil.inflate<DialogEditTextBinding>(
                LayoutInflater.from(this),
                R.layout.dialog_edit_text, null,
                false
        ).apply {
            hint = getString(R.string.dialog_hint_pattern_format)
            editText.setText(
                    sharedPreferences.getString(
                            PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                            getString(R.string.default_sharing_text_pattern)
                    )
            )
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
}