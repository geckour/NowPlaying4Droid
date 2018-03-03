package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class SkuDetail(
        val productId: String,

        val type: String,

        val price: String,

        @SerializedName("price_amount_micros")
        val priceInMicros: String,

        @SerializedName("price_currency_code")
        val priceCode: String,

        val title: String,

        val description: String
)