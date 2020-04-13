package com.geckour.nowplaying4gpm.ui.debug

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.databinding.ActivityDebugBinding
import com.geckour.nowplaying4gpm.ui.WithCrashlyticsActivity
import com.geckour.nowplaying4gpm.util.getDebugSpotifySearchFlag
import com.geckour.nowplaying4gpm.util.toggleDebugSpotifySearchFlag

class DebugActivity : WithCrashlyticsActivity() {

    companion object {

        fun getIntent(context: Context): Intent = Intent(context, DebugActivity::class.java)
    }

    private lateinit var binding: ActivityDebugBinding
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_debug)
        binding.toolbarTitle =
            "${getString(R.string.activity_title_debug)} - ${getString(R.string.app_name)}"
        binding.recyclerView.adapter = DebugMenuListAdapter { debugMenu, summaryView ->
            when (debugMenu) {
                DebugMenuListAdapter.DebugMenu.TOGGLE_SPOTIFY_SEARCH_DEBUG -> {
                    val flag = sharedPreferences.toggleDebugSpotifySearchFlag()
                    summaryView.text = flag.toString()
                }
            }
        }.apply {
            submitList(
                DebugMenuListAdapter.DebugMenu.values()
                    .map {
                        DebugMenuListAdapter.DebugMenuItem(
                            it,
                            when (it) {
                                DebugMenuListAdapter.DebugMenu.TOGGLE_SPOTIFY_SEARCH_DEBUG -> {
                                    sharedPreferences.getDebugSpotifySearchFlag().toString()
                                }
                                else -> null
                            }
                        )
                    }
            )
        }
    }
}