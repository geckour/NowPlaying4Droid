package com.geckour.nowplaying4gpm.wear

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import com.geckour.nowplaying4gpm.wear.databinding.ActivityMainBinding
import com.geckour.nowplaying4gpm.wear.domain.model.TrackInfo
import com.google.android.gms.wearable.*
import com.google.gson.Gson

class MainActivity : WearableActivity(), DataClient.OnDataChangedListener {

    companion object {
        private const val PATH = "/track_info"
        private const val KEY_TRACK_INFO = "key_track_info"
    }

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.info = TrackInfo.empty
    }

    override fun onResume() {
        super.onResume()

        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()

        Wearable.getDataClient(this).removeListener(this)
    }

    private fun onUpdateTrackInfo(trackInfo: TrackInfo?) {
        binding.info = trackInfo
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach {
            when (it.type) {
                DataEvent.TYPE_CHANGED -> {
                    it.dataItem.apply {
                        if (uri.path.compareTo(PATH) == 0) {
                            val trackInfo = Gson().fromJson(
                                    DataMapItem.fromDataItem(this)
                                            .dataMap
                                            .getString(KEY_TRACK_INFO),
                                    TrackInfo::class.java)

                            onUpdateTrackInfo(trackInfo)
                        }
                    }
                }
                DataEvent.TYPE_DELETED -> onUpdateTrackInfo(null)
            }
        }
    }
}
