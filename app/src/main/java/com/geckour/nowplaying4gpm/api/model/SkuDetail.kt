package com.geckour.nowplaying4gpm.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkuDetail(
    val productId: String,

    val type: String,

    val price: String,

    @SerialName("price_amount_micros")
    val priceInMicros: String,

    @SerialName("price_currency_code")
    val priceCode: String,

    val title: String,

    val description: String
)