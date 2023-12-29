package com.geckour.nowplaying4droid.app.domain.model

import com.geckour.nowplayingsubjectbuilder.lib.model.FormatPattern
import com.geckour.nowplayingsubjectbuilder.lib.model.TrackInfo
import java.io.Serializable

@kotlinx.serialization.Serializable
data class TrackDetail(
    val coreElement: TrackCoreElement,
    val releasedAt: String?,
    val artworkUriString: String?,
    val playerPackageName: String?,
    val spotifyData: SpotifyResult.Data?,
    val appleMusicData: AppleMusicResult.Data?,
    val youTubeMusicUrl: String?,
    val pixelNowPlaying: String?,
) : Serializable {

    companion object {

        const val PIXEL_NOW_PLAYING_PACKAGE_NAME = "com.google.android.as"

        val empty = TrackDetail(
            TrackCoreElement(null, null, null, null),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )

        fun fromPixelNowPlaying(
            pixelNowPlaying: String?,
            currentTrackDetail: TrackDetail?,
            artworkUriString: String?,
        ): TrackDetail? =
            pixelNowPlaying?.let {
                (currentTrackDetail ?: empty).copy(
                    playerPackageName = PIXEL_NOW_PLAYING_PACKAGE_NAME,
                    pixelNowPlaying = it,
                    artworkUriString = artworkUriString
                )
            } ?: run {
                // Resetting
                if (currentTrackDetail?.playerPackageName == PIXEL_NOW_PLAYING_PACKAGE_NAME) {
                    return@run null
                }
                val result = currentTrackDetail?.copy(
                    playerPackageName = currentTrackDetail.playerPackageName,
                    pixelNowPlaying = null
                )
                return@run if (result == empty) null else result
            }
    }

    @kotlinx.serialization.Serializable
    data class TrackCoreElement(
        val title: String?,
        val artist: String?,
        val album: String?,
        val composer: String?
    ) : Serializable {

        val contentQueryArgs: Array<String>? =
            if (title != null && artist != null && album != null) arrayOf(title, artist, album)
            else null

        val spotifySearchQuery = listOfNotNull(
            title?.let { if (it.isAscii) "track:\"$it\"" else "\"$it\"" },
            artist?.let { if (it.isAscii) "artist:\"$it\"" else "\"$it\"" },
            album?.let { if (it.isAscii) "album:\"$it\"" else "\"$it\"" }
        ).joinToString("+")

        val youTubeSearchQuery = "$title $artist"

        val appleMusicSearchQuery = listOfNotNull(
            title?.trim()?.replace(Regex("\\s+"), "+"),
            album?.trim()?.replace(Regex("\\s+"), "+"),
            artist?.trim()?.replace(Regex("\\s+"), "+")
        ).joinToString("+")

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
        formatPatterns = listOf(
            FormatPattern("TI", coreElement.title),
            FormatPattern("AR", coreElement.artist),
            FormatPattern("AL", coreElement.album),
            FormatPattern("CO", coreElement.composer),
            FormatPattern("SU", spotifyData?.sharingUrl),
            FormatPattern("YU", youTubeMusicUrl),
            FormatPattern("AU", appleMusicData?.sharingUrl),
            FormatPattern("PN", pixelNowPlaying),
            FormatPattern("PU", releasedAt),
        )
    )

    fun withData(
        spotifyData: SpotifyResult.Data?,
        appleMusicData: AppleMusicResult.Data?,
    ): TrackDetail = this.copy(
        spotifyData = spotifyData,
        appleMusicData = appleMusicData,
        releasedAt = spotifyData?.releasedAt ?: appleMusicData?.releasedAt ?: releasedAt
    )
}

@kotlinx.serialization.Serializable
data class ArtworkInfo(
    val artworkUriString: String?
)