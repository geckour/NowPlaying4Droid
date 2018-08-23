package com.geckour.nowplaying4gpm.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.ArtworkInfo
import com.geckour.nowplaying4gpm.domain.model.MastodonUserInfo
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.google.gson.Gson
import timber.log.Timber
import twitter4j.auth.AccessToken

enum class PrefKey(val defaultValue: Any? = null) {
    PREF_KEY_PATTERN_FORMAT_SHARE_TEXT("#NowPlaying TI - AR (AL)"),
    PREF_KEY_CHOSEN_PALETTE_COLOR(PaletteColor.LIGHT_VIBRANT.ordinal),
    PREF_KEY_WHETHER_RESIDE(true),
    PREF_KEY_WHETHER_USE_API(false),
    PREF_KEY_WHETHER_BUNDLE_ARTWORK(true),
    PREF_KEY_WHETHER_ENABLE_AUTO_POST_MASTODON(false),
    PREF_KEY_DELAY_POST_MASTODON(2000L),
    PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG(true),
    PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET(true),
    PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK(true),
    PREF_KEY_CURRENT_TRACK_INFO,
    PREF_KEY_TEMP_ARTWORK_INFO,
    PREF_KEY_BILLING_DONATE(false),
    PREF_KEY_TWITTER_ACCESS_TOKEN,
    PREF_KEY_MASTODON_USER_INFO,
    PREF_KEY_FLAG_ALERT_AUTH_TWITTER(false),
    PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE
}

fun SharedPreferences.refreshCurrentTrackInfo(trackInfo: TrackInfo) =
        edit().apply {
            putString(
                    PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name,
                    Gson().toJson(trackInfo))
        }.apply()

fun SharedPreferences.getFormatPattern(context: Context): String =
        getString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, context.getString(R.string.default_sharing_text_pattern))

private fun SharedPreferences.setTempArtworkInfo(artworkUri: Uri?) {
    edit().putString(
            PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name,
            Gson().toJson(ArtworkInfo(artworkUri?.toString()))).apply()
}

fun SharedPreferences.getTempArtworkInfo(): ArtworkInfo? =
        if (contains(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name)) {
            Gson().fromJsonOrNull(
                    getString(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name, null),
                    ArtworkInfo::class.java)
        } else null

fun SharedPreferences.getTempArtworkUri(context: Context): Uri? {
    val uri = getTempArtworkInfo()?.artworkUriString?.getUri() ?: return null

    return try {
        context.contentResolver.openInputStream(uri).close()
        uri
    } catch (e: Throwable) {
        Timber.e(e)
        null
    }
}

fun SharedPreferences.refreshTempArtwork(artworkUri: Uri?) {
    setTempArtworkInfo(artworkUri)
}

fun SharedPreferences.getSharingText(context: Context): String? {
    val trackInfo = getCurrentTrackInfo() ?: return null

    if (trackInfo.coreElement.isAllNonNull.not()) return null
    return getFormatPattern(context).getSharingText(trackInfo.coreElement)
}

fun SharedPreferences.getCurrentTrackInfo(): TrackInfo? =
        if (contains(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name)) {
            Gson().fromJsonOrNull(
                    getString(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name,
                            PrefKey.PREF_KEY_CURRENT_TRACK_INFO.defaultValue as? String),
                    TrackInfo::class.java) {
                refreshCurrentTrackInfo(TrackInfo.empty)
            }
        } else null

fun SharedPreferences.getChosePaletteColor(): PaletteColor =
        PaletteColor.values().getOrNull(
                getInt(
                        PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name,
                        PaletteColor.LIGHT_VIBRANT.ordinal)
        ) ?: PaletteColor.LIGHT_VIBRANT

fun SharedPreferences.getSwitchState(key: PrefKey): Boolean =
        if (contains(key.name))
            getBoolean(key.name, (key.defaultValue as? Boolean) ?: true)
        else key.defaultValue as? Boolean ?: true

fun SharedPreferences.getDelayDurationPostMastodon(): Long =
        if (contains(PrefKey.PREF_KEY_DELAY_POST_MASTODON.name))
            getLong(PrefKey.PREF_KEY_DELAY_POST_MASTODON.name,
                    PrefKey.PREF_KEY_DELAY_POST_MASTODON.defaultValue as Long)
        else PrefKey.PREF_KEY_DELAY_POST_MASTODON.defaultValue as Long

fun SharedPreferences.storeDelayDurationPostMastodon(duration: Long) {
    edit().putLong(PrefKey.PREF_KEY_DELAY_POST_MASTODON.name, duration).apply()
}

fun SharedPreferences.getDonateBillingState(): Boolean =
        contains(PrefKey.PREF_KEY_BILLING_DONATE.name)
                && getBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name,
                PrefKey.PREF_KEY_BILLING_DONATE.defaultValue as Boolean)

fun SharedPreferences.storeTwitterAccessToken(accessToken: AccessToken) {
    edit().putString(PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name, Gson().toJson(accessToken)).apply()
}

fun SharedPreferences.getTwitterAccessToken(): AccessToken? =
        if (contains(PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name))
            Gson().fromJsonOrNull(
                    getString(PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name,
                            PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.defaultValue as? String),
                    AccessToken::class.java)
        else null

fun SharedPreferences.storeMastodonUserInfo(userInfo: MastodonUserInfo) {
    edit().putString(PrefKey.PREF_KEY_MASTODON_USER_INFO.name, Gson().toJson(userInfo))
            .apply()
}

fun SharedPreferences.getMastodonUserInfo(): MastodonUserInfo? =
        if (contains(PrefKey.PREF_KEY_MASTODON_USER_INFO.name))
            Gson().fromJsonOrNull(
                    getString(PrefKey.PREF_KEY_MASTODON_USER_INFO.name,
                            PrefKey.PREF_KEY_MASTODON_USER_INFO.defaultValue as? String),
                    MastodonUserInfo::class.java)
        else null

fun SharedPreferences.setAlertTwitterAuthFlag(flag: Boolean) {
    edit().putBoolean(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name, flag).apply()
}

fun SharedPreferences.getAlertTwitterAuthFlag(): Boolean =
        if (contains(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name))
            getBoolean(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name,
                    PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.defaultValue as Boolean)
        else false

fun SharedPreferences.setReceivedDelegateShareNodeId(nodeId: String?) {
    edit().putString(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name, nodeId).apply()
}

fun SharedPreferences.getReceivedDelegateShareNodeId(): String? =
        if (contains(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name))
            getString(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name,
                    PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.defaultValue as? String)
        else null