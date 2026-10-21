package sh.saeed.azkarify

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.services.PrayerConfiguration
import sh.saeed.azkarify.ui.AzkarifyApp

/**
 * Renders the main screens to `app/build/screenshots` for review. Nothing is compared; the
 * images are for looking at the design on a JVM without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun launch(theme: String, language: String, intro: Boolean = false) {
        app.getSharedPreferences("azkarify", 0).edit(commit = true) {
            putBoolean("hasSeenIntro", !intro)
            putBoolean("locationPromptSeen", true)
            putString("theme", theme)
            putString("language", language)
            putString(
                "prayerConfiguration",
                PrayerConfiguration("Cairo", 30.0444, 31.2357, "Africa/Cairo", "EGYPTIAN").encode(),
            )
        }
        val model = AppViewModel(app)
        compose.setContent { AzkarifyApp(model) }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun tour(prefix: String) {
        shot("$prefix-1-today")
        compose.onNodeWithTag("tab.Azkar").performClick()
        shot("$prefix-2-azkar")
        compose.onNodeWithTag("azkarCategoryList").assertExists()
        compose.onNodeWithTag("tab.Counter").performClick()
        shot("$prefix-4-counter")
        compose.onNodeWithTag("tab.Settings").performClick()
        shot("$prefix-5-settings")
    }

    @Test fun day() {
        launch("light", "en")
        tour("day-en")
    }

    @Test fun night() {
        launch("dark", "en")
        tour("night-en")
    }

    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi") fun arabic() {
        launch("light", "ar")
        tour("day-ar")
    }

    @Test fun reader() {
        launch("light", "en")
        compose.onNodeWithTag("tab.Azkar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Morning & Evening").performClick()
        shot("day-en-3-reader")
    }

    @Test fun intro() {
        launch("light", "en", intro = true)
        shot("day-en-0-intro")
    }
}
