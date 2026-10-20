package sh.saeed.azkarify

import sh.saeed.azkarify.data.*
import org.junit.Assert.*
import org.junit.Test

class JourneyProgressTest {
    private val first = Step("first", "First")
    private val choice =
        Step("prayer", "Prayer", options = listOf("subh" to "Subh", "duha" to "Duha"))
    private val optional = Step("optional", "Optional", optional = true)
    private val journey = Journey("morning", "Morning", "Begin", listOf(first, choice, optional))
    private val day = "2026-10-03"

    @Test
    fun completingRequiredStepsAwardsBonusOnlyOnceEvenAfterReopening() {
        val store = JourneyProgress()
        store.toggle(day, journey, first)
        assertEquals(10, store.points)
        store.choose(day, journey, choice, "subh")
        store.toggle(day, journey, choice)
        assertTrue(store.complete(day, journey))
        assertEquals(45, store.points)
        store.toggle(day, journey, first)
        assertFalse(store.complete(day, journey))
        store.toggle(day, journey, first)
        assertTrue(store.complete(day, journey))
        assertEquals(45, store.points)
    }

    @Test
    fun choiceIsRequiredAndChangingItReopensTheStepWithoutRepeatingAwards() {
        val store = JourneyProgress()
        store.toggle(day, journey, choice)
        assertFalse(store.completed(day, journey, choice))
        assertEquals(0, store.points)
        store.choose(day, journey, choice, "subh")
        store.toggle(day, journey, choice)
        store.choose(day, journey, choice, "duha")
        assertFalse(store.completed(day, journey, choice))
        store.toggle(day, journey, choice)
        assertEquals(10, store.points)
    }

    @Test
    fun nextDayHasFreshProgressAndCanEarnPointsAgain() {
        val store = JourneyProgress()
        store.toggle(day, journey, first)
        assertFalse(store.completed("2026-10-04", journey, first))
        store.toggle("2026-10-04", journey, first)
        assertEquals(20, store.points)
        assertEquals(10, store.todayPoints(day))
    }

    @Test
    fun persistenceRetainsChoicesProgressAndAwards() {
        val store = JourneyProgress()
        store.choose(day, journey, choice, "duha")
        store.toggle(day, journey, choice)
        val restored = JourneyProgress(store.encode())
        assertEquals("duha", restored.choice(day, journey, choice))
        assertTrue(restored.completed(day, journey, choice))
        assertEquals(10, restored.points)
    }

    @Test
    fun optionalStepAwardsPointsWithoutCompletingRequiredSteps() {
        val store = JourneyProgress()
        store.toggle(day, journey, optional)
        assertEquals(0, store.completedCount(day, journey))
        assertEquals(10, store.points)
        assertFalse(store.complete(day, journey))
    }

    @Test
    fun everyThirtyThreeCounterTapsEarnsOnePointAcrossRelaunches() {
        val firstSession = JourneyProgress()
        repeat(32) { firstSession.recordCounterTap(day) }
        assertEquals(0, firstSession.points)

        val restored = JourneyProgress(firstSession.encode())
        restored.recordCounterTap(day)
        assertEquals(1, restored.points)
        repeat(33) { restored.recordCounterTap("2026-10-04") }
        assertEquals(2, restored.points)
        assertEquals(1, restored.todayPoints(day))
        assertEquals(1, restored.todayPoints("2026-10-04"))
    }

    @Test
    fun slideshowCompletionAwardsThirtyPointsOncePerCategoryPerDay() {
        val store = JourneyProgress()
        store.completeSlideshow(day, 27)
        store.completeSlideshow(day, 27)
        assertEquals(30, store.points)

        val restored = JourneyProgress(store.encode())
        restored.completeSlideshow(day, 28)
        restored.completeSlideshow("2026-10-04", 27)
        assertEquals(90, restored.points)
        assertEquals(60, restored.todayPoints(day))
    }
}
