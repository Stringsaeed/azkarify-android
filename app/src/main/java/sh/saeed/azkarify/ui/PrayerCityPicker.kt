@file:OptIn(ExperimentalMaterial3Api::class)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.services.CatalogCountry
import sh.saeed.azkarify.services.CatalogDatasetChanged
import sh.saeed.azkarify.services.CountryCatalogClient
import sh.saeed.azkarify.services.PrayerCityOption

@Composable
internal fun PrayerCityPicker(
    state: AppState,
    bundledCities: List<PrayerCityOption>,
    initialCountry: String?,
    onSelect: (PrayerCityOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val client = remember(context) { CountryCatalogClient.configured(context) }
    val fallbackCountries = remember(bundledCities) {
        bundledCities.map(PrayerCityOption::countryCode).distinct().map { code ->
            val locale = Locale.Builder().setRegion(code).build()
            CatalogCountry(
                code = code,
                englishName = locale.getDisplayCountry(Locale.ENGLISH),
                arabicName = locale.getDisplayCountry(Locale.forLanguageTag("ar")),
                flag = "",
            )
        }
    }
    var countryCode by rememberSaveable { mutableStateOf(initialCountry) }
    var countrySearch by rememberSaveable { mutableStateOf("") }
    var citySearch by rememberSaveable { mutableStateOf("") }
    var countries by remember { mutableStateOf(fallbackCountries) }
    var countriesLoading by remember { mutableStateOf(false) }
    var countriesFailed by remember { mutableStateOf(false) }
    var countryRetry by remember { mutableIntStateOf(0) }
    var cities by remember { mutableStateOf<List<PrayerCityOption>>(emptyList()) }
    var citiesLoading by remember { mutableStateOf(false) }
    var citiesFailed by remember { mutableStateOf(false) }
    var cityRetry by remember { mutableIntStateOf(0) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val palette = LocalPalette.current

    LaunchedEffect(client, state.language, countryRetry) {
        if (client == null) return@LaunchedEffect
        countriesLoading = true
        try {
            countries = client.countries(state.language)
            countriesFailed = false
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            countries = fallbackCountries
            countriesFailed = true
        } finally {
            countriesLoading = false
        }
    }

    LaunchedEffect(client, countryCode, citySearch, state.language, cityRetry) {
        val selected = countryCode ?: return@LaunchedEffect
        val query = citySearch.trim()
        val fallback = bundledCities.filter { city ->
            city.countryCode == selected &&
                (query.isEmpty() || city.name.contains(query, ignoreCase = true) ||
                    city.arabicName.contains(query, ignoreCase = true))
        }
        if (query.length == 1 || client == null) {
            cities = if (query.length == 1 && client != null) emptyList() else fallback
            nextCursor = null
            citiesFailed = false
            return@LaunchedEffect
        }
        if (query.isNotEmpty()) delay(350)
        citiesLoading = true
        try {
            val page = client.cities(selected, state.language, query)
            cities = page.items
            nextCursor = page.nextCursor
            citiesFailed = false
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            cities = fallback
            nextCursor = null
            citiesFailed = true
        } finally {
            citiesLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = palette.background,
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (countryCode != null)
                    TextButton(onClick = { countryCode = null }) { Text(state.text("Back")) }
                Text(
                    state.text(if (countryCode == null) "Choose a country" else "Choose a city"),
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, state.text("Close")) }
            }
            if (countriesFailed || citiesFailed || client == null) {
                Text(
                    state.text("Popular cities are available offline. Enter coordinates for another place."),
                    color = palette.secondary,
                )
            }
            if (countryCode == null) {
                OutlinedTextField(
                    countrySearch,
                    { countrySearch = it },
                    label = { Text(state.text("Search countries")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (countriesLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f)) {
                    items(
                        countries.filter { country ->
                            val query = countrySearch.trim()
                            query.isEmpty() || country.code.contains(query, ignoreCase = true) ||
                                country.englishName.contains(query, ignoreCase = true) ||
                                country.arabicName.contains(query, ignoreCase = true)
                        },
                        key = CatalogCountry::code,
                    ) { country ->
                        ListItem(
                            onClick = {
                                countryCode = country.code
                                citySearch = ""
                            },
                            modifier = Modifier.testTag("country.${country.code}"),
                            supportingContent = { Text(country.code) },
                        ) {
                            Text("${country.flag} ${country.displayName(state.language)}".trim())
                        }
                    }
                }
                if (countriesFailed)
                    TextButton(onClick = { countryRetry++ }) { Text(state.text("Retry")) }
            } else {
                OutlinedTextField(
                    citySearch,
                    { citySearch = it },
                    label = { Text(state.text("Search cities")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (citySearch.trim().length == 1 && client != null)
                    Text(state.text("Enter at least two characters to search cities."))
                if (citiesLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f)) {
                    items(cities, key = { it.id }) { city ->
                        ListItem(
                            onClick = { onSelect(city) },
                            modifier = Modifier.testTag("city.${city.countryCode}.${city.name}"),
                            supportingContent = { Text(city.countryCode) },
                        ) {
                            Text(city.displayName(state.language))
                        }
                    }
                    if (nextCursor != null && client != null) {
                        item {
                            TextButton(
                                onClick = {
                                    val selected = countryCode ?: return@TextButton
                                    val cursor = nextCursor ?: return@TextButton
                                    val query = citySearch.trim()
                                    scope.launch {
                                        citiesLoading = true
                                        try {
                                            val page = client.cities(selected, state.language, query, cursor)
                                            if (countryCode == selected && citySearch.trim() == query) {
                                                cities = (cities + page.items).distinctBy(PrayerCityOption::id)
                                                nextCursor = page.nextCursor
                                            }
                                        } catch (_: CatalogDatasetChanged) {
                                            try {
                                                val first = client.cities(selected, state.language, query)
                                                if (countryCode == selected && citySearch.trim() == query) {
                                                    cities = first.items
                                                    nextCursor = first.nextCursor
                                                }
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (_: Exception) {
                                                citiesFailed = true
                                            }
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            citiesFailed = true
                                        } finally {
                                            citiesLoading = false
                                        }
                                    }
                                },
                                enabled = !citiesLoading,
                            ) {
                                Text(state.text("Load more cities"))
                            }
                        }
                    }
                }
                if (citiesFailed)
                    TextButton(onClick = { cityRetry++ }) { Text(state.text("Retry")) }
            }
        }
    }
}
