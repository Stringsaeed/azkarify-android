package sh.saeed.azkarify

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
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
 * Renders the main screens to `app/build/screenshots` for review and for the store listing frames.
 * Nothing is compared; the images are for looking at the design on a JVM without an emulator.
 *
 * Phone shots use a 411×891dp xxhdpi window (1233×2673 px). Tablet shots use 720×1280dp xhdpi
 * (1440×2560 px), the Play Store's 9:16 tablet size.
 */
private const val PHONE = "w411dp-h891dp-xxhdpi"
private const val TABLET = "w720dp-h1280dp-xhdpi"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE)
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
                PrayerConfiguration(
                    "Dubai", 25.2048, 55.2708, "Asia/Dubai", "DUBAI",
                    englishLocationName = "Dubai", arabicLocationName = "دبي",
                ).encode(),
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

    /** The store frames: journey detail, today scrolled to the prayer journeys, the reader and a counter at 21. */
    private fun storeTour(prefix: String, language: String) {
        val en = language == "en"
        compose.onNodeWithTag("todayList")
            .performScrollToNode(hasText(if (en) "Isha journey" else "رحلة العشاء"))
        // Scrolling back up to the section title parks it at the top of the list.
        compose.onNodeWithTag("todayList")
            .performScrollToNode(hasText(if (en) "Your prayer journeys" else "رحلات الصلاة"))
        shot("$prefix-5-today-scrolled")
        compose.onNodeWithTag("todayList").performScrollToIndex(0)
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onNodeWithText(if (en) "A gentle morning" else "بداية صباحك").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("journeySteps").assertExists()
        shot("$prefix-2-journey")
        compose.onNodeWithTag("tab.Azkar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(if (en) "Morning & Evening" else "الصباح والمساء").performClick()
        compose.waitForIdle()
        shot("$prefix-3-reader")
        compose.onNodeWithTag("tab.Counter").performClick()
        compose.waitForIdle()
        val dial = hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)
        repeat(21) { compose.onNode(dial).performClick() }
        shot("$prefix-4-counter-21")
    }

    @Test fun day() {
        launch("light", "en")
        tour("day-en")
    }

    @Test fun dayStore() {
        launch("light", "en")
        storeTour("day-en", "en")
    }

    @Test fun night() {
        launch("dark", "en")
        tour("night-en")
    }

    @Test @Config(qualifiers = "ar-$PHONE") fun arabic() {
        launch("light", "ar")
        tour("day-ar")
    }

    @Test @Config(qualifiers = "ar-$PHONE") fun arabicStore() {
        launch("light", "ar")
        storeTour("day-ar", "ar")
    }

    @Test @Config(qualifiers = "ar-$PHONE") fun arabicNight() {
        launch("dark", "ar")
        shot("night-ar-1-today")
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

    @Test @Config(qualifiers = TABLET) fun tabletDay() {
        launch("light", "en")
        tour("tab-day-en")
    }

    @Test @Config(qualifiers = TABLET) fun tabletDayStore() {
        launch("light", "en")
        storeTour("tab-day-en", "en")
    }

    @Test @Config(qualifiers = TABLET) fun tabletNight() {
        launch("dark", "en")
        shot("tab-night-en-1-today")
    }

    @Test @Config(qualifiers = "ar-$TABLET") fun tabletArabic() {
        launch("light", "ar")
        tour("tab-day-ar")
    }

    @Test @Config(qualifiers = "ar-$TABLET") fun tabletArabicStore() {
        launch("light", "ar")
        storeTour("tab-day-ar", "ar")
    }

    @Test @Config(qualifiers = "ar-$TABLET") fun tabletArabicNight() {
        launch("dark", "ar")
        shot("tab-night-ar-1-today")
    }
}
