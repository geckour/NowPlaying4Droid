package com.geckour.nowplaying4gpm.ui

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.databinding.ActivityMainBinding
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.experimental.async
import timber.log.Timber

class MainActivity : WearableActivity() {

    companion object {
        private const val PATH = "/track_info"
        private const val KEY_SUBJECT = "key_subject"
        private const val KEY_ARTWORK = "key_artwork"
    }

    private lateinit var binding: ActivityMainBinding
    private val onDataChanged: (DataEventBuffer) -> Unit = {
        it.forEach {
            when (it.type) {
                DataEvent.TYPE_CHANGED -> {
                    it.dataItem.apply {
                        if (uri.path.compareTo(PATH) == 0) {
                            DataMapItem.fromDataItem(this@apply).dataMap.let {
                                async {
                                    val trackInfo =
                                            TrackInfo(
                                                    it.getString(KEY_SUBJECT),
                                                    if (it.containsKey(KEY_ARTWORK)) it.getAsset(KEY_ARTWORK).loadBitmap()
                                                    else null
                                            ).apply { Timber.d("track info: $this") }

                                    onUpdateTrackInfo(trackInfo)
                                }
                            }
                        }
                    }
                }
                DataEvent.TYPE_DELETED -> onUpdateTrackInfo(null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.info = TrackInfo.empty
    }

    override fun onResume() {
        super.onResume()

        Wearable.getDataClient(this).addListener(onDataChanged)
    }

    override fun onPause() {
        super.onPause()

        Wearable.getDataClient(this).removeListener(onDataChanged)
    }

    private suspend fun Asset.loadBitmap(): Bitmap? =
            async {
                Tasks.await(Wearable.getDataClient(this@MainActivity)
                        .getFdForAsset(this@loadBitmap)
                ).inputStream?.let {
                    BitmapFactory.decodeStream(it)
                }
            }.await()

    private fun onUpdateTrackInfo(trackInfo: TrackInfo?) {
        binding.info = trackInfo
    }
}
