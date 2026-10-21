package sh.saeed.azkarify

import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import sh.saeed.azkarify.ui.numberLocale

class FormatTest {
    @Test
    fun arabicUsesArabicIndicDigitsWhateverTheRegion() {
        val previous = Locale.getDefault()
        try {
            // Morocco writes Latin digits by default; the app's Arabic UI still uses ٠-٩.
            for (system in listOf(Locale.US, Locale.forLanguageTag("ar-MA"), Locale.forLanguageTag("ar-EG"))) {
                Locale.setDefault(system)
                val locale = numberLocale("ar")
                assertEquals("٣٣", NumberFormat.getIntegerInstance(locale).format(33))
                val time = DateTimeFormatter.ofPattern("H:mm", locale).withDecimalStyle(DecimalStyle.of(locale))
                assertEquals("١٦:٠٥", time.format(LocalTime.of(16, 5)))
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun englishKeepsLatinDigits() {
        assertEquals("33", NumberFormat.getIntegerInstance(numberLocale("en")).format(33))
    }
}
