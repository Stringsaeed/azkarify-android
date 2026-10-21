package sh.saeed.azkarify.data

import android.content.res.AssetManager
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

data class Category(val id: Int, val title: String, val path: String)

data class Entry(
    val id: Int,
    val repetitions: Int,
    val arabic: String,
    val translated: String,
    val transliteration: String = "",
) {
    fun text(language: String) = if (language == "ar") arabic else translated
}

data class Step(
    val id: String,
    val title: String,
    val categories: List<Int> = emptyList(),
    val optional: Boolean = false,
    val note: String = "",
    val options: List<Pair<String, String>> = emptyList(),
)

data class Journey(
    val id: String,
    val title: String,
    val subtitle: String,
    val steps: List<Step>,
    val prayer: String? = null,
) {
    val requiredSteps
        get() = steps.filterNot { it.optional }
}

class AzkarRepository(private val assets: AssetManager) {
    private val categoryCache = ConcurrentHashMap<String, List<Category>>()
    private val entryCache = ConcurrentHashMap<String, List<Entry>>()

    fun categories(language: String): List<Category> =
        categoryCache.computeIfAbsent(language) {
            json("content/$language/husn_$language.json").getJSONArray("items").objects().map {
                Category(it.getInt("id"), it.getString("title"), it.getString("detailUrl"))
            }
        }

    fun entries(category: Category): List<Entry> =
        entryCache.computeIfAbsent(category.path) {
            require(category.path.matches(Regex("/(en|ar)/[0-9]+\\.json")))
            json("content${category.path}").getJSONArray("items").objects().map {
                val text = it.getJSONObject("text")
                Entry(
                    it.getInt("id"),
                    it.getInt("repeat"),
                    text.getString("arabic"),
                    text.getString("translated"),
                    text.optString("arabicTranslated"),
                )
            }
        }

    fun journeys(copy: Map<String, String>): List<Journey> {
        fun label(key: String) = copy[key] ?: key
        val routines =
            JSONArray(read("journeys.json")).objects().map { j ->
                Journey(
                    j.getString("id"),
                    label(j.getString("title")),
                    label(j.getString("subtitle")),
                    j.getJSONArray("steps").objects().map { s ->
                        Step(
                            s.getString("id"),
                            label(s.getString("title")),
                            s.getJSONArray("categories").let { a ->
                                List(a.length()) { a.getInt(it) }
                            },
                            s.getBoolean("optional"),
                            label(s.getString("note")),
                            s.getJSONArray("options").objects().map {
                                it.getString("id") to label(it.getString("title"))
                            },
                        )
                    },
                )
            }
        return routines.take(2) + prayerJourneys(copy) + routines.drop(2)
    }

    private fun json(path: String) = JSONObject(read(path))

    private fun read(path: String) = assets.open(path).bufferedReader().use { it.readText() }
}

fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

fun prayerJourneys(copy: Map<String, String>): List<Journey> {
    fun label(key: String) = copy[key] ?: key
    return listOf(
            "fajr" to "Fajr",
            "dhuhr" to "Dhuhr",
            "asr" to "Asr",
            "maghrib" to "Maghrib",
            "isha" to "Isha",
        )
        .map { (id, name) ->
            Journey(
                "prayer-$id",
                label("$name journey"),
                label("Prepare, pray, and remember"),
                listOf(
                    Step(
                        "restroom",
                        label("Visit the restroom"),
                        listOf(6, 7),
                        note = label("If needed before wudu."),
                    ),
                    Step("wudu", label("Make wudu"), listOf(8, 9)),
                    Step(
                        "mosque",
                        label("Go to the mosque"),
                        listOf(12, 13),
                        note = label("When praying at the mosque."),
                    ),
                    Step("adhan", label("Azkar for the adhan"), listOf(15)),
                    Step(
                        "before-iqama",
                        label("Dua before the iqama"),
                        note = label("Take a moment for your own dua."),
                    ),
                    Step("prayer", label("Pray with remembrance"), (16..24).toList()),
                    Step("after-prayer", label("After prayer azkar"), listOf(25)),
                    Step(
                        "leave-mosque",
                        label("Leave the mosque"),
                        listOf(14),
                        note = label("When praying at the mosque."),
                    ),
                ),
                id,
            )
        }
}
