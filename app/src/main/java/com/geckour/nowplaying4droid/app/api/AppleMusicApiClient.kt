package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.domain.model.AppleMusicResult
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.normalizedAppleAlbumName
import com.geckour.nowplaying4droid.app.util.withCatching
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppleMusicApiClient {

    private val service: AppleMusicApiService = Retrofit.Builder()
        .client(OkHttpProvider.appleMusicApiClient)
        .baseUrl("https://api.music.apple.com")
        .addConverterFactory(
            json.asConverterFactory("application/json; charset=UTF8".toMediaType())
        )
        .build()
        .create(AppleMusicApiService::class.java)

    suspend fun searchAppleMusic(
        countryCode: String,
        query: String,
    ): AppleMusicResult {
        val internalCountryCode = countryCode.ifEmpty { "JP" }
        return withCatching(onError = { return AppleMusicResult.Failure(it) }) {
            val searchResult = service.searchAppleMusicItem(
                countryCode = internalCountryCode,
                query = query
            )
            searchResult.results.songs.data.firstOrNull()?.let { song ->
                val data = AppleMusicResult.Data(
                    sharingUrl = song.attributes.url,
                    artworkUrl = song.attributes.artwork.resolvedUrl,
                    trackName = song.attributes.name,
                    artistName = song.attributes.artistName,
                    albumName = song.attributes.albumName,
                    composerName = song.attributes.composerName,
                    releasedAt = song.attributes.releaseDate
                )
                return@let AppleMusicResult.Success(data)
            } ?: AppleMusicResult.Failure(IllegalStateException("No search result"))
        } ?: AppleMusicResult.Failure(IllegalStateException("Unknown error"))
    }

    suspend fun searchAppleMusic(
        countryCode: String,
        trackCoreElement: TrackDetail.TrackCoreElement,
        isStrictMode: Boolean
    ): AppleMusicResult {
        val internalCountryCode = countryCode.ifEmpty { "JP" }
        return withCatching(onError = { return AppleMusicResult.Failure(it) }) {
            val searchResult = service.searchAppleMusicItem(
                countryCode = internalCountryCode,
                query = trackCoreElement.appleMusicSearchQuery
            )
            searchResult.results.songs.data.firstOrNull { appleMusicSong ->
                if (isStrictMode.not()) return@firstOrNull true

                val titleValid = trackCoreElement.title?.let { title ->
                    title
                        .filterNot { it.isWhitespace() }
                        .lowercase() ==
                            appleMusicSong.attributes.name
                                .filterNot { it.isWhitespace() }
                                .lowercase()
                } != false
                val albumValid = trackCoreElement.album?.let { album ->
                    album.normalizedAppleAlbumName() ==
                            appleMusicSong.attributes.albumName.normalizedAppleAlbumName()
                } != false
                val artistValid = trackCoreElement.artist?.let { artist ->
                    artist
                        .filterNot { it.isWhitespace() }
                        .lowercase() ==
                            appleMusicSong.attributes.artistName
                                .filterNot { it.isWhitespace() }
                                .lowercase()
                } != false

                return@firstOrNull titleValid && albumValid && artistValid
            }?.let { song ->
                val data = AppleMusicResult.Data(
                    sharingUrl = song.attributes.url,
                    artworkUrl = song.attributes.artwork.resolvedUrl,
                    trackName = song.attributes.name,
                    artistName = song.attributes.artistName,
                    albumName = song.attributes.albumName,
                    composerName = song.attributes.composerName,
                    releasedAt = song.attributes.releaseDate
                )
                return@let AppleMusicResult.Success(data)
            } ?: AppleMusicResult.Failure(IllegalStateException("No search result"))
        } ?: AppleMusicResult.Failure(IllegalStateException("Unknown error"))
    }
}