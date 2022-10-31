package com.geckour.nowplaying4droid.wear.domain.model

data class SharingInfo(
    val subject: String?,
    val artwork: ByteArray?
) {
    companion object {
        val empty = SharingInfo(null, null)
    }
}