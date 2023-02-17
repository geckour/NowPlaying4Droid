package com.geckour.nowplaying4droid.app.api.model

@kotlinx.serialization.Serializable
data class YouTubeSearchResult(
    val items: List<Item>
) {

    @kotlinx.serialization.Serializable
    data class Item(
       val id: ID
    ) {

        @kotlinx.serialization.Serializable
        data class ID(
            val videoId: String
        )
    }
}
