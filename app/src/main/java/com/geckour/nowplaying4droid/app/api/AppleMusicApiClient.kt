package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.domain.model.AppleMusicResult
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.withCatching
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import timber.log.Timber
import java.util.*

class AppleMusicApiClient {

    @OptIn(ExperimentalSerializationApi::class)
    private val service: AppleMusicApiService = Retrofit.Builder()
        .client(OkHttpProvider.appleMusicApiClient)
        .baseUrl("https://api.music.apple.com")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AppleMusicApiService::class.java)

    suspend fun searchAppleMusic(
        countryCode: String,
        query: String,
    ): AppleMusicResult {
        return withCatching(onError = { return AppleMusicResult.Failure(it) }) {
            val searchResult = service.searchAppleMusicItem(
                countryCode = countryCode,
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
        return withCatching(onError = { return AppleMusicResult.Failure(it) }) {
            val searchResult = service.searchAppleMusicItem(
                countryCode = countryCode,
                query = trackCoreElement.appleMusicSearchQuery
            )
            searchResult.results.songs.data.firstOrNull { appleMusicSong ->
                if (isStrictMode.not()) return@firstOrNull true

                val titleValid = trackCoreElement.title?.let { title ->
                    title.filterNot { it.isWhitespace() }.lowercase() ==
                            appleMusicSong.attributes.name
                                .filterNot { it.isWhitespace() }
                                .lowercase()
                } != false
                val albumValid = trackCoreElement.album?.let { album ->
                    album.removeSuffix(" - EP")
                        .removeSuffix(" - Single")
                        .filterNot { it.isWhitespace() }
                        .lowercase().apply { Timber. d("np4d album name processed: $this") } ==
                            appleMusicSong.attributes.albumName
                                .removeSuffix(" - EP")
                                .removeSuffix(" - Single")
                                .filterNot { it.isWhitespace() }
                                .lowercase().apply { Timber. d("np4d Apple album name processed: $this") }
                } != false
                val artistValid = trackCoreElement.artist?.let { artist ->
                    artist.filterNot { it.isWhitespace() }.lowercase() ==
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