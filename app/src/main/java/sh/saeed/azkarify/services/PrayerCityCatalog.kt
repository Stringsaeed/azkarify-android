package sh.saeed.azkarify.services

import android.content.res.AssetManager
import org.json.JSONArray

data class PrayerCityOption(
    val name: String,
    val arabicName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val method: String,
    val id: String = name,
) {
    fun displayName(language: String) = if (language == "ar") arabicName else name
}

object PrayerCityCatalog {
    fun bundled(assets: AssetManager): List<PrayerCityOption> {
        val source = assets.open("prayer_cities.json").bufferedReader().use { it.readText() }
        val cities = JSONArray(source)
        return List(cities.length()) { index ->
            val city = cities.getJSONObject(index)
            PrayerCityOption(
                name = city.getString("name"),
                arabicName = city.getString("arabicName"),
                countryCode = city.getString("countryCode"),
                latitude = city.getDouble("latitude"),
                longitude = city.getDouble("longitude"),
                timeZone = city.getString("timeZone"),
                method = city.getString("method"),
            )
        }
    }
}
