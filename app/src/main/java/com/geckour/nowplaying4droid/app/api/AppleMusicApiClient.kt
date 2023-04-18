package com.geckour.nowplaying4droid.app.api

import com.geckour.nowplaying4droid.app.domain.model.AppleMusicResult
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.util.json
import com.geckour.nowplaying4droid.app.util.withCatching
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
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
        trackCoreElement: TrackDetail.TrackCoreElement,
        isStrictMode: Boolean
    ): AppleMusicResult {
        return withCatching(onError = { return AppleMusicResult.Failure(it) }) {
            val searchResult = service.searchAppleMusicItem(
                countryCode = countryCode,
                query = trackCoreElement.appleMusicSearchQuery
            )
            searchResult.results.songs.data.firstOrNull()?.let { song ->
                val artistIds = song.relationships
                    ?.artists?.data
                    ?.map { it.id }
                    .orEmpty()
                    .ifEmpty {
                        val data = AppleMusicResult.Data(
                            sharingUrl = song.attributes.url,
                            artworkUrl = song.attributes.artwork.resolvedUrl,
                            trackName = song.attributes.name,
                            artistName = song.attributes.artistName,
                            albumName = song.attributes.albumName,
                            composerName = song.attributes.composerName,
                        )
                        return@let if (isStrictMode.not() || trackCoreElement.isStrict(data)) {
                            AppleMusicResult.Success(data)
                        } else null
                    }

                val artistsString = service.getAppleMusicArtists(countryCode, artistIds).data
                    .joinToString(", ") { it.attributes.name }

                val data = AppleMusicResult.Data(
                    sharingUrl = song.attributes.url,
                    artworkUrl = song.attributes.artwork.resolvedUrl,
                    trackName = song.attributes.name,
                    artistName = artistsString,
                    albumName = song.attributes.albumName,
                    composerName = song.attributes.composerName,
                )
                return@let if (isStrictMode.not() || trackCoreElement.isStrict(data)) {
                    AppleMusicResult.Success(data)
                } else null
            } ?: AppleMusicResult.Failure(IllegalStateException("No search result"))
        } ?: AppleMusicResult.Failure(IllegalStateException("Unknown error"))
    }
}