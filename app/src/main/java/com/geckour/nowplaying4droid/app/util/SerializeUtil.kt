package com.geckour.nowplaying4droid.app.util

import com.sys1yagi.mastodon4j.api.entity.auth.AccessToken
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor

@OptIn(ExperimentalSerializationApi::class)
@Serializer(AccessToken::class)
object MastodonAccessTokenSerializer : KSerializer<AccessToken> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(AccessToken::class.java.name, PrimitiveKind.STRING)

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: AccessToken) {
        encoder.encodeString(
            json.encodeToString(
                MastodonAccessToken.serializer(),
                MastodonAccessToken.from(value)
            )
        )
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): AccessToken =
        json.decodeFromString<MastodonAccessToken>(decoder.decodeString()).toAccessToken()

    @Serializable
    data class MastodonAccessToken(
        @SerialName("access_token")
        var accessToken: String = "",

        @SerialName("token_type")
        var tokenType: String = "",

        @SerialName("scope")
        var scope: String = "",

        @SerialName("created_at")
        var createdAt: Long = 0L
    ) {

        companion object {

            fun from(accessToken: AccessToken): MastodonAccessToken =
                MastodonAccessToken(
                    accessToken.accessToken,
                    accessToken.tokenType,
                    accessToken.scope,
                    accessToken.createdAt
                )
        }

        fun toAccessToken(): AccessToken = AccessToken(accessToken, tokenType, scope, createdAt)
    }
}