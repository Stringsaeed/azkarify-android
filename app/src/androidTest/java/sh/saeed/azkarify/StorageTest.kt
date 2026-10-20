package sh.saeed.azkarify

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import sh.saeed.azkarify.data.*
import sh.saeed.azkarify.services.Reminders
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class StorageTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val day = "2026-10-03"

    @Test
    fun concurrentRepositoryReadsReturnCompleteBilingualContent() =
        runBlocking<Unit> {
            val repository = AzkarRepository(context.assets)
            coroutineScope {
                (0 until 160)
                    .map { index ->
                        async(Dispatchers.IO) {
                            val language = if (index % 2 == 0) "en" else "ar"
                            val categories = repository.categories(language)
                            assertEquals(listOf(27, 28, 1), categories.take(3).map { it.id })
                            assertEquals(132, categories.size)
                            assertEquals(
                                if (language == "ar") "أذكار الصباح والمساء"
                                else "Words of remembrance for morning and evening",
                                categories.first().title,
                            )
                            val entries = repository.entries(categories.first { it.id == 1 })
                            assertEquals(1, entries.first().id)
                            assertEquals(1, entries.first().repetitions)
                            assertEquals(
                                "(All praise is for Allah who gave us life after having taken it from us and unto Him is the resurrection.)",
                                entries.first().translated.takeIf { language == "en" }
                                    ?: repository
                                        .entries(repository.categories("en").first { it.id == 1 })
                                        .first()
                                        .translated,
                            )
                            assertTrue(entries.first().arabic.isNotBlank())
                        }
                    }
                    .awaitAll()
            }
        }

    @Test
    fun datastoreMigratesExistingProgressWithoutRemovingOtherPreferences() =
        runBlocking<Unit> {
            val name = "journey-test-${UUID.randomUUID()}"
            val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val step = Step("leave-home", "Leaving home")
            val journey = Journey("going-out", "Heading out", "", listOf(step))
            val original = JourneyProgress().apply { toggle(day, journey, step) }
            preferences
                .edit()
                .putString(journeyProgressKeyName, original.encode())
                .putString("accent", "blue")
                .commit()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val file = File(context.cacheDir, "$name.preferences_pb")
            val store =
                PreferenceDataStoreFactory.create(
                    migrations =
                        listOf(
                            SharedPreferencesMigration(
                                context,
                                name,
                                keysToMigrate = setOf(journeyProgressKeyName),
                            )
                        ),
                    scope = scope,
                    produceFile = { file },
                )
            try {
                val repository = JourneyProgressRepository(store)
                val restored = repository.progress.first()
                assertEquals(35, restored.points)
                assertTrue(restored.completed(day, journey, step))
                assertFalse(preferences.contains(journeyProgressKeyName))
                assertEquals("blue", preferences.getString("accent", null))
                repository.toggle(day, journey, step)
                repository.toggle(day, journey, step)
                assertEquals(35, repository.progress.first().points)
            } finally {
                scope.coroutineContext[Job]?.cancelAndJoin()
                file.delete()
                preferences.edit().clear().commit()
            }
        }

    @Test
    fun concurrentProgressUpdatesKeepEveryStepAndAward() =
        runBlocking<Unit> {
            val steps = (1..40).map { Step("step-$it", "Step $it") }
            val journey = Journey("routine", "Routine", "", steps)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val file = File(context.cacheDir, "progress-${UUID.randomUUID()}.preferences_pb")
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            try {
                val repository = JourneyProgressRepository(store)
                coroutineScope {
                    steps
                        .map { step ->
                            async(Dispatchers.IO) { repository.toggle(day, journey, step) }
                        }
                        .awaitAll()
                }
                val progress = repository.progress.first()
                assertEquals(40, progress.completedCount(day, journey))
                assertEquals(425, progress.points)
                assertEquals(425, progress.todayPoints(day))
            } finally {
                scope.coroutineContext[Job]?.cancelAndJoin()
                file.delete()
            }
        }

    @Test
    fun wrongPreferenceTypesUseSafeDefaults() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = context.applicationContext
        require(application is Application)
        val preferences = context.getSharedPreferences("azkarify", Context.MODE_PRIVATE)
        preferences
            .edit()
            .clear()
            .putInt("theme", 17)
            .putBoolean("accent", true)
            .putString("favorites", "wrong type")
            .commit()
        context.applyAppLanguage("en")
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val model = AppViewModel(application)
                store.put("preferences-test", model)
                assertEquals("system", model.state.value.theme)
                assertEquals("brown", model.state.value.accent)
                assertTrue(model.state.value.favorites.isEmpty())
                store.clear()
            }
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun inexactReminderSchedulesWithoutExactAlarmPermission() {
        val preferences = context.getSharedPreferences("azkarify", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("reminder.morning.enabled", true).commit()
        try {
            Reminders.schedule(context, "morning")
        } finally {
            preferences.edit().putBoolean("reminder.morning.enabled", false).commit()
            Reminders.schedule(context, "morning")
        }
    }
}
