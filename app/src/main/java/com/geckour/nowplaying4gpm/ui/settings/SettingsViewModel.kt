package com.geckour.nowplaying4gpm.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.api.SpotifyApiClient
import com.geckour.nowplaying4gpm.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4gpm.ui.SingleLiveEvent
import com.geckour.nowplaying4gpm.util.PrefKey
import com.geckour.nowplaying4gpm.util.getChosePaletteColor
import com.geckour.nowplaying4gpm.util.getDelayDurationPostMastodon
import com.geckour.nowplaying4gpm.util.getFormatPattern
import com.geckour.nowplaying4gpm.util.getMastodonUserInfo
import com.geckour.nowplaying4gpm.util.getSpotifyUserInfo
import com.geckour.nowplaying4gpm.util.getSwitchState
import com.geckour.nowplaying4gpm.util.getVisibilityMastodon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val spotifyApiClient: SpotifyApiClient,
    sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {

    internal var showingNotificationServicePermissionDialog = false

    internal val spotifyUserInfo = SingleLiveEvent<SpotifyUserInfo>()

    private val eventChannel = Channel<Event>(Channel.CONFLATED)
    internal val event = eventChannel.receiveAsFlow()

    internal val settingsVisible = mutableStateOf(false)
    internal val donated = mutableStateOf(false)
    internal val spotifyAuthenticated = mutableStateOf(false)
    internal val setting: MutableState<Setting> = mutableStateOf(
        Setting(
            listOf(
                Setting.Category(
                    R.string.pref_category_general,
                    listOf(
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_change_artwork_resolve_order,
                            R.string.pref_item_desc_change_artwork_resolve_order
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.ChangeArtworkResolveOrder)
                            }
                        },
                    )
                ),
                Setting.Category(
                    R.string.pref_category_share,
                    listOf(
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_pattern,
                            R.string.pref_item_desc_pattern,
                            summary = mutableStateOf(sharedPreferences.getFormatPattern(application))
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.ChangePatternFormat(it.summary))
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_pattern_modifiers,
                            R.string.pref_item_desc_pattern_modifiers
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.EditPatternModifier)
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_simplify_share,
                            R.string.pref_item_desc_simplify_share,
                            PrefKey.PREF_KEY_WHETHER_USE_SIMPLE_SHARE
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_auth_spotify,
                            R.string.pref_item_desc_auth_spotify,
                            summary = mutableStateOf(
                                sharedPreferences.getSpotifyUserInfo()?.userName ?: ""
                            )
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.AuthSpotify)
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_use_spotify_data,
                            R.string.pref_item_desc_use_spotify_data,
                            PrefKey.PREF_KEY_WHETHER_USE_SPOTIFY_DATA,
                            enabled = mutableStateOf(sharedPreferences.getSpotifyUserInfo() != null)
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_strict_match_pattern,
                            R.string.pref_item_desc_strict_match_pattern,
                            PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_bundle_artwork,
                            R.string.pref_item_desc_switch_bundle_artwork,
                            PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_copy_into_clipboard,
                            R.string.pref_item_desc_switch_copy_into_clipboard,
                            PrefKey.PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_auto_post_mastodon,
                            R.string.pref_item_desc_switch_auto_post_mastodon,
                            PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON,
                            onSwitchCheckedChanged = { state ->
                                mastodonEnabledStates.forEach { it.value = state }
                                reflectSetting()
                            }
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_auth_mastodon,
                            R.string.pref_item_desc_auth_mastodon,
                            summary = mutableStateOf(sharedPreferences.getMastodonUserInfo()?.let {
                                application.getString(
                                    R.string.pref_item_summary_auth_mastodon,
                                    it.userName,
                                    it.instanceName
                                )
                            } ?: ""),
                            enabled = mutableStateOf(
                                sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                                )
                            )
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.AuthMastodon)
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_delay_mastodon,
                            R.string.pref_item_desc_delay_mastodon,
                            summary = mutableStateOf(
                                application.getString(
                                    R.string.pref_item_summary_delay_mastodon,
                                    sharedPreferences.getDelayDurationPostMastodon()
                                )
                            ),
                            enabled = mutableStateOf(
                                sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                                )
                            )
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.SetMastodonPostDelay(it.summary))
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_visibility_mastodon,
                            R.string.pref_item_desc_visibility_mastodon,
                            summary = mutableStateOf(
                                application.getString(
                                    sharedPreferences.getVisibilityMastodon().getSummaryResId()
                                )
                            ),
                            enabled = mutableStateOf(
                                sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                                )
                            )
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.SetMastodonPostVisibility(it.summary))
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_player_package_mastodon,
                            R.string.pref_item_desc_player_package_mastodon,
                            enabled = mutableStateOf(
                                sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                                )
                            )
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.SelectPlayerPostMastodon)
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_success_notification_mastodon,
                            R.string.pref_item_desc_switch_success_notification_mastodon,
                            PrefKey.PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON,
                            enabled = mutableStateOf(
                                sharedPreferences.getSwitchState(
                                    PrefKey.PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON
                                )
                            )
                        ),
                    )
                ),
                Setting.Category(
                    R.string.pref_category_notification,
                    listOf(
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_reside_notification,
                            R.string.pref_item_desc_switch_reside_notification,
                            PrefKey.PREF_KEY_WHETHER_RESIDE
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_show_artwork_in_notification,
                            R.string.pref_item_desc_switch_show_artwork_in_notification,
                            PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_choose_color,
                            R.string.pref_item_desc_choose_color,
                            summary = mutableStateOf(
                                application.getString(
                                    sharedPreferences.getChosePaletteColor().getSummaryResId()
                                )
                            )
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.SelectNotificationColor(it.summary))
                            }
                        },
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_colorize_notification_bg,
                            R.string.pref_item_desc_switch_colorize_notification_bg,
                            PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG
                        ),
                    )
                ),
                Setting.Category(
                    R.string.pref_category_widget,
                    listOf(
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_show_artwork_in_widget,
                            R.string.pref_item_desc_switch_show_artwork_in_widget,
                            PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_launch_player_on_click_widget_artwork,
                            R.string.pref_item_desc_switch_launch_player_on_click_widget_artwork,
                            PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK
                        ),
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_switch_show_clear_button_in_widget,
                            R.string.pref_item_desc_switch_show_clear_button_in_widget,
                            PrefKey.PREF_KEY_WHETHER_SHOW_CLEAR_BUTTON_IN_WIDGET
                        ),
                    )
                ),
                Setting.Category(
                    R.string.pref_category_wear,
                    listOf(
                        Setting.Category.Item(
                            sharedPreferences,
                            R.string.pref_item_title_auth_twitter,
                            R.string.pref_item_desc_auth_twitter,
                            summary = mutableStateOf("")
                        ) {
                            viewModelScope.launch {
                                eventChannel.send(Event.AuthTwitter)
                            }
                        },
                    )
                ),
            ) + if (donated.value) {
                listOf(
                    Setting.Category(
                        R.string.pref_category_others,
                        listOf(
                            Setting.Category.Item(
                                sharedPreferences,
                                R.string.pref_item_title_donate,
                                R.string.pref_item_desc_donate
                            ) {
                                viewModelScope.launch {
                                    eventChannel.send(Event.Donate)
                                }
                            },
                        )
                    )
                )
            } else emptyList()
        )
    )

    val authTwitterSummary = setting.value.categories[4].items[0].summary
    val authMastodonSummary = setting.value.categories[1].items[9].summary
    val authSpotifySummary = setting.value.categories[1].items[3].summary
    val mastodonEnabledStates = listOf(
        setting.value.categories[1].items[9].enabled,
        setting.value.categories[1].items[10].enabled,
        setting.value.categories[1].items[11].enabled,
        setting.value.categories[1].items[12].enabled,
        setting.value.categories[1].items[13].enabled,
    )
    val spotifyEnabledStates = listOf(
        setting.value.categories[1].items[4].enabled,
    )

    internal fun reflectSetting() {
        setting.value = setting.value
    }

    internal fun storeSpotifyUserInfo(verifier: String) = viewModelScope.launch(Dispatchers.IO) {
        spotifyUserInfo.postValue(spotifyApiClient.storeSpotifyUserInfo(verifier))
    }

    data class Setting(
        val categories: List<Category>
    ) {

        data class Category(
            @StringRes val nameStringRes: Int,
            val items: List<Item>
        ) {

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
        }
    }

    sealed class Event {
        object ChangeArtworkResolveOrder : Event()
        data class ChangePatternFormat(val summary: MutableState<String?>) : Event()
        object EditPatternModifier : Event()
        object AuthSpotify : Event()
        object AuthMastodon : Event()
        data class SetMastodonPostDelay(val summary: MutableState<String?>) : Event()
        data class SetMastodonPostVisibility(val summary: MutableState<String?>) : Event()
        object SelectPlayerPostMastodon : Event()
        data class SelectNotificationColor(val summary: MutableState<String?>) : Event()
        object AuthTwitter : Event()
        object Donate : Event()
    }
}