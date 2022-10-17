package com.geckour.nowplaying4gpm.wear.domain.model

import android.graphics.Bitmap

data class SharingInfo(
    val subject: String?,
    val artwork: Bitmap?
) {
    companion object {
        val empty = SharingInfo(null, null)
    }
}