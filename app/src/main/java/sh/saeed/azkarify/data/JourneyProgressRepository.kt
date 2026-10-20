package sh.saeed.azkarify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal const val journeyProgressKeyName = "journey-progress.v2"
internal val journeyProgressKey = stringPreferencesKey(journeyProgressKeyName)
internal val Context.journeyDataStore by
    preferencesDataStore(
        name = "journey-progress",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceMigrations = { context ->
            listOf(
                SharedPreferencesMigration(
                    context,
                    "azkarify",
                    keysToMigrate = setOf(journeyProgressKeyName),
                )
            )
        },
    )

class JourneyProgressRepository(private val store: DataStore<Preferences>) {
    private fun decode(preferences: Preferences) =
        JourneyProgress(preferences.asMap()[journeyProgressKey] as? String ?: "{}")

    val progress = store.data.map(::decode).flowOn(Dispatchers.IO)

    suspend fun toggle(day: String, journey: Journey, step: Step) = update {
        it.toggle(day, journey, step)
    }

    suspend fun choose(day: String, journey: Journey, step: Step, option: String) = update {
        it.choose(day, journey, step, option)
    }

    suspend fun recordCounterTap(day: String) = update { it.recordCounterTap(day) }

    suspend fun completeSlideshow(day: String, categoryId: Int) = update {
        it.completeSlideshow(day, categoryId)
    }

    private suspend fun update(change: (JourneyProgress) -> Unit) =
        withContext(Dispatchers.IO) {
            val updated =
                store.edit { preferences ->
                    val progress = decode(preferences)
                    change(progress)
                    preferences[journeyProgressKey] = progress.encode()
                }
            decode(updated)
        }
}
