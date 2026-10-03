package com.stringsaeed.azkarify.data

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stringsaeed.azkarify.services.PrayerConfiguration
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

enum class ProgressFailure {
    Load, Save,
}

data class AppState(
    val language: String = "en",
    val theme: String = "system",
    val accent: String = "brown",
    val introSeen: Boolean = false,
    val locationPromptSeen: Boolean = false,
    val categories: List<Category> = emptyList(),
    val favorites: Set<Int> = emptySet(),
    val journeys: List<Journey> = emptyList(),
    val copy: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val progressVersion: Int = 0,
    val progressReady: Boolean = false,
    val progressFailure: ProgressFailure? = null,
    val prayerConfiguration: PrayerConfiguration? = null,
) {
    fun text(key: String) = copy[key] ?: key
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val preferences: SharedPreferences = application.getSharedPreferences("azkarify", 0)
    private val repository = AzkarRepository(application.assets)
    private val savedPreferences = preferences.all
    private val progressRepository = JourneyProgressRepository(application.journeyDataStore)
    var progress = JourneyProgress()
        private set

    private var progressJob: Job? = null
    private val initialLanguage = application.appLanguage()
    private val mutableState =
        MutableStateFlow(
            AppState(
                language = initialLanguage,
                copy = application.uiStrings(initialLanguage),
                theme = (savedPreferences["theme"] as? String)?.takeIf {
                    it in setOf("system", "light", "dark")
                } ?: "system",
                accent = (savedPreferences["accent"] as? String)?.takeIf {
                    it in setOf("brown", "saffron", "teal", "blue", "green")
                } ?: "brown",
                introSeen = savedPreferences["hasSeenIntro"] as? Boolean ?: false,
                locationPromptSeen = savedPreferences["locationPromptSeen"] as? Boolean ?: false,
                favorites = (savedPreferences["favorites"] as? Set<*>).orEmpty()
                    .filterIsInstance<String>().mapNotNull(String::toIntOrNull).toSet(),
                prayerConfiguration = PrayerConfiguration.decode(savedPreferences["prayerConfiguration"] as? String),
            ))
    val state = mutableState.asStateFlow()

    init {
        loadProgress()
        load()
    }

    fun load() {
        val language = mutableState.value.language
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val copy = getApplication<Application>().uiStrings(language)
                    Triple(repository.categories(language), copy, repository.journeys(copy))
                }
                if (mutableState.value.language == language) mutableState.update {
                    it.copy(
                        categories = loaded.first,
                        copy = loaded.second,
                        journeys = loaded.third,
                        loading = false,
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (mutableState.value.language == language) mutableState.update {
                    it.copy(
                        error = e.localizedMessage,
                        loading = false
                    )
                }
            }
        }
    }

    suspend fun entries(category: Category) =
        withContext(Dispatchers.IO) { repository.entries(category) }

    fun toggleFavorite(id: Int) {
        val next = mutableState.value.favorites.let { if (id in it) it - id else it + id }
        preferences.edit { putStringSet("favorites", next.map(Int::toString).toSet()) }
        mutableState.update { it.copy(favorites = next) }
    }

    fun setTheme(value: String) {
        preferences.edit { putString("theme", value) }
        mutableState.update { it.copy(theme = value) }
    }

    fun setAccent(value: String) {
        preferences.edit { putString("accent", value) }
        mutableState.update { it.copy(accent = value) }
    }

    fun setLanguage(value: String) {
        require(value == "en" || value == "ar")
        preferences.edit { putString("language", value) }
        getApplication<Application>().applyAppLanguage(value)
        if (value == mutableState.value.language) return
        mutableState.update {
            it.copy(
                language = value,
                copy = getApplication<Application>().uiStrings(value),
                categories = emptyList(),
            )
        }
        load()
    }

    fun refreshLanguage() {
        val value = getApplication<Application>().appLanguage()
        if (value == mutableState.value.language) return
        mutableState.update {
            it.copy(
                language = value,
                copy = getApplication<Application>().uiStrings(value),
                categories = emptyList(),
            )
        }
        load()
    }

    fun finishIntro() {
        preferences.edit { putBoolean("hasSeenIntro", true) }
        mutableState.update { it.copy(introSeen = true) }
    }

    fun finishLocationPrompt() {
        preferences.edit { putBoolean("locationPromptSeen", true) }
        mutableState.update { it.copy(locationPromptSeen = true) }
    }

    fun savePrayer(value: PrayerConfiguration) {
        require(value.isValid)
        preferences.edit { putString("prayerConfiguration", value.encode()) }
        mutableState.update { it.copy(prayerConfiguration = value) }
    }

    fun clearPrayer() {
        preferences.edit { remove("prayerConfiguration") }
        mutableState.update { it.copy(prayerConfiguration = null) }
    }

    fun loadProgress() {
        progressJob?.cancel()
        mutableState.update { it.copy(progressFailure = null) }
        progressJob = viewModelScope.launch {
            try {
                progressRepository.progress.collect { loaded ->
                    progress = loaded
                    mutableState.update {
                        it.copy(
                            progressVersion = it.progressVersion + 1,
                            progressReady = true,
                            progressFailure = null,
                        )
                    }
                }
            } catch (_: IOException) {
                mutableState.update { it.copy(progressFailure = ProgressFailure.Load) }
            }
        }
    }

    fun dismissProgressFailure() {
        mutableState.update { it.copy(progressFailure = null) }
    }

    fun toggleStep(
        day: String,
        journey: Journey,
        step: Step,
        onSaved: (JourneyProgress) -> Unit = {},
    ) = saveProgress({ progressRepository.toggle(day, journey, step) }, onSaved)

    fun choose(day: String, journey: Journey, step: Step, option: String) =
        saveProgress({ progressRepository.choose(day, journey, step, option) })

    private fun saveProgress(
        update: suspend () -> JourneyProgress,
        onSaved: (JourneyProgress) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                onSaved(update())
            } catch (_: IOException) {
                mutableState.update { it.copy(progressFailure = ProgressFailure.Save) }
            }
        }
    }
}
