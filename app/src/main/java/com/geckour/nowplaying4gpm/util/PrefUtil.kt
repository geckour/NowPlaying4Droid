package com.geckour.nowplaying4gpm.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.ArtworkInfo
import com.geckour.nowplaying4gpm.domain.model.MastodonUserInfo
import com.geckour.nowplaying4gpm.domain.model.PackageState
import com.geckour.nowplaying4gpm.domain.model.SpotifyUserInfo
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.stringify
import twitter4j.auth.AccessToken

enum class PrefKey(val defaultValue: Any? = null) {
    PREF_KEY_WHETHER_USE_API(false),
    PREF_KEY_ARTWORK_RESOLVE_ORDER,
    PREF_KEY_PATTERN_FORMAT_SHARE_TEXT("#NowPlaying TI - AR (AL)"),
    PREF_KEY_FORMAT_PATTERN_MODIFIERS,
    PREF_KEY_STRICT_MATCH_PATTERN_MODE(false),
    PREF_KEY_WHETHER_BUNDLE_ARTWORK(true),
    PREF_KEY_WHETHER_COPY_INTO_CLIPBOARD(false),
    PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON(false),
    PREF_KEY_DELAY_POST_MASTODON(2000L),
    PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON(emptyList<String>()),
    PREF_KEY_SHOW_SUCCESS_NOTIFICATION_MASTODON(false),
    PREF_KEY_WHETHER_RESIDE(true),
    PREF_KEY_WHETHER_SHOW_ARTWORK_IN_NOTIFICATION(true),
    PREF_KEY_CHOSEN_PALETTE_COLOR(PaletteColor.LIGHT_VIBRANT.ordinal),
    PREF_KEY_CHOSEN_MASTODON_VISIBILITY(Visibility.PUBLIC.ordinal),
    PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG(true),
    PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET(true),
    PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK(true),
    PREF_KEY_WHETHER_SHOW_CLEAR_BUTTON_IN_WIDGET(false),
    PREF_KEY_CURRENT_TRACK_INFO,
    PREF_KEY_TEMP_ARTWORK_INFO,
    PREF_KEY_BILLING_DONATE(false),
    PREF_KEY_SPOTIFY_USER_INFO,
    PREF_KEY_TWITTER_ACCESS_TOKEN,
    PREF_KEY_MASTODON_USER_INFO,
    PREF_KEY_FLAG_ALERT_AUTH_TWITTER(false),
    PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE,
    PREF_KEY_DEBUG_SPOTIFY_SEARCH(false),
    PREF_KEY_DENIED_IGNORE_BATTERY_OPTIMIZATION(false)
}

@Serializable
data class ArtworkResolveMethod(
    val key: ArtworkResolveMethodKey,
    val enabled: Boolean
) {
    enum class ArtworkResolveMethodKey(val strResId: Int) {
        CONTENT_RESOLVER(R.string.dialog_list_item_content_resolver),
        MEDIA_METADATA_URI(R.string.dialog_list_item_media_metadata_uri),
        MEDIA_METADATA_BITMAP(R.string.dialog_list_item_media_metadata_bitmap),
        NOTIFICATION_BITMAP(R.string.dialog_list_item_notification_bitmap),
        LAST_FM(R.string.dialog_list_item_last_fm)
    }
}

@Serializable
data class FormatPatternModifier(
    val key: FormatPattern,
    val prefix: String? = null,
    val suffix: String? = null
)

@Serializable
data class PlayerPackageState(
    val packageName: String,
    val appName: String,
    val state: Boolean
)

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.refreshCurrentTrackInfo(trackInfo: TrackInfo?) =
    edit().putString(
        PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name,
        trackInfo?.let { json.stringify(it) }
    ).apply()

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.setArtworkResolveOrder(order: List<ArtworkResolveMethod>) =
    edit().putString(
        PrefKey.PREF_KEY_ARTWORK_RESOLVE_ORDER.name,
        json.stringify(order)
    ).apply()

fun SharedPreferences.getArtworkResolveOrder(): List<ArtworkResolveMethod> =
    getString(PrefKey.PREF_KEY_ARTWORK_RESOLVE_ORDER.name, null)?.let {
        json.parseListOrNull<ArtworkResolveMethod>(it)
    } ?: ArtworkResolveMethod.ArtworkResolveMethodKey
        .values()
        .map { ArtworkResolveMethod(it, true) }

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.setFormatPatternModifiers(modifiers: List<FormatPatternModifier>) =
    edit().putString(
        PrefKey.PREF_KEY_FORMAT_PATTERN_MODIFIERS.name,
        json.stringify(FormatPatternModifier.serializer().list, modifiers)
    ).apply()

fun SharedPreferences.getFormatPatternModifiers(): List<FormatPatternModifier> =
    getString(PrefKey.PREF_KEY_FORMAT_PATTERN_MODIFIERS.name, null)?.let { jsonString ->
        val stored = json.parseListOrNull<FormatPatternModifier>(
            jsonString
        ) ?: return@let null
        return@let FormatPattern.replaceablePatterns
            .map { pattern ->
                val modifier = stored.firstOrNull { it.key == pattern }
                FormatPatternModifier(pattern, modifier?.prefix, modifier?.suffix)
            }
    } ?: FormatPattern.replaceablePatterns
        .map { FormatPatternModifier(it) }

fun SharedPreferences.getFormatPattern(context: Context): String =
    getString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, null)
        ?: context.getString(R.string.default_sharing_text_pattern)

@OptIn(ImplicitReflectionSerializer::class)
private fun SharedPreferences.setTempArtworkInfo(artworkUri: Uri?) {
    edit().putString(
        PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name,
        json.stringify(ArtworkInfo(artworkUri?.toString()))
    ).apply()
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
    trackInfo: TrackInfo? = getCurrentTrackInfo()
): String? =
    if (readyForShare(context, trackInfo))
        getFormatPattern(context).getSharingText(
            requireNotNull(trackInfo),
            getFormatPatternModifiers()
        )
    else null

fun SharedPreferences.getCurrentTrackInfo(): TrackInfo? {
    val jsonString =
        if (contains(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name))
            getString(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name, null)
        else null
    json.parseOrNull<TrackInfo>(jsonString)?.apply { return this }

    refreshCurrentTrackInfo(null)
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

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.getPackageStateListPostMastodon(): List<PackageState> =
    if (contains(PrefKey.PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON.name))
        getString(PrefKey.PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON.name, null)
            ?.let { json.parseListOrNull<PackageState>(it) }
            ?: emptyList()
    else emptyList()

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.storePackageStatePostMastodon(packageName: String, state: Boolean? = null) {
    val toStore = getPackageStateListPostMastodon().let { stateList ->
        val index = stateList.indexOfFirst { it.packageName == packageName }
        if (index > -1)
            stateList.apply { stateList[index].state = state ?: return@apply }
        else stateList + PackageState(packageName)
    }
    edit().putString(
        PrefKey.PREF_KEY_PACKAGE_LIST_AUTO_POST_MASTODON.name,
        json.stringify(toStore)
    ).apply()
}

fun SharedPreferences.storeDelayDurationPostMastodon(duration: Long) {
    edit().putLong(PrefKey.PREF_KEY_DELAY_POST_MASTODON.name, duration).apply()
}

fun SharedPreferences.getDonateBillingState(): Boolean =
    contains(PrefKey.PREF_KEY_BILLING_DONATE.name)
            && getBoolean(
        PrefKey.PREF_KEY_BILLING_DONATE.name,
        PrefKey.PREF_KEY_BILLING_DONATE.defaultValue as Boolean
    )

fun SharedPreferences.cleaerSpotifyUserInfoImmediately() {
    edit().remove(PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name).commit()
}

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.storeSpotifyUserInfoImmediately(spotifyUserInfo: SpotifyUserInfo) {
    edit().remove(PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name)
        .putString(
            PrefKey.PREF_KEY_SPOTIFY_USER_INFO.name,
            json.stringify(spotifyUserInfo)
        ).commit()
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

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.storeTwitterAccessToken(accessToken: AccessToken) {
    edit().putString(
        PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name,
        accessToken.asString()
    ).apply()
}

fun SharedPreferences.getTwitterAccessToken(): AccessToken? {
    return if (contains(PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name))
        getString(
            PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name,
            PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.defaultValue as? String
        )?.let { it.toSerializableObject<AccessToken>() }
    else null
}

@OptIn(ImplicitReflectionSerializer::class)
fun SharedPreferences.storeMastodonUserInfo(userInfo: MastodonUserInfo) {
    edit().putString(
        PrefKey.PREF_KEY_MASTODON_USER_INFO.name,
        json.stringify(userInfo)
    ).apply()
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
    edit().putBoolean(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name, flag).apply()
}

fun SharedPreferences.getAlertTwitterAuthFlag(): Boolean =
    if (contains(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name))
        getBoolean(
            PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name,
            PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.defaultValue as Boolean
        )
    else false

fun SharedPreferences.setReceivedDelegateShareNodeId(nodeId: String?) {
    edit().putString(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name, nodeId).apply()
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

fun SharedPreferences.toggleDebugSpotifySearchFlag(): Boolean {
    val key = PrefKey.PREF_KEY_DEBUG_SPOTIFY_SEARCH

    val setTo = getDebugSpotifySearchFlag().not()
    edit().putBoolean(key.name, setTo).apply()

    return setTo
}

fun SharedPreferences.readyForShare(
    context: Context,
    trackInfo: TrackInfo? = getCurrentTrackInfo()
): Boolean {
    return trackInfo != null &&
            (getSwitchState(PrefKey.PREF_KEY_STRICT_MATCH_PATTERN_MODE).not() ||
                    trackInfo.isSatisfiedSpecifier(getFormatPattern(context)))
}