package sh.saeed.azkarify

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsProperties
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
 * Records the app for the Play Store preview video as a 30 fps frame sequence in
 * `app/build/footage/<lang>/`, driving the clock by hand so every animation is captured.
 * `marks.json` beside the frames lists the frame index of each beat for the edit.
 *
 * Not a test of anything; run it on purpose: `./gradlew :app:testDebugUnitTest --tests '*PreviewFootageTest*'`.
 */
private const val FOOTAGE_PHONE = "w411dp-h891dp-xxhdpi"
private const val FPS = 30

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = FOOTAGE_PHONE)
class PreviewFootageTest {
    @get:Rule val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var dir: File
    private var frame = 0
    private var clockMs = 0L
    private val marks = linkedMapOf<String, Int>()

    private fun launch(language: String) {
        app.getSharedPreferences("azkarify", 0).edit(commit = true) {
            clear()
            putBoolean("hasSeenIntro", true)
            putBoolean("locationPromptSeen", true)
            putString("theme", "light")
            putString("language", language)
            putString(
                "prayerConfiguration",
                PrayerConfiguration(
                    "Dubai", 25.2048, 55.2708, "Asia/Dubai", "DUBAI",
                    englishLocationName = "Dubai", arabicLocationName = "دبي",
                ).encode(),
            )
        }
        // Progress is stored in files; start every take from a clean day.
        app.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        dir = File("build/footage/$language").apply { deleteRecursively(); mkdirs() }
        val model = AppViewModel(app)
        compose.setContent { AzkarifyApp(model) }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        // Let the first layout and any loading settle before frame 0.
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
    }

    /** Captures frames while advancing the clock, so animations play out at 30 fps. */
    private fun roll(seconds: Double) {
        val frames = (seconds * FPS).toInt()
        repeat(frames) {
            val target = (frame + 1) * 1000L / FPS
            compose.mainClock.advanceTimeBy(target - clockMs)
            clockMs = target
            compose.waitForIdle()
            File(dir, "%05d.jpg".format(frame)).outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 92, it)
            }
            frame++
        }
    }

    private fun mark(name: String) {
        marks[name] = frame
    }

    private fun finish() {
        File(dir, "marks.json").writeText(
            marks.entries.joinToString(",\n", "{\n", "\n}") { "  \"${it.key}\": ${it.value}" } +
                "\n",
        )
        File(dir, "frames.txt").writeText("$frame\n")
    }

    /** A finger drag on [node] over [seconds], one event per frame, then a release so the list flings. */
    private fun drag(node: SemanticsNodeInteraction, dy: Float, seconds: Double) {
        val steps = (seconds * FPS).toInt()
        node.performTouchInput { down(Offset(center.x, center.y + dy.coerceAtMost(0f) * -0.5f)) }
        roll(1.0 / FPS)
        repeat(steps) {
            node.performTouchInput { moveBy(Offset(0f, dy / steps)) }
            roll(1.0 / FPS)
        }
        node.performTouchInput { up() }
    }

    private fun tap(node: SemanticsNodeInteraction, settle: Double) {
        node.performClick()
        roll(settle)
    }

    private fun tour(language: String) {
        val en = language == "en"
        launch(language)
        val today = compose.onNodeWithTag("todayList")

        // 1. Today: rest, then scroll down to the prayer journeys and back.
        mark("today")
        roll(1.6)
        mark("scroll")
        drag(today, -1500f, 1.0)
        roll(1.4)
        today.performScrollToIndex(0)
        roll(0.8)

        // 2. The morning journey: tick the steps one by one until it completes.
        mark("journey")
        tap(compose.onNodeWithText(if (en) "A gentle morning" else "بداية صباحك"), 1.4)
        compose.onNodeWithTag("journeySteps").assertExists()
        val todo =
            hasContentDescription(if (en) "Mark complete: " else "إكمال: ", substring = true) and
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, if (en) "Incomplete" else "غير مكتملة")
        repeat(3) { i ->
            mark("step${i + 1}")
            tap(compose.onAllNodes(todo).onFirst(), 1.1)
        }
        // The prayer step wants a choice first.
        tap(compose.onNode(hasClickAction() and hasText(if (en) "Subh" else "الصبح", substring = true)), 0.7)
        mark("step4")
        tap(compose.onAllNodes(todo).onFirst(), 0.6)
        mark("complete")
        roll(2.6)
        tap(compose.onNodeWithText(if (en) "Done" else "تم"), 0.9)
        mark("back")
        tap(compose.onNodeWithContentDescription(if (en) "Back" else "رجوع"), 1.3)

        // 3. The reader, then focus mode.
        mark("reader")
        tap(compose.onNodeWithTag("tab.Azkar"), 0.6)
        tap(compose.onNodeWithText(if (en) "Morning & Evening" else "الصباح والمساء"), 1.3)
        drag(compose.onNodeWithTag("azkarEntriesList"), -900f, 1.2)
        roll(1.0)
        mark("focus")
        tap(compose.onNodeWithContentDescription(if (en) "Focus mode" else "وضع التركيز"), 1.4)
        repeat(2) { tap(compose.onNodeWithTag("slideshowNextOrDone"), 1.0) }
        tap(compose.onNodeWithContentDescription(if (en) "Close" else "إغلاق"), 0.6)

        // 4. The counter: a steady run of taps.
        mark("counter")
        tap(compose.onNodeWithTag("tab.Counter"), 0.8)
        val dial = hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)
        repeat(12) { i ->
            mark("count${i + 1}")
            tap(compose.onNode(dial), 0.36)
        }
        roll(0.8)

        // 5. Night, back on Today.
        mark("settings")
        tap(compose.onNodeWithTag("tab.Settings"), 1.0)
        mark("night")
        tap(compose.onNodeWithText(if (en) "Night" else "ليلي"), 1.2)
        mark("night-today")
        tap(compose.onNodeWithTag("tab.Today"), 2.2)
        mark("end")
        finish()
    }

    @Test fun english() = tour("en")

    @Test @Config(qualifiers = "ar-$FOOTAGE_PHONE") fun arabic() = tour("ar")
}
