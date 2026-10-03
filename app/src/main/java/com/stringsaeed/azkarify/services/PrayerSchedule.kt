package com.stringsaeed.azkarify.services

import com.batoulapps.adhan2.*
import com.batoulapps.adhan2.data.DateComponents
import java.text.DateFormat
import java.time.ZoneId
import java.util.*
import org.json.JSONObject

// Adhan calculation methods; method labels match the iOS app.
val prayerMethods =
    listOf(
        "MUSLIM_WORLD_LEAGUE" to "Muslim World League",
        "EGYPTIAN" to "Egyptian General Authority of Survey",
        "KARACHI" to "University of Islamic Sciences, Karachi",
        "UMM_AL_QURA" to "Umm al-Qura, Makkah",
        "DUBAI" to "Dubai, UAE",
        "MOON_SIGHTING_COMMITTEE" to "Moonsighting Committee",
        "NORTH_AMERICA" to "North America, ISNA",
        "KUWAIT" to "Kuwait",
        "QATAR" to "Qatar",
        "SINGAPORE" to "Singapore",
        "TURKEY" to "Turkey, Diyanet approximation",
    )

data class PrayerConfiguration(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val method: String = "MUSLIM_WORLD_LEAGUE",
    val hanafi: Boolean = false,
    val ishaAdjustment: Int = 0,
) {
    val isValid
        get() =
            locationName.isNotBlank() &&
                latitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude.isFinite() &&
                longitude in -180.0..180.0 &&
                timeZone in ZoneId.getAvailableZoneIds() &&
                prayerMethods.any { it.first == method } &&
                ishaAdjustment in -60..60

    fun encode() =
        JSONObject()
            .put("name", locationName)
            .put("lat", latitude)
            .put("lon", longitude)
            .put("zone", timeZone)
            .put("method", method)
            .put("hanafi", hanafi)
            .put("isha", ishaAdjustment)
            .toString()

    companion object {
        fun decode(value: String?): PrayerConfiguration? =
            value?.let { it ->
                runCatching {
                        val j = JSONObject(it)
                        PrayerConfiguration(
                            j.getString("name"),
                            j.getDouble("lat"),
                            j.getDouble("lon"),
                            j.getString("zone"),
                            j.getString("method"),
                            j.getBoolean("hanafi"),
                            j.optInt("isha"),
                        )
                    }
                    .getOrNull()
                    ?.takeIf { it.isValid }
            }
    }
}

data class ScheduledPrayer(val id: String, val time: Date)

object PrayerSchedule {
    fun calculate(config: PrayerConfiguration, date: Date): List<ScheduledPrayer> {
        require(config.isValid)
        val calendar =
            Calendar.getInstance(TimeZone.getTimeZone(config.timeZone)).apply { time = date }
        val coordinates = Coordinates(config.latitude, config.longitude)
        val params =
            CalculationMethod.valueOf(config.method)
                .parameters
                .copy(
                    madhab = if (config.hanafi) Madhab.HANAFI else Madhab.SHAFI,
                    highLatitudeRule = HighLatitudeRule.recommendedFor(coordinates),
                    prayerAdjustments = PrayerAdjustments(isha = config.ishaAdjustment),
                )
        val times =
            runCatching {
                    PrayerTimes(
                        Coordinates(config.latitude, config.longitude),
                        DateComponents(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ),
                        params,
                    )
                }
                .getOrNull() ?: return emptyList()
        return listOf(
                "fajr" to times.fajr,
                "dhuhr" to times.dhuhr,
                "asr" to times.asr,
                "maghrib" to times.maghrib,
                "isha" to times.isha,
            )
            .map { (id, time) -> ScheduledPrayer(id, Date(time.toEpochMilliseconds())) }
    }

    fun format(date: Date, config: PrayerConfiguration, language: String): String =
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag(language))
            .apply { timeZone = TimeZone.getTimeZone(config.timeZone) }
            .format(date)
}
