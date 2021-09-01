package com.geckour.nowplaying4gpm.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.getChosePaletteColor
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getSpotifyUserInfo
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getTwitterAccessToken
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val spotifyApiClient: SpotifyApiClient,
    sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {

    internal var showingNotificationServicePermissionDialog = false

    internal val settingsVisible = mutableStateOf(false)
    internal val donated = mutableStateOf(false)

    internal val spotifyEnabledState =
        mutableStateOf(sharedPreferences.getSpotifyUserInfo() != null)

    internal val errorDialogData = mutableStateOf<ErrorDialogData?>(null)

    internal val patternFormatSummary =
        mutableStateOf<String?>(sharedPreferences.getFormatPattern(application))
    internal val postMastodonDelaySummary =
        mutableStateOf<String?>(
            (getApplication() as Context).getString(
                R.string.pref_item_summary_delay_mastodon,
                sharedPreferences.getDelayDurationPostMastodon()
            ))
    internal val postMastodonVisibilitySummary =
        mutableStateOf<String?>(
            (getApplication() as Context).getString(
                sharedPreferences.getVisibilityMastodon().getSummaryResId()
            )
        )
    internal val chosePaletteColorSummary =
        mutableStateOf<String?>(
            (getApplication() as Context).getString(
                sharedPreferences.getChosePaletteColor().getSummaryResId()
            )
        )
    internal val authSpotifySummary =
        mutableStateOf<String?>(sharedPreferences.getSpotifyUserInfo()?.userName ?: "")
    internal val authMastodonSummary = mutableStateOf<String?>("")
    internal val authTwitterSummary =
        mutableStateOf<String?>(sharedPreferences.getTwitterAccessToken()?.screenName ?: "")

    internal val openChangeArtworkResolveOrderDialog = mutableStateOf(false)
    internal val openChangePatternFormatDialog = mutableStateOf(false)
    internal val openEditPatternModifierDialog = mutableStateOf(false)
    internal val openAuthMastodonDialog = mutableStateOf(false)
    internal val openSetMastodonPostDelayDialog = mutableStateOf(false)
    internal val openSetMastodonPostVisibilityDialog = mutableStateOf(false)
    internal val openSelectPlayerPostMastodonDialog = mutableStateOf(false)
    internal val openSelectNotificationColorDialog = mutableStateOf(false)

    internal val snackbarHostState = mutableStateOf(SnackbarHostState())

    internal fun storeSpotifyUserInfo(verifier: String) = viewModelScope.launch {
        val userInfo = spotifyApiClient.storeSpotifyUserInfo(verifier)

        authSpotifySummary.value = userInfo?.userName
        userInfo ?: return@launch

        spotifyEnabledState.value = true
        snackbarHostState.value
            .showSnackbar(
                message = (getApplication() as Context).getString(
                    R.string.snackbar_text_success_auth_spotify
                )
            )
    }


    data class Item(
        val sharedPreferences: SharedPreferences,
        @StringRes val titleStringRes: Int,
        @StringRes val descStringRes: Int,
        val switchPrefKey: PrefKey? = null,
        val switchState: MutableState<Boolean?> = mutableStateOf(
            switchPrefKey?.let { sharedPreferences.getSwitchState(it) }
        ),
        val onSwitchCheckedChanged: (Boolean) -> Unit = {},
        val summary: MutableState<String?> = mutableStateOf(switchPrefKey?.let { "" }),
        val enabled: MutableState<Boolean> = mutableStateOf(true),
        val visible: Boolean = true,
        val onClick: (item: Item) -> Unit = {},
    )

    data class ErrorDialogData(
        @StringRes val titleRes: Int,
        @StringRes val textRes: Int,
        val onDismiss: () -> Unit = {}
    )
}