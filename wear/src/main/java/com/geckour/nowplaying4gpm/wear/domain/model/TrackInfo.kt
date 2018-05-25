package com.geckour.nowplaying4gpm.wear.domain.model

data class TrackInfo(
        val subject: String?,
        val base64Artwork: String?
) {
    companion object {
        val empty = TrackInfo(null, null)
    }
}