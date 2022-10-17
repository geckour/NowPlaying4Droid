package com.geckour.nowplaying4gpm.app.api

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.geckour.nowplaying4gpm.app.util.withCatching

class BillingApiClient(
    context: Context,
    private val onError: () -> Unit,
    private val onDonateCompleted: (result: com.geckour.nowplaying4gpm.app.api.BillingApiClient.BillingResult) -> Unit
) : PurchasesUpdatedListener {

    enum class BillingResult {
        SUCCESS,
        DUPLICATED,
        CANCELLED,
        FAILURE
    }

    private val client: BillingClient =
        BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

    init {
        client.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) =
                Unit

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    override fun onPurchasesUpdated(
        result: com.android.billingclient.api.BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val billingResult = when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases?.isEmpty() == false) {
                    if (purchases.none { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                        com.geckour.nowplaying4gpm.app.api.BillingApiClient.BillingResult.SUCCESS
                    } else com.geckour.nowplaying4gpm.app.api.BillingApiClient.BillingResult.DUPLICATED
                } else com.geckour.nowplaying4gpm.app.api.BillingApiClient.BillingResult.FAILURE
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                com.geckour.nowplaying4gpm.app.api.BillingApiClient.BillingResult.CANCELLED
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                com.geckour.nowplaying4gpm.app.api.BillingApiClient.BillingResult.DUPLICATED
            }
            else -> {
                com.geckour.nowplaying4gpm.app.api.BillingApiClient.BillingResult.FAILURE
            }
        }
        onDonateCompleted(billingResult)
    }

    fun startBilling(activity: AppCompatActivity, skus: List<String>) {
        withCatching {
            val params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.INAPP)
                .setSkusList(skus)
                .build()
            client.querySkuDetailsAsync(params) { result, skuDetailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    onError()
                    return@querySkuDetailsAsync
                }

                skuDetailsList?.firstOrNull()?.let {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetailsList.first())
                        .build()
                    client.launchBillingFlow(activity, flowParams)
                } ?: run {
                    onError()
                }
            }
        }
    }

    fun requestUpdate() {
        withCatching {
            client.queryPurchasesAsync(BillingClient.SkuType.INAPP) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.forEach {
                        if (it.purchaseState != Purchase.PurchaseState.PURCHASED) return@forEach
                        if (it.isAcknowledged) return@forEach

                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(it.purchaseToken)
                            .build()
                        client.acknowledgePurchase(params) {}
                    }
                }
            }
        }
    }
}