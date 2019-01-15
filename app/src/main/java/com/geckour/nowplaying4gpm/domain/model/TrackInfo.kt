package com.geckour.nowplaying4gpm.domain.model

import java.io.Serializable

data class TrackInfo(
        val coreElement: TrackCoreElement,
        val artworkUriString: String?,
        val playerPackageName: String?,
        val playerAppName: String?,
        val spotifyUrl: String?
) : Serializable {
    companion object {
        val empty: TrackInfo =
                TrackInfo(TrackCoreElement.empty,
                        null,
                        null,
                        null,
                        null)
    }
}

data class ArtworkInfo(
        val artworkUriString: String?
)

data class TrackCoreElement(
        val title: String?,
        val artist: String?,
        val album: String?
) : Serializable {
    companion object {
        val empty: TrackCoreElement = TrackCoreElement(null, null, null)
    }

    val isAllNonNull: Boolean =
            title != null && artist != null && album != null

    val spotifySearchQuery: String? =
            if (this.isAllNonNull)
                "\"$title\" \"$artist\" album:\"$album\""
            else null
}