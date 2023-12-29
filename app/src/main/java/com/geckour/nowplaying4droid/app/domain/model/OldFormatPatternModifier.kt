package com.geckour.nowplaying4droid.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OldFormatPatternModifier(
    val key: OldFormatPattern,
    val prefix: String?,
    val suffix: String?,
) {

    enum class OldFormatPattern(val value: String) {
        S_QUOTE("'"),
        S_QUOTE_DOUBLE("''"),
        TITLE("TI"),
        ARTIST("AR"),
        ALBUM("AL"),
        COMPOSER("CO"),
        SPOTIFY_URL("SU"),
        YOUTUBE_MUSIC_URL("YU"),
        APPLE_MUSIC_URL("AU"),
        PIXEL_NOW_PLAYING("PN"),
        NEW_LINE("\\n")
    }
}
