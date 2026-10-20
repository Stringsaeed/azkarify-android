package sh.saeed.azkarify

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import sh.saeed.azkarify.services.catalogRequestHash
import sh.saeed.azkarify.services.parseCities
import sh.saeed.azkarify.services.parseCountries

class CountryCatalogTest {
    @Test
    fun integrityHashMatchesTheCountriesApiReference() {
        assertEquals(
            "qiMLNzO9e1_BHYxHtLy42Hk2R1Eyku5kndgTj5uXaac",
            catalogRequestHash(
                "GET",
                "https://api.example.com/v1/countries.json?lang=en",
                "en",
            ),
        )
    }

    @Test
    fun catalogPagesKeepLocalizedNamesAndPrayerCoordinates() {
        val countries = parseCountries(JSONObject("""{
          "data":[{"code":"AE","flag":"🇦🇪","names":{"en":"United Arab Emirates","ar":"الإمارات العربية المتحدة"}}],
          "meta":{"nextCursor":"next-country"}
        }"""))
        assertEquals("AE", countries.items.single().code)
        assertEquals("الإمارات العربية المتحدة", countries.items.single().displayName("ar"))
        assertEquals("next-country", countries.nextCursor)

        val cities = parseCities(JSONObject("""{
          "data":[{"id":292223,"countryCode":"AE","names":{"en":"Dubai","ar":"دبي"},
            "location":{"latitude":25.2048,"longitude":55.2708},"timezone":"Asia/Dubai",
            "prayerCalculationMethod":"dubai"}],
          "meta":{"nextCursor":null}
        }"""))
        assertEquals("292223", cities.items.single().id)
        assertEquals("دبي", cities.items.single().displayName("ar"))
        assertEquals(25.2048, cities.items.single().latitude, 0.0)
        assertEquals("DUBAI", cities.items.single().method)
        assertNull(cities.nextCursor)
    }
}
