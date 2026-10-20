package sh.saeed.azkarify.services

import android.content.Context
import sh.saeed.azkarify.BuildConfig
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CatalogCountry(
    val code: String,
    val englishName: String,
    val arabicName: String,
    val flag: String,
) {
    fun displayName(language: String) = if (language == "ar") arabicName else englishName
}

data class CatalogPage<T>(val items: List<T>, val nextCursor: String?)

internal class CatalogDatasetChanged : IOException("Country catalog changed")

internal fun parseCountries(document: JSONObject): CatalogPage<CatalogCountry> {
    val data = document.getJSONArray("data")
    val countries = List(data.length()) { index ->
        val value = data.getJSONObject(index)
        val names = value.getJSONObject("names")
        val english = names.getString("en")
        CatalogCountry(
            code = value.getString("code"),
            englishName = english,
            arabicName = names.optString("ar").takeIf { it.isNotBlank() && it != "null" } ?: english,
            flag = value.optString("flag"),
        )
    }
    return CatalogPage(countries, nextCursor(document))
}

internal fun parseCities(document: JSONObject): CatalogPage<PrayerCityOption> {
    val data = document.getJSONArray("data")
    val cities = List(data.length()) { index ->
        val value = data.getJSONObject(index)
        val names = value.getJSONObject("names")
        val location = value.getJSONObject("location")
        val english = names.getString("en")
        val timeZone = value.getString("timezone")
        val method = value.optString("prayerCalculationMethod").uppercase()
        PrayerCityOption(
            name = english,
            arabicName = names.optString("ar").takeIf { it.isNotBlank() && it != "null" } ?: english,
            countryCode = value.getString("countryCode"),
            latitude = location.getDouble("latitude"),
            longitude = location.getDouble("longitude"),
            timeZone = timeZone,
            method = method.takeIf { candidate -> prayerMethods.any { it.first == candidate } }
                ?: PrayerSchedule.recommendedMethod(timeZone),
            id = value.get("id").toString(),
        )
    }
    return CatalogPage(cities, nextCursor(document))
}

private fun nextCursor(document: JSONObject): String? =
    document.getJSONObject("meta").optString("nextCursor").takeIf { it.isNotBlank() && it != "null" }

internal class CountryCatalogClient(
    private val cacheDirectory: File,
    private val baseUrl: String,
    private val authorization: CatalogAuthorization,
) {
    init { cacheDirectory.mkdirs() }

    companion object {
        fun configured(context: Context): CountryCatalogClient? {
            val authorization =
                if (BuildConfig.DEBUG) {
                    val token = BuildConfig.CATALOG_DEV_TOKEN
                    if (token.isBlank()) return null
                    LocalCatalogAuthorization(token)
                } else {
                    PlayCatalogAuthorization(context, BuildConfig.CATALOG_CLOUD_PROJECT_NUMBER)
                }
            val cache = File(context.applicationContext.filesDir, "country-catalog").apply { mkdirs() }
            return CountryCatalogClient(cache, BuildConfig.CATALOG_BASE_URL, authorization)
        }
    }

    suspend fun countries(language: String): List<CatalogCountry> {
        for (attempt in 0..1) {
            try {
                val result = mutableListOf<CatalogCountry>()
                var cursor: String? = null
                var version: String? = null
                do {
                    val parameters = mutableListOf("limit" to "250")
                    if (cursor != null) parameters += "cursor" to cursor
                    val document = fetch("countries.json", parameters, language)
                    val pageVersion = document.getJSONObject("meta").getString("version")
                    if (version != null && version != pageVersion) throw CatalogDatasetChanged()
                    version = pageVersion
                    val page = parseCountries(document)
                    result += page.items
                    cursor = page.nextCursor
                } while (cursor != null)
                return result.distinctBy(CatalogCountry::code)
            } catch (error: CatalogDatasetChanged) {
                cacheDirectory.listFiles()?.forEach(File::delete)
                if (attempt == 1) throw error
            }
        }
        throw CatalogDatasetChanged()
    }

    suspend fun cities(
        countryCode: String,
        language: String,
        query: String = "",
        cursor: String? = null,
    ): CatalogPage<PrayerCityOption> {
        require(countryCode.matches(Regex("[A-Z]{2}")))
        val parameters = mutableListOf("limit" to "50")
        if (query.isNotBlank()) parameters += "q" to query.trim()
        if (cursor != null) parameters += "cursor" to cursor
        return parseCities(fetch("countries/$countryCode/cities.json", parameters, language))
    }

    private suspend fun fetch(
        path: String,
        parameters: List<Pair<String, String>>,
        language: String,
    ): JSONObject = withContext(Dispatchers.IO) {
        val query = parameters.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8.name())}"
        }
        val url = "${baseUrl.trimEnd('/')}/$path?$query"
        val cacheKey = MessageDigest.getInstance("SHA-256")
            .digest("$url|$language".toByteArray(StandardCharsets.UTF_8))
        val filename = Base64.getUrlEncoder().withoutPadding().encodeToString(cacheKey)
        val cacheFile = File(cacheDirectory, "$filename.json")
        val cached = runCatching {
            JSONObject(cacheFile.readText()).also { JSONObject(it.getString("body")) }
        }.getOrNull()
        val age = System.currentTimeMillis() - (cached?.optLong("storedAt") ?: 0L)
        if (cached != null && age in 0 until 86_400_000L) {
            return@withContext JSONObject(cached.getString("body"))
        }

        try {
            val etag = cached?.optString("etag")?.takeIf(String::isNotBlank)
            val (authName, authValue) = authorization.header("GET", url, language, etag)
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Accept-Language", language)
                if (etag != null) connection.setRequestProperty("If-None-Match", etag)
                connection.setRequestProperty(authName, authValue)
                when (connection.responseCode) {
                    200 -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val document = JSONObject(body)
                        runCatching { save(cacheFile, body, connection.getHeaderField("ETag") ?: "") }
                        document
                    }
                    304 -> {
                        if (cached == null) throw IOException("Catalog cache is missing")
                        val body = cached.getString("body")
                        runCatching { save(cacheFile, body, etag ?: "") }
                        JSONObject(body)
                    }
                    409 -> throw CatalogDatasetChanged()
                    else -> throw IOException("Catalog request failed (${connection.responseCode})")
                }
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (error is CatalogDatasetChanged) {
                cacheDirectory.listFiles()?.forEach(File::delete)
                throw error
            }
            if (cached != null) JSONObject(cached.getString("body")) else throw error
        }
    }

    private fun save(file: File, body: String, etag: String) {
        val payload = JSONObject()
            .put("storedAt", System.currentTimeMillis())
            .put("etag", etag)
            .put("body", body)
            .toString()
        val temporary = File(file.parentFile, "${file.name}.${UUID.randomUUID()}.tmp")
        temporary.writeText(payload)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw IOException("Could not save catalog cache")
        }
    }
}
