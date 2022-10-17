package com.geckour.nowplaying4gpm.wear.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Text
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.wear.domain.model.TrackInfo
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        private const val PATH_TRACK_INFO_POST = "/track_info/post"
        private const val PATH_TRACK_INFO_GET = "/track_info/get"
        private const val PATH_POST_TWITTER = "/post/twitter"
        private const val PATH_POST_SUCCESS = "/post/success"
        private const val PATH_POST_FAILURE = "/post/failure"
        private const val PATH_SHARE_DELEGATE = "/share/delegate"
        private const val PATH_SHARE_SUCCESS = "/share/success"
        private const val PATH_SHARE_FAILURE = "/share/failure"
        private const val KEY_SUBJECT = "key_subject"
        private const val KEY_ARTWORK = "key_artwork"
    }

    private val onDataChanged: (DataEventBuffer) -> Unit = { buffer ->
        buffer.forEach { event ->
            when (event.type) {
                DataEvent.TYPE_CHANGED -> {
                    if (event.dataItem.uri.path?.compareTo(PATH_TRACK_INFO_POST) == 0) {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                        val subject = dataMap.getString(KEY_SUBJECT)
                        updateTrackInfo(TrackInfo(subject, null))

                        if (dataMap.containsKey(KEY_ARTWORK)) {
                            dataMap.getAsset(KEY_ARTWORK)?.loadBitmap {
                                updateTrackInfo(TrackInfo(subject, it))
                            }
                        }
                    }
                }

                DataEvent.TYPE_DELETED -> updateTrackInfo(TrackInfo.empty)
            }
        }
    }

    private val onMessageReceived: (MessageEvent) -> Unit = {
        lifecycleScope.launchWhenResumed {
            when (it.path) {
                PATH_POST_SUCCESS -> onPostToTwitterSuccess()

                PATH_POST_FAILURE -> onFailure()

                PATH_SHARE_SUCCESS -> onDelegateSuccess()

                PATH_SHARE_FAILURE -> onFailure()
            }
        }
    }

    private val trackInfo = mutableStateOf(TrackInfo.empty)
    private val indicatorDrawableResource = mutableStateOf<Int?>(null)
    private val indicatorDrawableTint = mutableStateOf(R.color.colorPrimaryDark)

    private lateinit var artworkGestureDetector: GestureDetectorCompat

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        artworkGestureDetector = GestureDetectorCompat(
            this,
            object : GestureDetector.OnGestureListener {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onShowPress(event: MotionEvent) = Unit

                override fun onSingleTapUp(event: MotionEvent): Boolean = true

                override fun onScroll(
                    event1: MotionEvent,
                    event2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean = true

                override fun onLongPress(event: MotionEvent) {
                    invokeShareOnHost()
                }

                override fun onFling(
                    event1: MotionEvent,
                    event2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean = true
            }
        )

        setContent {
            val info by remember { trackInfo }
            val indicatorResource by remember { indicatorDrawableResource }
            val indicatorTint by remember { indicatorDrawableTint }
            Box(modifier = Modifier.fillMaxSize()) {
                info.artwork?.let {
                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInteropFilter { event ->
                                artworkGestureDetector.onTouchEvent(event)
                            },
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Inside
                    )
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colorResource(id = R.color.colorMaskArtwork))
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .align(Alignment.Center),
                    text = info.subject?.foldBreaks()
                        ?: stringResource(id = R.string.subject_placeholder),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                info.subject?.let {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.5f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { invokeShare() }
                            ) {
                                Image(
                                    modifier = Modifier.size(36.dp),
                                    painter = painterResource(id = R.drawable.ic_twitter),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = indicatorDrawableResource.value != null,
                    enter = fadeIn(animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = colorResource(id = R.color.colorBackground)),
                        contentAlignment = Alignment.Center
                    ) {
                        indicatorResource?.let {
                            Image(
                                painter = painterResource(id = it),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorResource(id = indicatorTint))
                            )
                        }
                    }
                }
            }
        }

        Wearable.getDataClient(this).addListener(onDataChanged)
        Wearable.getMessageClient(this).addListener(onMessageReceived)
    }

    override fun onResume() {
        super.onResume()

        requestTrackInfo()
    }

    override fun onDestroy() {
        Wearable.getDataClient(this).removeListener(onDataChanged)
        Wearable.getMessageClient(this).removeListener(onMessageReceived)

        super.onDestroy()
    }

    private fun Asset.loadBitmap(onComplete: (bitmap: Bitmap) -> Unit) =
        Wearable.getDataClient(this@MainActivity)
            .getFdForAsset(this@loadBitmap)
            .addOnCompleteListener { task ->
                onComplete(BitmapFactory.decodeStream(task.result.inputStream))
            }

    private fun updateTrackInfo(trackInfo: TrackInfo) {
        this.trackInfo.value = trackInfo
    }

    private fun requestTrackInfo() {
        Wearable.getNodeClient(this@MainActivity).connectedNodes.addOnCompleteListener { task ->
            task.result
                ?.filter { it.isNearby }
                ?.forEach {
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(it.id, PATH_TRACK_INFO_GET, null)
                }
        }
    }

    private fun invokeShare() {
        Wearable.getNodeClient(this@MainActivity).connectedNodes.addOnCompleteListener { task ->
            task.result?.filter { it.isNearby }?.forEach {
                Wearable.getMessageClient(this@MainActivity)
                    .sendMessage(it.id, PATH_POST_TWITTER, null)
            }
        }
    }

    private fun invokeShareOnHost(): Boolean {
        Wearable.getNodeClient(this@MainActivity).connectedNodes.addOnCompleteListener { task ->
            task.result?.filter { it.isNearby }?.forEach {
                Wearable.getMessageClient(this@MainActivity)
                    .sendMessage(it.id, PATH_SHARE_DELEGATE, null)
            }
        }

        return true
    }

    private suspend fun onPostToTwitterSuccess() {
        indicatorDrawableTint.value = R.color.colorPrimaryDark
        indicatorDrawableResource.value = R.drawable.ic_baseline_send_24px
        delay(650)
        indicatorDrawableResource.value = null
    }

    private suspend fun onFailure() {
        indicatorDrawableTint.value = R.color.colorAccent
        indicatorDrawableResource.value = R.drawable.ic_baseline_error_24px
        delay(650)
        indicatorDrawableResource.value = null
    }

    private suspend fun onDelegateSuccess() {
        indicatorDrawableTint.value = R.color.colorPrimaryDark
        indicatorDrawableResource.value = R.drawable.ic_baseline_mobile_screen_share_24px
        delay(650)
        indicatorDrawableResource.value = null
    }

    private fun String.foldBreaks(): String = this.replace(Regex("[\r\n]"), " ")
}