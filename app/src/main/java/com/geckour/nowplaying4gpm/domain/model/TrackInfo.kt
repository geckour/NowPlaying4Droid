package com.geckour.nowplaying4gpm.domain.model

import com.geckour.nowplaying4gpm.util.FormatPattern
import com.geckour.nowplaying4gpm.util.containedPatterns
import java.io.Serializable

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

    data class TrackCoreElement(
        val title: String?,
        val artist: String?,
        val album: String?,
        val composer: String?
    ) : Serializable {

        val isAllNonNull: Boolean
            get() = title != null && artist != null && album != null

        val spotifySearchQuery: String?
            get() =
                if (this.isAllNonNull)
                    "track:\"$title\" \"$artist\" album:\"$album\""
                else null
    }
}

data class ArtworkInfo(
    val artworkUriString: String?
)