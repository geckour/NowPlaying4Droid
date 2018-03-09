package com.geckour.nowplaying4gpm.domain.model

data class TrackInfo(
        val coreElement: TrackCoreElement,
        val artwork: ArtworkInfo =
                ArtworkInfo(
                        null,
                        TrackCoreElement(null, null, null))
) {
    val isArtworkCompatible: Boolean =
            coreElement == artwork.trackCoreElement
}

data class TrackCoreElement(
        val title: String?,
        val artist: String?,
        val album: String?
) {
    val isComplete: Boolean =
            title != null && artist != null && album != null
}

data class ArtworkInfo(
        val artworkUriString: String?,
        val trackCoreElement: TrackCoreElement
)