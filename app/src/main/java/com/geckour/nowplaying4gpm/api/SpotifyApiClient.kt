package com.geckour.nowplaying4gpm.api

import android.content.Context
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.model.SpotifyUser
import com.geckour.nowplaying4gpm.domain.model.SpotifySearchResult
import com.geckour.nowplaying4gpm.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.util.getSpotifyUserInfo
import com.geckour.nowplaying4gpm.util.json
import com.geckour.nowplaying4gpm.util.storeSpotifyUserInfoImmediately
import com.geckour.nowplaying4gpm.util.withCatching
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import timber.log.Timber
import java.util.*

class SpotifyApiClient(context: Context) {

    companion object {

        const val SPOTIFY_CALLBACK = "np4gpm://spotify.callback"
        private const val SPOTIFY_CALLBACK_ENCODED = "np4gpm%3A%2F%2Fspotify.callback"
        const val OAUTH_URL =
            "https://accounts.spotify.com/authorize?client_id=${BuildConfig.SPOTIFY_CLIENT_ID}&response_type=code&redirect_uri=$SPOTIFY_CALLBACK_ENCODED&scope=user-read-private"
    }

    private val authService = Retrofit.Builder()
        .client(OkHttpProvider.spotifyAuthClient)
        .baseUrl("https://accounts.spotify.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SpotifyAuthService::class.java)

    private fun getService(token: String): SpotifyApiService = Retrofit.Builder()
        .client(OkHttpProvider.getSpotifyApiClient(token))
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SpotifyApiService::class.java)

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var currentQueryAndResult: Pair<String, SpotifySearchResult>? = null

    suspend fun storeSpotifyUserInfo(code: String): SpotifyUserInfo? = withContext(Dispatchers.IO) {
        val token = withCatching { authService.getToken(code) }
            ?: return@withContext null
        val userName = getUser(token.accessToken)?.displayName
            ?: return@withContext null
        val userInfo = SpotifyUserInfo(
            token,
            userName,
            token.getExpiredAt()
        )
        sharedPreferences.storeSpotifyUserInfoImmediately(userInfo)
        return@withContext userInfo
    }

    private suspend fun refreshTokenIfNeeded(): SpotifyUserInfo? {
        val currentUserInfo = sharedPreferences.getSpotifyUserInfo() ?: return null
        if (currentUserInfo.refreshTokenExpiredAt == null ||
            currentUserInfo.refreshTokenExpiredAt <= System.currentTimeMillis()
        ) {
            val currentRefreshToken = currentUserInfo.token.refreshToken ?: return null
            val newToken =
                withCatching { authService.refreshToken(currentRefreshToken) } ?: return null
            val newUserInfo = currentUserInfo.copy(
                token = newToken.copy(
                    refreshToken = newToken.refreshToken ?: currentRefreshToken
                ),
                refreshTokenExpiredAt = newToken.getExpiredAt()
                    ?: currentUserInfo.refreshTokenExpiredAt
            )
            sharedPreferences.storeSpotifyUserInfoImmediately(newUserInfo)
            return newUserInfo
        }

        return null
    }

    private suspend fun getUser(token: String): SpotifyUser? =
        withCatching { getService(token.apply { Timber.d("np4d spotify token: $this") }).getUser() }

    suspend fun getSpotifyData(trackCoreElement: TrackInfo.TrackCoreElement): SpotifySearchResult {
        val query = trackCoreElement.spotifySearchQuery
        currentQueryAndResult?.let {
            if (it.second !is SpotifySearchResult.Failure && query == it.first) return it.second
        }
        return (withCatching({ return SpotifySearchResult.Failure(query, it) }) {
            val token =
                (refreshTokenIfNeeded() ?: sharedPreferences.getSpotifyUserInfo())?.token
                    ?: throw IllegalStateException("Init token first.")
            val countryCode =
                if (token.scope == "user-read-private") "from_token"
                else Locale.getDefault().country
            getService(token.accessToken).searchSpotifyItem(query, marketCountryCode = countryCode)
                .tracks
                ?.items
                ?.firstOrNull()
                ?.let {
                    Timber.d("np4d track: $it")
                    SpotifySearchResult.Success(
                        query,
                        SpotifySearchResult.Data(
                            it.urls["spotify"] ?: return@let null,
                            it.album.images.firstOrNull()?.url,
                            it.name,
                            it.artistString,
                            it.album.name
                        )
                    )
                } ?: SpotifySearchResult.Failure(query, IllegalStateException("No search result"))
        } ?: SpotifySearchResult.Failure(null, IllegalStateException("Unknown error"))).apply {
            currentQueryAndResult = query to this
        }
    }
}