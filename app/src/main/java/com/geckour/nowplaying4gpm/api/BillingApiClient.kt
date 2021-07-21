package com.geckour.nowplaying4gpm.api

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.util.showErrorDialog
import com.geckour.nowplaying4gpm.util.withCatching
import kotlinx.coroutines.launch

class BillingApiClient(
    context: Context,
    private val onDonateCompleted: (result: BillingResult) -> Unit
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
                        BillingResult.SUCCESS
                    } else BillingResult.DUPLICATED
                } else BillingResult.FAILURE
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                BillingResult.CANCELLED
            }
            else -> {
                BillingResult.FAILURE
            }
        }
        onDonateCompleted(billingResult)
    }

    suspend fun startBilling(activity: AppCompatActivity, skus: List<String>) {
        withCatching {
            val params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.INAPP)
                .setSkusList(skus)
                .build()
            client.querySkuDetailsAsync(params) { result, skuDetailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    activity.lifecycleScope.launch {
                        activity.showErrorDialog(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_on_start_purchase
                        )
                    }
                    return@querySkuDetailsAsync
                }

                skuDetailsList?.firstOrNull()?.let {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetailsList.first())
                        .build()
                    client.launchBillingFlow(activity, flowParams)
                } ?: run {
                    activity.lifecycleScope.launch {
                        activity.showErrorDialog(
                            R.string.dialog_title_alert_failure_purchase,
                            R.string.dialog_message_alert_on_start_purchase
                        )
                    }
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