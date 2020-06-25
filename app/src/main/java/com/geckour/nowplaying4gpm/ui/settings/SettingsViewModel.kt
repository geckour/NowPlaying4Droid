package com.geckour.nowplaying4gpm.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4gpm.ui.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val spotifyApiClient = SpotifyApiClient(application)

    internal var showingNotificationServicePermissionDialog = false

    internal val reflectDonation = SingleLiveEvent<Boolean>()
    internal val spotifyUserInfo = SingleLiveEvent<SpotifyUserInfo>()

    internal fun storeSpotifyUserInfo(verifier: String) = viewModelScope.launch(Dispatchers.IO) {
        spotifyUserInfo.postValue(spotifyApiClient.storeSpotifyUserInfo(verifier))
    }
}