package com.geckour.nowplaying4droid.app.domain.model

import com.geckour.nowplayingsubjectbuilder.lib.model.TrackInfo
import java.io.Serializable

@kotlinx.serialization.Serializable
data class TrackDetail(
    val coreElement: TrackCoreElement,
    val artworkUriString: String?,
    val playerPackageName: String?,
    val spotifyData: SpotifyResult.Data?
) : Serializable {

    @kotlinx.serialization.Serializable
    data class TrackCoreElement(
        val title: String?,
        val artist: String?,
        val album: String?,
        val composer: String?
    ) : Serializable {

        val isAllNonNull: Boolean
            get() = title != null && artist != null && album != null

        val spotifySearchQuery: String
            get() = listOfNotNull(
                title?.let { if (it.isAscii) "track:\"$it\"" else "\"$it\"" },
                artist?.let { if (it.isAscii) "artist:\"$it\"" else "\"$it\"" },
                album?.let { if (it.isAscii) "album:\"$it\"" else "\"$it\"" }
            ).joinToString("+")

        private val String.isAscii: Boolean get() = all { it.code in 0x20..0x7E }
    }

    fun toTrackInfo(): TrackInfo = TrackInfo(
        coreElement.title,
        coreElement.artist,
        coreElement.album,
        coreElement.composer,
        spotifyData?.sharingUrl
    )
}

@kotlinx.serialization.Serializable
data class ArtworkInfo(
    val artworkUriString: String?
)