package com.geckour.nowplaying4gpm.wear

import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import com.geckour.nowplaying4gpm.wear.domain.model.SharingInfo
import com.geckour.nowplaying4gpm.wear.ui.MainActivity
import com.geckour.nowplaying4gpm.wear.ui.loadByteArray
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
import com.google.android.horologist.tiles.SuspendingTileService

@OptIn(ExperimentalHorologistTilesApi::class)
class NP4DTileService : SuspendingTileService() {

    private var sharingInfo = SharingInfo.empty

    private lateinit var renderer: NP4DTileRenderer

    override fun onCreate() {
        super.onCreate()

        Wearable.getDataClient(this).addListener(onDataChanged)

        renderer = NP4DTileRenderer(this)
    }

    override fun onDestroy() {
        Wearable.getDataClient(this).removeListener(onDataChanged)

        super.onDestroy()
    }

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources =
        renderer.produceRequestedResources(sharingInfo.artwork, requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile =
        renderer.renderTimeline(sharingInfo, requestParams)


    private val onDataChanged: (DataEventBuffer) -> Unit = { buffer ->
        buffer.forEach { event ->
            when (event.type) {
                DataEvent.TYPE_CHANGED -> {
                    if (event.dataItem.uri.path?.compareTo(MainActivity.PATH_POST_SHARING_INFO) == 0) {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                        val subject = dataMap.getString(MainActivity.KEY_SUBJECT)
                        sharingInfo = SharingInfo(subject, null)

                        if (dataMap.containsKey(MainActivity.KEY_ARTWORK)) {
                            dataMap.getAsset(MainActivity.KEY_ARTWORK)?.loadByteArray(this) {
                                sharingInfo = SharingInfo(
                                    subject,
                                    it
                                )
                            }
                        }
                    }
                }

                DataEvent.TYPE_DELETED -> sharingInfo = SharingInfo.empty
            }
        }
    }
}