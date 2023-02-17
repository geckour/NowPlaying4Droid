package com.geckour.nowplaying4droid.app.ui.sharing

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4droid.app.api.LastFmApiClient
import com.geckour.nowplaying4droid.app.api.SpotifyApiClient
import com.geckour.nowplaying4droid.app.api.YouTubeDataClient
import com.geckour.nowplaying4droid.app.util.forceUpdateTrackDetailIfNeeded
import com.geckour.nowplaying4droid.app.util.getCurrentTrackDetail
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SharingActivity : AppCompatActivity(), KoinComponent {

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
    private val spotifyApiClient: SpotifyApiClient by inject()
    private val lastFmApiClient: LastFmApiClient by inject()
    private val youTubeDataClient: YouTubeDataClient by inject()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        lifecycleScope.launch {
            val trackDetail = sharedPreferences.getCurrentTrackDetail()
                ?: forceUpdateTrackDetailIfNeeded(
                    this@SharingActivity,
                    sharedPreferences,
                    spotifyApiClient,
                    youTubeDataClient,
                    lastFmApiClient
                )
            viewModel.startShare(this@SharingActivity, sharedPreferences, trackDetail)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntent(intent)
        finish()
    }
}