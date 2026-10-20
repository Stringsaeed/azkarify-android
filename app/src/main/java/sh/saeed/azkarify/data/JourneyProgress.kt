package sh.saeed.azkarify.data

import org.json.JSONObject
import java.util.UUID

class JourneyProgress(encoded: String = "{}") {
    private val payload = runCatching { JSONObject(encoded) }.getOrElse { JSONObject() }

    private fun key(day: String, journey: Journey, step: Step) = "$day|${journey.id}|${step.id}"

    private fun records() =
        payload.optJSONObject("records") ?: JSONObject().also { payload.put("records", it) }

    private fun awards() =
        payload.optJSONObject("awards") ?: JSONObject().also { payload.put("awards", it) }

    private fun choices() =
        payload.optJSONObject("choices") ?: JSONObject().also { payload.put("choices", it) }

    fun choice(day: String, journey: Journey, step: Step): String =
        choices().optString(key(day, journey, step))

    fun completed(day: String, journey: Journey, step: Step) =
        records().optBoolean(key(day, journey, step)) &&
            (step.options.isEmpty() || step.options.any { it.first == choice(day, journey, step) })

    fun completedCount(day: String, journey: Journey) =
        journey.requiredSteps.count { completed(day, journey, it) }

    fun complete(day: String, journey: Journey) =
        completedCount(day, journey) == journey.requiredSteps.size

    fun choose(day: String, journey: Journey, step: Step, option: String) {
        require(step in journey.steps && step.options.any { it.first == option })
        if (choice(day, journey, step) != option) records().put(key(day, journey, step), false)
        choices().put(key(day, journey, step), option)
    }

    fun toggle(day: String, journey: Journey, step: Step) {
        require(step in journey.steps)
        if (
            step.options.isNotEmpty() &&
                step.options.none { it.first == choice(day, journey, step) }
        )
            return
        val key = key(day, journey, step)
        val value = !completed(day, journey, step)
        records().put(key, value)
        if (value) {
            award(key, 10)
            val bonus = "$day|${journey.id}|bonus"
            if (complete(day, journey)) award(bonus, 25)
        }
    }

    fun recordCounterTap(day: String) {
        val remainder = payload.optInt("counterTapRemainder", 0).coerceIn(0, 32) + 1
        payload.put("counterTapRemainder", remainder % 33)
        if (remainder == 33) award("$day|counter|${UUID.randomUUID()}", 1)
    }

    fun completeSlideshow(day: String, categoryId: Int) {
        award("$day|slideshow|$categoryId", 30)
    }

    private val dayPoints = mutableMapOf<String, Int>()
    var points: Int = 0
        private set

    init {
        for (key in awards().keys()) {
            val amount = awards().optInt(key, 0).coerceAtLeast(0)
            points += amount
            val day = key.substringBefore('|')
            dayPoints[day] = (dayPoints[day] ?: 0) + amount
        }
    }

    private fun award(key: String, amount: Int) {
        if (awards().has(key)) return
        awards().put(key, amount)
        points += amount
        val day = key.substringBefore('|')
        dayPoints[day] = (dayPoints[day] ?: 0) + amount
    }

    fun todayPoints(day: String) = dayPoints[day] ?: 0

    fun encode() = payload.toString()
}
