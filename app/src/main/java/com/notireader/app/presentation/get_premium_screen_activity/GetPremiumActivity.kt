package com.notireader.app.presentation.get_premium_screen_activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.*
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.common.collect.ImmutableList
import com.notireader.app.databinding.ActivityGetPremiumBinding
import com.notireader.app.presentation.main_screen_activity.MainActivity
import com.notireader.app.util.PremiumManager
import kotlin.collections.firstOrNull
import kotlin.collections.isNullOrEmpty


class GetPremiumActivity : AppCompatActivity(), PurchasesUpdatedListener {
    private lateinit var billingClient: BillingClient
    private var premiumProductDetails: ProductDetails? = null
    private var selectedOfferToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PremiumManager.isPremiumUnlocked(this)) {
            finish()
            return
        }
        enableEdgeToEdge()
        val binding = ActivityGetPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.crossButton.visibility = View.INVISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.crossButton.visibility = View.VISIBLE
        }, 1000)
        binding.crossButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Query product details and set offer token
        fun queryPremiumProduct() {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("premium_subscription") // TODO: replace with your product ID
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                ).build()
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetails = productDetailsList.productDetailsList.firstOrNull()
                    premiumProductDetails = productDetails
                    // For one-time products:
                    selectedOfferToken = productDetails?.oneTimePurchaseOfferDetails?.offerToken
                    // For subscriptions, use:
                    // selectedOfferToken = productDetails?.subscriptionOfferDetails?.get(0)?.offerToken
                }
            }
        }

        // BillingClient setup
        val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
        }
        billingClient = BillingClient.newBuilder(applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPremiumProduct()
                }
            }

            override fun onBillingServiceDisconnected() {
                // TODO: try calling start connection method
            }
        })

        binding.getPremiumBtn.setOnClickListener {
            if (premiumProductDetails == null || selectedOfferToken.isNullOrEmpty()) {
                Toast.makeText(this, "Product details not loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val productDetailsParamsList = listOf(
                ProductDetailsParams.newBuilder()
                    .setProductDetails(premiumProductDetails!!)
                    .setOfferToken(selectedOfferToken ?: "")
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient.launchBillingFlow(this, billingFlowParams)
        }
    }


    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    val acknowledgeParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgeParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            PremiumManager.setPremiumUnlocked(this, true)
                        }
                    }
                }
            }
        }
    }
}