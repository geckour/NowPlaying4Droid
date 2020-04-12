package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SkuDetail(
    val productId: String,

    val type: String,

    val price: String,

    @Json(name = "price_amount_micros")
    val priceInMicros: String,

    @Json(name = "price_currency_code")
    val priceCode: String,

    val title: String,

    val description: String
)