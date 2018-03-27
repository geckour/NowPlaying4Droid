package com.geckour.nowplaying4gpm.domain.model

import java.io.Serializable

data class TrackInfo(
        val coreElement: TrackCoreElement,
        val artwork: ArtworkInfo =
                ArtworkInfo(
                        null,
                        TrackCoreElement(null, null, null),
                        false)
) {
    val isArtworkSame: Boolean =
            coreElement == artwork.trackCoreElement
}

data class TrackCoreElement(
        val title: String?,
        val artist: String?,
        val album: String?
): Serializable {
    val isAllNonNull: Boolean =
            title != null && artist != null && album != null
}

data class ArtworkInfo(
        val artworkUriString: String?,
        val trackCoreElement: TrackCoreElement,
        val fromContentResolver: Boolean
)