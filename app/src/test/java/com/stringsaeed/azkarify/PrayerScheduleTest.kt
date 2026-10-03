package com.stringsaeed.azkarify

import com.stringsaeed.azkarify.services.*
import java.time.Instant
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class PrayerScheduleTest {
    private val raleigh =
        PrayerConfiguration("Raleigh", 35.775, -78.6336, "America/New_York", "NORTH_AMERICA", true)

    private fun date(value: String) = Date.from(Instant.parse(value))

    @Test
    fun matchesTheIosAdhanReferenceUsingTheConfiguredLocalDay() {
        assertEquals(
            listOf(
                    "2015-07-12T08:42:00Z",
                    "2015-07-12T17:21:00Z",
                    "2015-07-12T22:22:00Z",
                    "2015-07-13T00:32:00Z",
                    "2015-07-13T01:57:00Z",
                )
                .map(::date),
            PrayerSchedule.calculate(raleigh, date("2015-07-13T00:00:00Z")).map { it.time },
        )
    }

    @Test
    fun ishaAdjustmentChangesOnlyIsha() {
        val date = date("2015-07-12T12:00:00Z")
        val base = PrayerSchedule.calculate(raleigh, date)
        val adjusted = PrayerSchedule.calculate(raleigh.copy(ishaAdjustment = 30), date)
        assertEquals(base.dropLast(1), adjusted.dropLast(1))
        assertEquals(base.last().time.time + 1_800_000, adjusted.last().time.time)
    }

    @Test
    fun hanafiAsrIsLater() {
        val date = date("2015-07-12T12:00:00Z")
        val hanafi = PrayerSchedule.calculate(raleigh, date)
        val shafi = PrayerSchedule.calculate(raleigh.copy(hanafi = false), date)
        assertTrue(hanafi[2].time > shafi[2].time)
        assertEquals(hanafi.filter { it.id != "asr" }, shafi.filter { it.id != "asr" })
    }

    @Test
    fun invalidCoordinatesAndZonesAreRejected() {
        assertFalse(raleigh.copy(latitude = 91.0).isValid)
        assertFalse(raleigh.copy(longitude = Double.NaN).isValid)
        assertFalse(raleigh.copy(timeZone = "missing/timezone").isValid)
        assertFalse(raleigh.copy(ishaAdjustment = 61).isValid)
        assertNull(PrayerConfiguration.decode("broken"))
    }

    @Test
    fun configurationSurvivesEncoding() {
        assertEquals(raleigh, PrayerConfiguration.decode(raleigh.encode()))
    }

    @Test
    fun polarDayHasNoFabricatedPrayerTimes() {
        val polar = PrayerConfiguration("Longyearbyen", 78.2232, 15.6469, "Arctic/Longyearbyen")
        assertTrue(PrayerSchedule.calculate(polar, date("2026-06-21T12:00:00Z")).isEmpty())
    }
}
