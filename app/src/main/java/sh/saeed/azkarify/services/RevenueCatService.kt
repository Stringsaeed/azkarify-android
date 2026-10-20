package sh.saeed.azkarify.services

import android.content.Context
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import sh.saeed.azkarify.BuildConfig

object RevenueCatService {
    fun configure(context: Context) {
        if (!Purchases.isConfigured && BuildConfig.REVENUECAT_API_KEY.startsWith("goog_")) {
            Purchases.configure(
                PurchasesConfiguration.Builder(
                        context.applicationContext,
                        BuildConfig.REVENUECAT_API_KEY,
                    )
                    .build()
            )
        }
    }

    val isConfigured
        get() = Purchases.isConfigured
}
