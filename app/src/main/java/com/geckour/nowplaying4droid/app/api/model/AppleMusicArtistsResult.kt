package com.geckour.nowplaying4droid.app.api.model

import kotlinx.serialization.Serializable

@Serializable
data class AppleMusicArtistsResult(
    val data: List<AppleMusicArtists>,
) {

    @Serializable
    data class AppleMusicArtists(
        val id: String,
        val attributes: AppleMusicArtistsAttributes,
    ) {

        @Serializable
        data class AppleMusicArtistsAttributes(
            val name: String,
        )
    }
}