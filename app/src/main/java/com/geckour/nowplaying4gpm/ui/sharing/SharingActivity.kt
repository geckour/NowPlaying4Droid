package com.geckour.nowplaying4gpm.ui.sharing

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.util.getCurrentTrackInfo

class SharingActivity : AppCompatActivity() {

    enum class IntentRequestCode {
        SHARE
    }

    companion object {

        fun getIntent(context: Context): Intent =
            Intent(context, SharingActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    private val viewModel: SharingViewModel by viewModels()

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val trackInfo = sharedPreferences.getCurrentTrackInfo()
        viewModel.startShare(this, sharedPreferences, trackInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
        finish()
    }
}