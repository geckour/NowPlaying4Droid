package com.geckour.nowplaying4gpm.wear.domain.model

import android.graphics.Bitmap

data class TrackInfo(
    val subject: String?,
    val artwork: Bitmap?
) {
    companion object {
        val empty = TrackInfo(null, null)
    }
}