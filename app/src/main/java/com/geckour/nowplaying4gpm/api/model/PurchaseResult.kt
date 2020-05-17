package com.geckour.nowplaying4gpm.api.model

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseResult(
    val autoRenewing: Boolean,
    val orderId: String,
    val packageName: String,
    val productId: String,
    val purchaseTime: Long,
    val purchaseState: Int,
    val developerPayload: String?,
    val purchaseToken: String
)