package sh.saeed.azkarify

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.data.applyAppLanguage
import sh.saeed.azkarify.data.journeyDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

class AppFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val reset = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .getSharedPreferences("azkarify", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .putBoolean("hasSeenIntro", true)
                    .putBoolean("locationPromptSeen", true)
                    .putString("language", "en")
                    .commit()
                InstrumentationRegistry.getInstrumentation().targetContext.applyAppLanguage("en")
                runBlocking(Dispatchers.IO) {
                    InstrumentationRegistry.getInstrumentation()
                        .targetContext
                        .journeyDataStore
                        .edit { it.clear() }
                }
                try {
                    base.evaluate()
                } catch (error: Throwable) {
                    runCatching {
                        compose
                            .onAllNodes(isRoot(), useUnmergedTree = true)
                            .onFirst()
                            .printToLog("AzkarifyTestFailure")
                    }
                    throw error
                }
            }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(reset).around(compose)

    private fun awaitTopics() {
        try {
            compose.waitUntil(10_000) {
                compose
                    .onAllNodesWithText("Words of remembrance for morning and evening")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        } catch (error: Throwable) {
            compose.runOnIdle {
                val state =
                    ViewModelProvider(compose.activity)[AppViewModel::class.java].state.value
                Log.e(
                    "AzkarifyStartup",
                    "language=${state.language} intro=${state.introSeen} prompt=${state.locationPromptSeen} categories=${state.categories.take(3)} error=${state.error}",
                )
            }
            compose
                .onAllNodes(isRoot(), useUnmergedTree = true)
                .onFirst()
                .printToLog("AzkarifyStartup")
            throw error
        }
    }

    @Test
    fun favoritesSearchReadingCounterAndArabicSettingsWork() {
        awaitTopics()
        compose
            .onNodeWithTag("azkarCategoryList")
            .performScrollToNode(hasContentDescription("Add to favorites"))
        compose.onAllNodesWithContentDescription("Add to favorites").onFirst().performClick()
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Favorites").performClick()
        compose
            .onNodeWithText("Words of remembrance for morning and evening")
            .assertIsDisplayed()
            .performClick()
        compose.onNodeWithTag("azkarEntriesList").performScrollToNode(hasText("📿  3"))
        compose.onAllNodesWithText("📿  3").onFirst().performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Count: 3").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Count: 3").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Count: 2").assertIsDisplayed()
        compose.onNodeWithText("Reset").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Count: 3").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Count: 3").performScrollTo()
        compose.waitUntil(5_000) { compose.onNodeWithContentDescription("Count: 3").isDisplayed() }
        compose.onNodeWithContentDescription("Count: 3").assertIsDisplayed()
    }

    @Test
    fun searchOnlyFocusesAfterATapAndReleasesFocusOnSubmit() {
        awaitTopics()
        compose.onNodeWithContentDescription("Search azkar").assertIsNotFocused()
        compose.activityRule.scenario.recreate()
        awaitTopics()
        compose.onNodeWithContentDescription("Search azkar").assertIsNotFocused()
        compose.onNodeWithContentDescription("Search azkar").performClick().assertIsFocused()
        compose.onNodeWithContentDescription("Search azkar").performTextInput("before sleeping")
        compose.onNodeWithContentDescription("Search azkar").performImeAction()
        compose.onNodeWithContentDescription("Search azkar").assertIsNotFocused()
        compose.onNodeWithText("What to say before sleeping").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun returningFromScreensDoesNotFocusSearch() {
        awaitTopics()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Search azkar").assertIsNotFocused()
        compose
            .onNodeWithContentDescription("Search azkar")
            .performClick()
            .performTextInput("wake up")
        compose
            .onNodeWithText("Supplications for when you wake up")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose
            .onNodeWithContentDescription("Search azkar")
            .assertIsNotFocused()
            .assertTextEquals("wake up")
        compose.onNodeWithContentDescription("Search azkar").performClick().assertIsFocused()
    }

    @Test
    fun materialSearchFiltersAndClearsTheList() {
        awaitTopics()
        compose.onNodeWithContentDescription("Search azkar").performTextInput("before sleeping")
        compose.onNodeWithText("What to say before sleeping").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Words of remembrance for morning and evening").assertDoesNotExist()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose
            .onNodeWithText("Words of remembrance for morning and evening")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("What to say before sleeping").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun adaptivePanesKeepSelectionAndSearchAcrossRecreation() {
        awaitTopics()
        val expanded = compose.activity.resources.configuration.screenWidthDp >= 600
        compose.onNodeWithTag("azkarListPane").assertIsDisplayed()
        if (expanded) compose.onNodeWithTag("azkarDetailPane").assertIsDisplayed()
        else compose.onNodeWithTag("azkarDetailPane").assertIsNotDisplayed()
        compose.onNodeWithContentDescription("Search azkar").performTextInput("wake up")
        compose
            .onNodeWithText("Supplications for when you wake up")
            .performScrollTo()
            .performClick()
        val prayer =
            "(All praise is for Allah who gave us life after having taken it from us and unto Him is the resurrection.)"
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(prayer).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(prayer).assertIsDisplayed()
        if (expanded) compose.onNodeWithTag("azkarListPane").assertIsDisplayed()
        else compose.onNodeWithTag("azkarListPane").assertIsNotDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(prayer).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(prayer).assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("azkarListPane").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search azkar").assertTextEquals("wake up")
        compose.onNodeWithText("What to say before sleeping").assertDoesNotExist()
    }

    @Test
    fun languageAndThemePersistAfterActivityRecreation() {
        awaitTopics()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Language").performClick()
        compose.onNodeWithText("العربية").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("المظهر").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("المظهر").performClick()
        compose.onNodeWithText("داكن").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("داكن").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("العربية").assertIsDisplayed()
        compose.onNodeWithText("داكن").assertIsDisplayed()
    }

    @Test
    fun reminderTimePickerSavesAndCancelsDrafts() {
        awaitTopics()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("08:00").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Switch to text input mode").performClick()
        compose
            .onNode(hasSetTextAction() and hasContentDescription("for hour"))
            .performTextReplacement("9")
        compose
            .onNode(hasSetTextAction() and hasContentDescription("for minutes"))
            .performTextReplacement("15")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("08:00").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Switch to text input mode").performClick()
        compose
            .onNode(hasSetTextAction() and hasContentDescription("for hour"))
            .performTextReplacement("9")
        compose
            .onNode(hasSetTextAction() and hasContentDescription("for minutes"))
            .performTextReplacement("15")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("09:15").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("09:15").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun journeyCompletionAwardsPointsAndSurvivesRecreation() {
        awaitTopics()
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Journeys").performClick()
        compose.onNodeWithTag("azkarJourneyList").performScrollToNode(hasText("Heading out"))
        compose.onNodeWithText("Heading out").performClick()
        compose.waitUntil(5_000) {
            compose
                .onAllNodesWithContentDescription("Mark complete: Leaving home azkar")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithContentDescription("Mark complete: Leaving home azkar").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Journey complete").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Journey complete").assertIsDisplayed()
        compose
            .onNode(hasText(" 35 ") and hasAnyAncestor(hasTestTag("azkarDetailPane")))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Mark incomplete: Leaving home azkar").performClick()
        compose.onNodeWithContentDescription("Mark complete: Leaving home azkar").performClick()
        compose
            .onNode(hasText(" 35 ") and hasAnyAncestor(hasTestTag("azkarDetailPane")))
            .assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Journey complete").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Journey complete").assertIsDisplayed()
        compose
            .onNode(hasText(" 35 ") and hasAnyAncestor(hasTestTag("azkarDetailPane")))
            .assertIsDisplayed()
    }
}
