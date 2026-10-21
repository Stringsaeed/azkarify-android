package sh.saeed.azkarify.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import sh.saeed.azkarify.data.AppState

/**
 * Locale for numbers and dates in the UI language. Arabic UI follows the system region, so
 * digits are Arabic-Indic (٣٣) where the system locale uses them and Latin where it does not.
 */
fun numberLocale(language: String): Locale =
    if (language == "ar")
        Locale.Builder().setLanguage("ar").setRegion(Locale.getDefault().country.ifEmpty { "EG" }).build()
    else Locale.forLanguageTag(language)

private val formats = ConcurrentHashMap<String, NumberFormat>()

fun AppState.number(value: Int): String =
    formats.getOrPut(language) { NumberFormat.getIntegerInstance(numberLocale(language)) }.format(value)

/** "2 / 4" in English; words in Arabic ("٢ من ٤"), since a slash reorders in right-to-left text. */
fun AppState.count(done: Int, total: Int): String =
    if (language == "ar") "${number(done)} من ${number(total)}" else "$done / $total"

/** "3 of 24": positions in a list. */
fun AppState.position(index: Int, total: Int): String =
    if (language == "ar") "${number(index)} من ${number(total)}" else "$index of $total"

fun AppState.percent(value: Int): String =
    if (language == "ar") "${number(value)}٪" else "$value%"

/** "Wednesday · 21 October". */
fun AppState.dateEyebrow(date: LocalDate): String =
    date.format(formatter("EEEE · d MMMM"))

/** "in 42 min" / "in 1 h 5 min". */
fun AppState.remaining(minutes: Long): String {
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).toInt()
    return if (language == "ar")
        when {
            h == 0 -> "بعد ${number(m)} دقيقة"
            m == 0 -> "بعد ${number(h)} ساعة"
            else -> "بعد ${number(h)} ساعة و${number(m)} دقيقة"
        }
    else
        when {
            h == 0 -> "in $m min"
            m == 0 -> "in $h h"
            else -> "in $h h $m min"
        }
}

/** "3 times" on the count button. */
fun AppState.times(count: Int): String =
    if (language == "ar") "${number(count)} ${if (count in 3..10) "مرات" else "مرة"}" else "$count times"

/** A compact clock time ("4:58", "16:58") for the prayer strip's five columns. */
fun AppState.clock(date: Date, zone: String, is24: Boolean): String =
    formatter(if (is24) "H:mm" else "h:mm").format(Instant.ofEpochMilli(date.time).atZone(ZoneId.of(zone)))

private fun AppState.formatter(pattern: String): DateTimeFormatter {
    val locale = numberLocale(language)
    return DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale))
}
