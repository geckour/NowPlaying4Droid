package com.geckour.nowplaying4gpm.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
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

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var patternFormatDialogBinding: DialogEditTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences.edit()
                .putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, getString(R.string.default_sharing_text_pattern))
                .apply()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        patternFormatDialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_edit_text, null, false)

        binding.toolbar.title = "設定 - ${getString(R.string.app_name)}"
        binding.summaryPattern = sharedPreferences.getString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, getString(R.string.default_sharing_text_pattern))
        binding.itemPatternFormat?.root?.setOnClickListener { onClickItemPatternFormat() }

        requestPermissionForMediaControl()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            0 -> {
                if (grantResults?.all { it == PackageManager.PERMISSION_GRANTED} != false) {
                    startService(NotifyMediaMetaDataService.getIntent(this))
                    finish()
                } else requestPermissionForMediaControl()
            }
        }
    }

    private fun requestPermissionForMediaControl() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MEDIA_CONTENT_CONTROL) == PackageManager.PERMISSION_GRANTED) {
            startService(NotifyMediaMetaDataService.getIntent(this))
        } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.MEDIA_CONTENT_CONTROL), 0)
    }

    private fun onClickItemPatternFormat() {
        AlertDialog.Builder(this).generate(
                getString(R.string.dialog_title_pattern_format),
                getString(R.string.dialog_message_pattern_format),
                patternFormatDialogBinding.root) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    sharedPreferences.edit()
                            .putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, patternFormatDialogBinding.editText.text.toString())
                            .apply()
                }
            }
            dialog.dismiss()
        }
    }
}