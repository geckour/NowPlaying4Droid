package com.geckour.nowplaying4droid.app.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppleMusicSearchData(
    val results: AppleMusicSearchResults
) {

    @Serializable
    data class AppleMusicSearchResults(
        val songs: AppleMusicSongs
    ) {

        @Serializable
        data class AppleMusicSongs(
            @SerialName("href")
            val queryUrl: String,

            val data: List<AppleMusicSong>,
        ) {

            @Serializable
            data class AppleMusicSong(
                val id: String,
                val type: String,
                val href: String,
                val attributes: AppleMusicSongAttributes,
            ) {

                @Serializable
                data class AppleMusicSongAttributes(
                    val name: String,
                    val artistName: String,
                    val albumName: String,
                    val composerName: String?,
                    val releaseDate: String,
                    val url: String,
                    val artwork: AppleMusicArtwork,
                ) {

                    @Serializable
                    data class AppleMusicArtwork(
                        val url: String,
                        val width: Int,
                        val height: Int,
                    ) {

                        val resolvedUrl =
                            url.replace("{w}", width.toString())
                                .replace("{h}", height.toString())
                    }
                }
            }
        }
    }
}