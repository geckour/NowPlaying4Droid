package com.geckour.nowplaying4gpm.domain.model

import java.io.Serializable

data class TrackInfo(
        val coreElement: TrackCoreElement,
        val artworkUriString: String?
): Serializable

data class ArtworkInfo(
        val artworkUriString: String?,
        val trackCoreElement: TrackCoreElement
)

data class TrackCoreElement(
        val title: String?,
        val artist: String?,
        val album: String?
): Serializable {
    val isAllNonNull: Boolean =
            title != null && artist != null && album != null
}