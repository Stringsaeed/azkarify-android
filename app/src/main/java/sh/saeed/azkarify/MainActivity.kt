package sh.saeed.azkarify

import android.app.LocaleManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.data.applyAppLanguage
import sh.saeed.azkarify.services.RevenueCatService
import sh.saeed.azkarify.ui.AzkarifyApp

class MainActivity : ComponentActivity() {
    private val model by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            Build.VERSION.SDK_INT >= 33 &&
                getSystemService(LocaleManager::class.java).applicationLocales.isEmpty
        ) {
            val saved = getSharedPreferences("azkarify", 0).all["language"] as? String
            if (saved == "en" || saved == "ar") applyAppLanguage(saved)
        }
        RevenueCatService.configure(applicationContext)
        enableEdgeToEdge()
        setContent { AzkarifyApp(model) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        model.refreshLanguage()
    }
}
