package com.geckour.nowplaying4gpm.wear.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.material.Text
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
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.wear.domain.model.SharingInfo
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    companion object {
        internal const val PATH_POST_SHARING_INFO = "/post/sharing_info"
        private const val PATH_GET_SHARING_INFO = "/get/sharing_info"
        private const val PATH_POST_TWITTER = "/post/twitter"
        private const val PATH_POST_SUCCESS = "/post/success"
        private const val PATH_POST_FAILURE = "/post/failure"
        private const val PATH_SHARE_DELEGATE = "/share/delegate"
        private const val PATH_SHARE_SUCCESS = "/share/success"
        private const val PATH_SHARE_FAILURE = "/share/failure"
        internal const val KEY_SUBJECT = "key_subject"
        internal const val KEY_ARTWORK = "key_artwork"
        internal const val ARG_AUTO_SHARE = "arg_auto_share"
    }

    private val onDataChanged: (DataEventBuffer) -> Unit = { buffer ->
        buffer.forEach { event ->
            when (event.type) {
                DataEvent.TYPE_CHANGED -> {
                    if (event.dataItem.uri.path?.compareTo(PATH_POST_SHARING_INFO) == 0) {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                        val subject = dataMap.getString(KEY_SUBJECT)
                        updateSharingInfo(SharingInfo(subject, null))

                        if (dataMap.containsKey(KEY_ARTWORK)) {
                            dataMap.getAsset(KEY_ARTWORK)?.loadByteArray(this) {
                                updateSharingInfo(
                                    SharingInfo(
                                        subject,
                                        it
                                    )
                                )
                            }
                        }
                    }
                }

                DataEvent.TYPE_DELETED -> updateSharingInfo(SharingInfo.empty)
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

    private val sharingInfo = mutableStateOf(SharingInfo.empty)
    private val indicatorDrawableResource = mutableStateOf<Int?>(null)
    private val indicatorDrawableTint = mutableStateOf(R.color.colorPrimaryDark)

    private lateinit var artworkGestureDetector: GestureDetectorCompat

    private val autoShare = MutableStateFlow(false)

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

        Wearable.getDataClient(this).addListener(onDataChanged)
        Wearable.getMessageClient(this).addListener(onMessageReceived)

        setContent {
            val info by remember { sharingInfo }
            val indicatorResource by remember { indicatorDrawableResource }
            val indicatorTint by remember { indicatorDrawableTint }

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(
                    modifier = Modifier.fillMaxSize(),
                    targetState = info.artwork,
                    animationSpec = tween(300)
                ) {
                    it?.let {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInteropFilter { event ->
                                    artworkGestureDetector.onTouchEvent(event)
                                },
                            bitmap = BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Inside
                        )
                    }
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
                    visible = indicatorResource != null,
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

        if (intent.getBooleanExtra(ARG_AUTO_SHARE, false)) {
            invokeShare()
        }
    }

    override fun onResume() {
        super.onResume()

        requestSharingInfo()
    }

    override fun onDestroy() {
        Wearable.getDataClient(this).removeListener(onDataChanged)
        Wearable.getMessageClient(this).removeListener(onMessageReceived)

        super.onDestroy()
    }

    private fun updateSharingInfo(sharingInfo: SharingInfo) {
        this.sharingInfo.value = sharingInfo
    }

    private fun requestSharingInfo() {
        Wearable.getNodeClient(this@MainActivity)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                nodes?.filter { it.isNearby }
                    ?.forEach {
                        Wearable.getMessageClient(this@MainActivity)
                            .sendMessage(it.id, PATH_GET_SHARING_INFO, null)
                    }
            }
    }

    private fun invokeShare() {
        Wearable.getNodeClient(this)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                nodes?.filter { it.isNearby }
                    ?.forEach {
                        Wearable.getMessageClient(this)
                            .sendMessage(it.id, PATH_POST_TWITTER, null)
                    }
            }
    }

    private fun invokeShareOnHost(): Boolean {
        Wearable.getNodeClient(this@MainActivity)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                nodes?.filter { it.isNearby }
                    ?.forEach {
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

internal fun Asset.loadByteArray(context: Context, onSuccess: (byteArray: ByteArray) -> Unit) =
    Wearable.getDataClient(context)
        .getFdForAsset(this@loadByteArray)
        .addOnSuccessListener { result ->
            onSuccess(result.inputStream.readBytes())
        }