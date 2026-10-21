package sh.saeed.azkarify

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.ui.AzkarifyApp

/** The Azkar search hides while the list scrolls down and returns on any scroll up. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class AzkarSearchTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun searchHidesOnScrollDownAndReturnsOnScrollUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("azkarify", 0).edit(commit = true) {
            putBoolean("hasSeenIntro", true)
            putBoolean("locationPromptSeen", true)
            putString("language", "en")
        }
        val model = AppViewModel(app)
        compose.setContent { AzkarifyApp(model) }
        compose.onNodeWithTag("tab.Azkar").performClick()
        compose.waitUntil(10_000) { model.state.value.categories.isNotEmpty() }
        val search = compose.onNodeWithContentDescription("Search azkar")
        search.assertIsDisplayed()
        compose.onNodeWithTag("azkarCategoryList").performTouchInput { swipeUp() }
        compose.waitForIdle()
        search.assertIsNotDisplayed()
        compose.onNodeWithTag("azkarCategoryList").performTouchInput { swipeDown(startY = centerY, endY = centerY + 300f) }
        compose.waitForIdle()
        search.assertIsDisplayed()
    }
}
