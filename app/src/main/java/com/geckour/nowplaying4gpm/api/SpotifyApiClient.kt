package com.geckour.nowplaying4gpm.api

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.model.SpotifyUser
import com.geckour.nowplaying4gpm.domain.model.SpotifySearchResult
import com.geckour.nowplaying4gpm.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.util.getSpotifyUserInfo
import com.geckour.nowplaying4gpm.util.moshi
import com.geckour.nowplaying4gpm.util.storeSpotifyUserInfoImmediately
import com.geckour.nowplaying4gpm.util.withCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SpotifyAuthService::class.java)

    private fun getService(token: String): SpotifyApiService = Retrofit.Builder()
        .client(OkHttpProvider.getSpotifyApiClient(token))
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SpotifyApiService::class.java)

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var refreshTokenJob: Job? = null
    private var storeSpotifyUserInfoJob: Job? = null

    private val _refreshedUserInfo = MutableLiveData<SpotifyUserInfo>()
    val refreshedUserInfo: LiveData<SpotifyUserInfo> = _refreshedUserInfo

    suspend fun storeSpotifyUserInfo(code: String) = coroutineScope {
        storeSpotifyUserInfoJob?.join()
        storeSpotifyUserInfoJob = launch(Dispatchers.IO) {
            val token = withCatching { authService.getToken(code) }
                ?: return@launch
            val userName = getUser(token.accessToken)?.displayName
                ?: return@launch
            val userInfo = SpotifyUserInfo(
                token,
                userName,
                token.getExpiredAt()
            )
            sharedPreferences.storeSpotifyUserInfoImmediately(userInfo)
            _refreshedUserInfo.postValue(userInfo)
        }
    }

    suspend fun refreshTokenIfNeeded() = coroutineScope {
        val currentUserInfo = sharedPreferences.getSpotifyUserInfo() ?: return@coroutineScope
        if (currentUserInfo.refreshTokenExpiredAt == null ||
            currentUserInfo.refreshTokenExpiredAt >= System.currentTimeMillis()
        ) {
            refreshTokenJob?.join()
            refreshTokenJob = launch {
                val currentRefreshToken = currentUserInfo.token.refreshToken ?: return@launch
                val newToken =
                    withCatching { authService.refreshToken(currentRefreshToken) } ?: return@launch
                val newUserInfo = currentUserInfo.copy(
                    token = newToken.copy(
                        refreshToken = newToken.refreshToken ?: currentRefreshToken
                    ),
                    refreshTokenExpiredAt = newToken.getExpiredAt()
                        ?: currentUserInfo.refreshTokenExpiredAt
                )
                sharedPreferences.storeSpotifyUserInfoImmediately(newUserInfo)
                _refreshedUserInfo.postValue(newUserInfo)
            }
        }
    }

    private suspend fun getUser(token: String): SpotifyUser? =
        withCatching { getService(token).getUser() }

    suspend fun getSpotifyUrl(trackCoreElement: TrackInfo.TrackCoreElement): SpotifySearchResult {
        refreshTokenIfNeeded()
        refreshTokenJob?.join()
        val query = trackCoreElement.spotifySearchQuery
        return withCatching({ return SpotifySearchResult.Failure(query, it) }) {
            val token =
                sharedPreferences.getSpotifyUserInfo()?.token
                    ?: throw IllegalStateException("Init token first.")
            val countryCode =
                if (token.scope == "user-read-private") "from_token"
                else Locale.getDefault().country
            getService(token.accessToken).searchSpotifyItem(query, marketCountryCode = countryCode)
                .tracks
                ?.items
                ?.firstOrNull()
                ?.urls
                ?.get("spotify")
                ?.let {
                    SpotifySearchResult.Success(query, it)
                } ?: SpotifySearchResult.Failure(query, IllegalStateException("No search result"))
        } ?: SpotifySearchResult.Failure(null, IllegalStateException("Unknown error"))
    }
}