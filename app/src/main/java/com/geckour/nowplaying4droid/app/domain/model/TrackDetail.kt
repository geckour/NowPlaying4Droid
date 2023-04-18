package com.geckour.nowplaying4droid.app.domain.model

import com.geckour.nowplayingsubjectbuilder.lib.model.TrackInfo
import java.io.Serializable

@kotlinx.serialization.Serializable
data class TrackDetail(
    val coreElement: TrackCoreElement,
    val artworkUriString: String?,
    val playerPackageName: String?,
    val spotifyData: SpotifyResult.Data?,
    val appleMusicData: AppleMusicResult.Data?,
    val youTubeMusicUrl: String?
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

        val spotifySearchQuery = listOfNotNull(
            title?.let { if (it.isAscii) "track:\"$it\"" else "\"$it\"" },
            artist?.let { if (it.isAscii) "artist:\"$it\"" else "\"$it\"" },
            album?.let { if (it.isAscii) "album:\"$it\"" else "\"$it\"" }
        ).joinToString("+")

        val youTubeSearchQuery = "$title $artist"

        val appleMusicSearchQuery =
            "${
                title?.replace(Regex("\\s"), "+").orEmpty()
            }+${
                album?.replace(Regex("\\s"), "+").orEmpty()
            }+${artist?.replace(Regex("\\s"), "+").orEmpty()}"

        fun isStrict(appleMusicData: AppleMusicResult.Data): Boolean =
            title == appleMusicData.trackName &&
                    artist == appleMusicData.artistName &&
                    album == appleMusicData.albumName

        fun withData(
            spotifyData: SpotifyResult.Data?,
            appleMusicData: AppleMusicResult.Data?,
        ): TrackCoreElement = TrackCoreElement(
            spotifyData?.trackName ?: appleMusicData?.trackName ?: title,
            spotifyData?.artistName ?: appleMusicData?.artistName ?: artist,
            spotifyData?.albumName ?: appleMusicData?.albumName ?: album,
            appleMusicData?.composerName ?: composer
        )

        private val String.isAscii: Boolean get() = all { it.code in 0x20..0x7E }
    }

    fun toTrackInfo(): TrackInfo = TrackInfo(
        coreElement.title,
        coreElement.artist,
        coreElement.album,
        coreElement.composer,
        spotifyData?.sharingUrl,
        youTubeMusicUrl,
        appleMusicData?.sharingUrl
    )
}

@kotlinx.serialization.Serializable
data class ArtworkInfo(
    val artworkUriString: String?
)