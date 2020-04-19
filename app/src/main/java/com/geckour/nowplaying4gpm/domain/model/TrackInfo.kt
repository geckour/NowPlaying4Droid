package com.geckour.nowplaying4gpm.domain.model

import com.geckour.nowplaying4gpm.util.FormatPattern
import com.geckour.nowplaying4gpm.util.containedPatterns
import java.io.Serializable

@kotlinx.serialization.Serializable
data class TrackInfo(
    val coreElement: TrackCoreElement,
    val artworkUriString: String?,
    val playerPackageName: String?,
    val playerAppName: String?,
    val spotifyUrl: String?
) : Serializable {

    fun isSatisfiedSpecifier(sharingFormatText: String): Boolean =
        sharingFormatText.containedPatterns.all {
            when (it) {
                FormatPattern.TITLE -> this.coreElement.title != null
                FormatPattern.ARTIST -> this.coreElement.artist != null
                FormatPattern.ALBUM -> this.coreElement.album != null
                FormatPattern.COMPOSER -> this.coreElement.composer != null
                FormatPattern.PLAYER_NAME -> this.playerAppName != null
                FormatPattern.SPOTIFY_URL -> this.spotifyUrl != null
                else -> true
            }
        }

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
            ).joinToString(" ")

        val String.isAscii: Boolean get() = all { it.toInt() in 0x20..0x7E }
    }
}

@kotlinx.serialization.Serializable
data class ArtworkInfo(
    val artworkUriString: String?
)