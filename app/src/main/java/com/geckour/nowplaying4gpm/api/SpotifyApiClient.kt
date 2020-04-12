package com.geckour.nowplaying4gpm.api

import android.content.Context
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.api.model.SpotifyUser
import com.geckour.nowplaying4gpm.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.util.getSpotifyUserInfo
import com.geckour.nowplaying4gpm.util.moshi
import com.geckour.nowplaying4gpm.util.storeSpotifyUserInfoImmediately
import com.geckour.nowplaying4gpm.util.withCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*

class SpotifyApiClient(private val context: Context) {

    companion object {

        const val SPOTIFY_CALLBACK = "np4gpm://spotify.callback"
        private const val SPOTIFY_ENCODED_CALLBACK = "np4gpm%3A%2F%2Fspotify.callback"
        const val OAUTH_URL =
            "https://accounts.spotify.com/authorize?client_id=${BuildConfig.SPOTIFY_CLIENT_ID}&response_type=code&redirect_uri=$SPOTIFY_ENCODED_CALLBACK&scope=user-read-private"
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

    suspend fun storeSpotifyUserInfo(code: String): SpotifyUserInfo? = withContext(Dispatchers.IO) {
        val token = withCatching { authService.getToken(code) }
            ?: return@withContext null
        val userName = withCatching { getUser(token.accessToken).displayName }
            ?: return@withContext null
        return@withContext SpotifyUserInfo(token, userName).apply {
            sharedPreferences.storeSpotifyUserInfoImmediately(this)
        }
    }

    private suspend fun refreshTokenIfNeeded() {
        val token = sharedPreferences.getSpotifyUserInfo()?.token ?: return
        if (System.currentTimeMillis() / 1000 > token.expiresIn) {
            storeSpotifyUserInfo(token.codeForRefreshToken)
        }
    }

    private suspend fun getUser(token: String): SpotifyUser = getService(token).getUser()

    suspend fun getSpotifyUrl(trackCoreElement: TrackInfo.TrackCoreElement): String? {
        return trackCoreElement.spotifySearchQuery?.let {
            withCatching { refreshTokenIfNeeded() }
            withCatching {
                val token =
                    sharedPreferences.getSpotifyUserInfo()?.token
                        ?: throw IllegalStateException("Init token first.")
                val countryCode =
                    if (token.scope == "user-read-private") "from_token"
                    else Locale.getDefault().country
                getService(token.accessToken).searchSpotifyItem(it, marketCountryCode = countryCode)
                    .tracks
                    ?.items
                    ?.firstOrNull()
                    ?.urls
                    ?.get("spotify")
            }
        }
    }
}