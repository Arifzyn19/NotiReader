package com.notireader.app.presentation.get_premium_screen_activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.google.common.collect.ImmutableList
import com.notireader.app.databinding.ActivityGetPremiumBinding
import com.notireader.app.presentation.main_screen_activity.MainActivity
import com.notireader.app.util.PremiumManager

class GetPremiumActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private var premiumProductDetails: ProductDetails? = null
    val TAG = "MyTag"

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@GetPremiumActivity, MainActivity::class.java))
                finish()
            }
        })

        billingClient = BillingClient.newBuilder(applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished-responseCode: ${billingResult.responseCode}")
                Log.d(TAG, "onBillingSetupFinished-debugMessage: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPremiumProduct()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected: ")
            }
        })

        binding.getPremiumBtn.setOnClickListener {
            val product = premiumProductDetails
            if (product == null) {
                Toast.makeText(this, "Product details not loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "onCreate-subscriptionOfferDetails: ${product.subscriptionOfferDetails}")

            val offerToken = product.subscriptionOfferDetails
                ?.firstOrNull()  // take first offer
                ?.offerToken

            Log.d(TAG, "onCreate: $offerToken")

            if (offerToken.isNullOrEmpty()) {
                Toast.makeText(this, "No subscription offers available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offerToken)
                .build()

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            val result = billingClient.launchBillingFlow(this, billingFlowParams)
            Log.d(TAG, "launchBillingFlow result: $result")
        }

    }

    fun queryPremiumProduct() {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("purchase_349")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()))
                .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            Log.d(TAG, "queryPremiumProduct-responseCode: ${billingResult.responseCode}")
            Log.d(TAG, "queryPremiumProduct-debugMessage: ${billingResult.debugMessage}")
            Log.d(TAG, "queryPremiumProduct-productDetailsList: ${productDetailsList.productDetailsList}")
            Log.d(TAG, "queryPremiumProduct-unfetchedProductList: ${productDetailsList.unfetchedProductList}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                premiumProductDetails = productDetailsList.productDetailsList.firstOrNull()
                Log.d(TAG, "queryPremiumProduct-premiumProductDetails: $premiumProductDetails")
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        Log.d(TAG, "onPurchasesUpdated: $billingResult")
        Log.d(TAG, "onPurchasesUpdated: $purchases")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    val acknowledgeParams =
                        com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
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