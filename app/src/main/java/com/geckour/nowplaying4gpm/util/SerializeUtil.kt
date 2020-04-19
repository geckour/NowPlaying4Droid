package com.geckour.nowplaying4gpm.util

import com.sys1yagi.mastodon4j.api.entity.auth.AccessToken
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PrimitiveDescriptor
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encode
import kotlinx.serialization.parse
import kotlinx.serialization.stringify

@OptIn(ImplicitReflectionSerializer::class)
@Serializer(AccessToken::class)
object MastodonAccessTokenSerializer : KSerializer<AccessToken> {

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor(AccessToken::class.java.name, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AccessToken) {
        encoder.encode(json.stringify(MastodonAccessToken.from(value)))
    }

    override fun deserialize(decoder: Decoder): AccessToken =
        json.parse<MastodonAccessToken>(decoder.decodeString()).toAccessToken()

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