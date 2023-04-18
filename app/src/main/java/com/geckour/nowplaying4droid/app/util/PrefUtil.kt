package com.geckour.nowplaying4droid.app.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.domain.model.ArtworkInfo
import com.geckour.nowplaying4droid.app.domain.model.MastodonUserInfo
import com.geckour.nowplaying4droid.app.domain.model.PackageState
import com.geckour.nowplaying4droid.app.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplayingsubjectbuilder.lib.model.FormatPattern
import com.geckour.nowplayingsubjectbuilder.lib.model.FormatPatternModifier
import com.geckour.nowplayingsubjectbuilder.lib.model.TrackInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

enum class PrefKey(val defaultValue: Any? = null) {
    PREF_KEY_ARTWORK_RESOLVE_ORDER,
    PREF_KEY_PATTERN_FORMAT_SHARE_TEXT("#NowPlaying TI - AR (AL)"),
    PREF_KEY_FORMAT_PATTERN_MODIFIERS,
    PREF_KEY_STRICT_MATCH_PATTERN_MODE(false),
    PREF_KEY_WHETHER_BUNDLE_ARTWORK(true),
    PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD(false),
    PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON(false),
    PREF_KEY_DELAY_POST_MASTODON(2000L),
    PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON(emptyList<String>()),
    PREF_KEY_PACKAGE_LIST_SPOTIFY(emptyList<String>()),
    PREF_KEY_PACKAGE_LIST_APPLE_MUSIC(emptyList<String>()),
    PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON(false),
    PREF_KEY_WHETHER_RESIDE(true),
    PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION(true),
    PREF_KEY_CHOSEN_PALETTE_COLOR(PaletteColor.LIGHT_VIBRANT.ordinal),
    PREF_KEY_CHOSEN_MASTODON_VISIBILITY(Visibility.PUBLIC.ordinal),
    PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG(true),
    PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET(true),
    PREF_KEY_WHETHER_LAUNCH_PLAYER_WITH_WIDGET_ARTWORK(true),
    PREF_KEY_WHETHER_SHOW_CLEAR_BUTTON_IN_WIDGET(false),
    PREF_KEY_CURRENT_TRACK_INFO,
    PREF_KEY_TEMP_ARTWORK_INFO,
    PREF_KEY_BILLING_DONATE(false),
    PREF_KEY_SPOTIFY_USER_INFO,
    PREF_KEY_MASTODON_USER_INFO,
    PREF_KEY_FLAG_ALERT_AUTH_TWITTER(false),
    PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE,
    PREF_KEY_DEBUG_SPOTIFY_SEARCH(false),
    PREF_KEY_DENIED_IGNORE_BATTERY_OPTIMIZATION(false),
    PREF_KEY_WHETHER_USE_SIMPLE_SHARE(false),
    PREF_KEY_WHETHER_USE_SPOTIFY_DATA(false),
    PREF_KEY_WHETHER_SEARCH_SPOTIFY_STRICTLY(false),
    PREF_KEY_WHETHER_USE_APPLE_MUSIC_DATA(false),
    PREF_KEY_WHETHER_SEARCH_APPLE_MUSIC_STRICTLY(false),
}

@Serializable
data class ArtworkResolveMethod(
    val key: ArtworkResolveMethodKey,
    val enabled: Boolean
) {
    enum class ArtworkResolveMethodKey(val strResId: Int) {
        CONTENT_RESOLVER(R.string.dialog_list_item_content_resolver),
        MEDIA_METADATA_URI(R.string.dialog_list_item_media_metadata_uri),
        SPOTIFY(R.string.dialog_list_item_spotify),
        APPLE_MUSIC(R.string.dialog_list_item_apple_music),
        MEDIA_METADATA_BITMAP(R.string.dialog_list_item_media_metadata_bitmap),
        NOTIFICATION_BITMAP(R.string.dialog_list_item_notification_bitmap),
        LAST_FM(R.string.dialog_list_item_last_fm)
    }
}

fun SharedPreferences.refreshCurrentTrackDetail(trackDetail: TrackDetail?) {
    edit {
        putString(
            PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name,
            trackDetail?.let { json.encodeToString(it) }
        )
    }
}

fun SharedPreferences.setArtworkResolveOrder(order: List<ArtworkResolveMethod>) {
    edit { putString(PrefKey.PREF_KEY_ARTWORK_RESOLVE_ORDER.name, json.encodeToString(order)) }
}

fun SharedPreferences.getArtworkResolveOrder(): List<ArtworkResolveMethod> =
    getString(PrefKey.PREF_KEY_ARTWORK_RESOLVE_ORDER.name, null)?.let {
        json.parseListOrNull<ArtworkResolveMethod>(it)
    }.orEmpty().let { stored ->
        val origin = ArtworkResolveMethod.ArtworkResolveMethodKey
            .values()
            .toList()
        stored + (origin - stored.map { it.key }.toSet()).map { ArtworkResolveMethod(it, true) }
    }

fun SharedPreferences.setFormatPatternModifiers(modifiers: List<FormatPatternModifier>) {
    edit {
        putString(
            PrefKey.PREF_KEY_FORMAT_PATTERN_MODIFIERS.name,
            json.encodeToString(ListSerializer(FormatPatternModifier.serializer()), modifiers)
        )
    }
}

fun SharedPreferences.getFormatPatternModifiers(): List<FormatPatternModifier> =
    getString(PrefKey.PREF_KEY_FORMAT_PATTERN_MODIFIERS.name, null)?.let { jsonString ->
        val stored = json.parseListOrNull<FormatPatternModifier>(
            jsonString
        ) ?: return@let null
        return@let FormatPattern.replaceablePatterns
            .map { pattern ->
                val modifier = stored.firstOrNull { it.key == pattern }
                FormatPatternModifier(pattern, modifier?.prefix ?: "", modifier?.suffix ?: "")
            }
    } ?: FormatPattern.replaceablePatterns
        .map { FormatPatternModifier(it) }

fun SharedPreferences.getFormatPattern(context: Context): String =
    getString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, null)
        ?: context.getString(R.string.default_sharing_text_pattern)

private fun SharedPreferences.setTempArtworkInfo(artworkUri: Uri?) {
    edit {
        putString(
            PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name,
            json.encodeToString(ArtworkInfo(artworkUri?.toString()))
        )
    }
}

fun SharedPreferences.getTempArtworkInfo(): ArtworkInfo? {
    return if (contains(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name)) {
        json.parseOrNull(
            getString(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name, null)
        )
    } else null
}

fun SharedPreferences.getTempArtworkUri(context: Context): Uri? {
    val uri = getTempArtworkInfo()?.artworkUriString?.getUri() ?: return null

    return withCatching {
        context.contentResolver.openInputStream(uri)?.close() ?: return null
        uri
    }
}

fun SharedPreferences.refreshTempArtwork(artworkUri: Uri?) {
    setTempArtworkInfo(artworkUri)
}

fun SharedPreferences.getSharingText(
    context: Context,
    trackDetail: TrackDetail? = getCurrentTrackDetail()
): String? =
    if (readyForShare(context, trackDetail))
        getFormatPattern(context).getSharingText(
            requireNotNull(trackDetail).toTrackInfo(),
            getFormatPatternModifiers(),
            getSwitchState(PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE)
        )
    else null

fun SharedPreferences.getSharingText(
    context: Context,
    pixelNowPlaying: String
): String? {
    val trackInfo = TrackInfo(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        pixelNowPlaying
    )
    return if (readyForShare(context, trackInfo))
        getFormatPattern(context).getSharingText(
            trackInfo,
            getFormatPatternModifiers(),
            getSwitchState(PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE)
        )
    else null
}

fun SharedPreferences.getCurrentTrackDetail(): TrackDetail? {
    val jsonString =
        if (contains(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name))
            getString(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name, null)
        else null
    json.parseOrNull<TrackDetail>(jsonString)?.apply { return this }

    refreshCurrentTrackDetail(null)
    return null
}

fun SharedPreferences.getChosePaletteColor(): PaletteColor =
    PaletteColor.values().getOrNull(
        getInt(
            PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name,
            PaletteColor.LIGHT_VIBRANT.ordinal
        )
    ) ?: PaletteColor.LIGHT_VIBRANT

fun SharedPreferences.getSwitchState(key: PrefKey): Boolean =
    if (contains(key.name))
        getBoolean(key.name, (key.defaultValue as? Boolean) ?: true)
    else key.defaultValue as? Boolean ?: true

fun SharedPreferences.getDelayDurationPostMastodon(): Long =
    if (contains(PrefKey.PREF_KEY_DELAY_POST_MASTODON.name))
        getLong(
            PrefKey.PREF_KEY_DELAY_POST_MASTODON.name,
            PrefKey.PREF_KEY_DELAY_POST_MASTODON.defaultValue as Long
        )
    else PrefKey.PREF_KEY_DELAY_POST_MASTODON.defaultValue as Long

fun SharedPreferences.getVisibilityMastodon(): Visibility =
    Visibility.values().getOrNull(
        getInt(
            PrefKey.PREF_KEY_CHOSEN_MASTODON_VISIBILITY.name,
            Visibility.PUBLIC.ordinal
        )
    ) ?: Visibility.PUBLIC

fun SharedPreferences.getPackageStateListPostMastodon(): List<PackageState> =
    if (contains(PrefKey.PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON.name))
        getString(PrefKey.PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON.name, null)
            ?.let { json.parseListOrNull(it) }
            ?: emptyList()
    else emptyList()

fun SharedPreferences.storePackageStatePostMastodon(packageName: String, state: Boolean? = null) {
    val toStore = getPackageStateListPostMastodon().let { stateList ->
        val index = stateList.indexOfFirst { it.packageName == packageName }
        if (index > -1)
            stateList.apply { stateList[index].state = state ?: return@apply }
        else stateList + PackageState(packageName)
    }
    edit {
        putString(
            PrefKey.PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON.name,
            json.encodeToString(toStore)
        )
    }
}

fun SharedPreferences.getPackageStateListSpotify(): List<PackageState> =
    (if (contains(PrefKey.PREF_KEY_PACKAGE_LIST_SPOTIFY.name)) {
        val spotifyPackageStates = getString(PrefKey.PREF_KEY_PACKAGE_LIST_SPOTIFY.name, null)
            ?.let { json.parseListOrNull<PackageState>(it) }
            ?: emptyList()
        getPackageStateListPostMastodon().map { mastodonPackageState ->
            mastodonPackageState.copy(
                state = spotifyPackageStates.firstOrNull {
                    it.packageName == mastodonPackageState.packageName
                }?.state ?: true
            )
        }
    } else {
        emptyList()
    }).let { states ->
        states.ifEmpty {
            getPackageStateListPostMastodon().map { it.copy(state = true) }
        }
    }

fun SharedPreferences.storePackageStateSpotify(packageName: String, state: Boolean? = null) {
    val toStore = getPackageStateListSpotify().let { stateList ->
        val index = stateList.indexOfFirst { it.packageName == packageName }
        if (index > -1)
            stateList.apply { stateList[index].state = state ?: return@apply }
        else stateList + PackageState(packageName)
    }
    edit {
        putString(
            PrefKey.PREF_KEY_PACKAGE_LIST_SPOTIFY.name,
            json.encodeToString(toStore)
        )
    }
}

fun SharedPreferences.getPackageStateListAppleMusic(): List<PackageState> =
    (if (contains(PrefKey.PREF_KEY_PACKAGE_LIST_APPLE_MUSIC.name)) {
        val appleMusicPackageStates =
            getString(PrefKey.PREF_KEY_PACKAGE_LIST_APPLE_MUSIC.name, null)
                ?.let { json.parseListOrNull<PackageState>(it) }
                ?: emptyList()
        getPackageStateListPostMastodon().map { mastodonPackageState ->
            mastodonPackageState.copy(
                state = appleMusicPackageStates.firstOrNull {
                    it.packageName == mastodonPackageState.packageName
                }?.state ?: true
            )
        }
    } else {
        emptyList()
    }).let { states ->
        states.ifEmpty {
            getPackageStateListPostMastodon().map { it.copy(state = true) }
        }
    }

fun SharedPreferences.storePackageStateAppleMusic(packageName: String, state: Boolean? = null) {
    val toStore = getPackageStateListAppleMusic().let { stateList ->
        val index = stateList.indexOfFirst { it.packageName == packageName }
        if (index > -1)
            stateList.apply { stateList[index].state = state ?: return@apply }
        else stateList + PackageState(packageName)
    }
    edit {
        putString(
            PrefKey.PREF_KEY_PACKAGE_LIST_APPLE_MUSIC.name,
            json.encodeToString(toStore)
        )
    }
}

fun SharedPreferences.storeDelayDurationPostMastodon(duration: Long) {
    edit { putLong(PrefKey.PREF_KEY_DELAY_POST_MASTODON.name, duration) }
}

fun SharedPreferences.getDonateBillingState(): Boolean =
    contains(PrefKey.PREF_KEY_BILLING_DONATE.name)
            && getBoolean(
        PrefKey.PREF_KEY_BILLING_DONATE.name,
        PrefKey.PREF_KEY_BILLING_DONATE.defaultValue as Boolean
    )

fun SharedPreferences.clearSpotifyUserInfoImmediately() {
    edit(true) { remove(PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name) }
}

fun SharedPreferences.storeSpotifyUserInfoImmediately(spotifyUserInfo: SpotifyUserInfo) {
    edit(true) {
        remove(PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name)
        putString(PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name, json.encodeToString(spotifyUserInfo))
    }
}

fun SharedPreferences.getSpotifyUserInfo(): SpotifyUserInfo? {
    return if (contains(PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name))
        json.parseOrNull(
            getString(
                PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name,
                PrefKey.PREF_KEY_SPOTIFY_USER_INFO.defaultValue as? String
            )
        )
    else null
}

fun SharedPreferences.storeMastodonUserInfo(userInfo: MastodonUserInfo) {
    edit { putString(PrefKey.PREF_KEY_MASTODON_USER_INFO.name, json.encodeToString(userInfo)) }
}

fun SharedPreferences.getMastodonUserInfo(): MastodonUserInfo? {
    return if (contains(PrefKey.PREF_KEY_MASTODON_USER_INFO.name))
        json.parseOrNull(
            getString(
                PrefKey.PREF_KEY_MASTODON_USER_INFO.name,
                PrefKey.PREF_KEY_MASTODON_USER_INFO.defaultValue as? String
            )
        )
    else null
}

fun SharedPreferences.setAlertTwitterAuthFlag(flag: Boolean) {
    edit { putBoolean(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name, flag) }
}

fun SharedPreferences.getAlertTwitterAuthFlag(): Boolean =
    if (contains(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name))
        getBoolean(
            PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name,
            PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.defaultValue as Boolean
        )
    else false

fun SharedPreferences.setReceivedDelegateShareNodeId(nodeId: String?) {
    edit { putString(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name, nodeId) }
}

fun SharedPreferences.getReceivedDelegateShareNodeId(): String? =
    if (contains(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name))
        getString(
            PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name,
            PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.defaultValue as? String
        )
    else null

fun SharedPreferences.getDebugSpotifySearchFlag(): Boolean =
    PrefKey.PREF_KEY_DEBUG_SPOTIFY_SEARCH.let { key ->
        getBoolean(key.name, key.defaultValue as Boolean)
    }

fun SharedPreferences.readyForShare(
    context: Context,
    trackDetail: TrackDetail? = getCurrentTrackDetail()
): Boolean =
    readyForShare(context, trackDetail?.toTrackInfo())

fun SharedPreferences.readyForShare(
    context: Context,
    trackInfo: TrackInfo?
): Boolean =
    trackInfo != null &&
            (getSwitchState(PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE).not() ||
                    (trackInfo.isSatisfiedSpecifier(getFormatPattern(context))))