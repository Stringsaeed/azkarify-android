@file:OptIn(ExperimentalMaterial3Api::class)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.services.CatalogCountry
import sh.saeed.azkarify.services.CatalogDatasetChanged
import sh.saeed.azkarify.services.CountryCatalogClient
import sh.saeed.azkarify.services.PrayerCityOption
import sh.saeed.azkarify.ui.components.AzButton
import sh.saeed.azkarify.ui.components.AzButtonVariant
import sh.saeed.azkarify.ui.components.AzChevron
import sh.saeed.azkarify.ui.components.AzIcon
import sh.saeed.azkarify.ui.components.AzIconButton
import sh.saeed.azkarify.ui.components.AzIconButtonVariant
import sh.saeed.azkarify.ui.components.AzListRow
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.components.azGroupItem
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzSpace

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
    val colors = Az.colors

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
        containerColor = colors.paper,
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = AzSpace.gutter),
            verticalArrangement = Arrangement.spacedBy(AzSpace.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AzSpace.xs)) {
                if (countryCode != null) AzIconButton(AzSymbol.back, state.text("Back"), { countryCode = null })
                Text(
                    state.text(if (countryCode == null) "Choose a country" else "Choose a city"),
                    modifier = Modifier.weight(1f),
                    style = Az.type.title2,
                    color = colors.ink,
                )
                AzIconButton(AzSymbol.close, state.text("Close"), onDismiss, variant = AzIconButtonVariant.Filled)
            }
            if (countriesFailed || citiesFailed || client == null) {
                Text(
                    state.text("Popular cities are available offline. Enter coordinates for another place."),
                    style = Az.type.footnote,
                    color = colors.inkMuted,
                )
            }
            if (countryCode == null) {
                SearchField(countrySearch, { countrySearch = it }, state.text("Search countries"))
                if (countriesLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.paperSunk)
                val visible =
                    remember(countries, countrySearch) {
                        val query = countrySearch.trim()
                        countries.filter { country ->
                            query.isEmpty() || country.code.contains(query, ignoreCase = true) ||
                                country.englishName.contains(query, ignoreCase = true) ||
                                country.arabicName.contains(query, ignoreCase = true)
                        }
                    }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = AzSpace.xl)) {
                    itemsIndexed(visible, key = { _, it -> it.code }, contentType = { _, _ -> "country" }) { index, country ->
                        // Flags are emoji, which the design system leaves out.
                        AzListRow(
                            country.displayName(state.language),
                            Modifier.azGroupItem(index, visible.size, colors).testTag("country.${country.code}"),
                            onClick = {
                                countryCode = country.code
                                citySearch = ""
                            },
                            subtitle = country.code,
                            divider = index > 0,
                            inset = AzSpace.md,
                            trailing = { AzChevron() },
                        )
                    }
                }
                if (countriesFailed)
                    AzButton(state.text("Retry"), { countryRetry++ }, variant = AzButtonVariant.Text)
            } else {
                SearchField(citySearch, { citySearch = it }, state.text("Search cities"))
                if (citySearch.trim().length == 1 && client != null)
                    Text(state.text("Enter at least two characters to search cities."), style = Az.type.footnote, color = colors.inkMuted)
                if (citiesLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.paperSunk)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = AzSpace.xl)) {
                    itemsIndexed(cities, key = { _, it -> it.id }, contentType = { _, _ -> "city" }) { index, city ->
                        AzListRow(
                            city.displayName(state.language),
                            Modifier.azGroupItem(index, cities.size, colors).testTag("city.${city.countryCode}.${city.name}"),
                            onClick = { onSelect(city) },
                            divider = index > 0,
                            inset = AzSpace.md,
                        )
                    }
                    if (nextCursor != null && client != null) {
                        item {
                            AzButton(
                                state.text("Load more cities"),
                                {
                                    val selected = countryCode ?: return@AzButton
                                    val cursor = nextCursor ?: return@AzButton
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
                                Modifier.padding(top = AzSpace.sm),
                                AzButtonVariant.Text,
                                enabled = !citiesLoading,
                            )
                        }
                    }
                }
                if (citiesFailed)
                    AzButton(state.text("Retry"), { cityRetry++ }, variant = AzButtonVariant.Text)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, label: String) {
    val colors = Az.colors
    TextField(
        value,
        onChange,
        Modifier.fillMaxWidth(),
        placeholder = { Text(label, style = Az.type.body, color = colors.inkMuted) },
        leadingIcon = { AzIcon(AzSymbol.search, null, tint = colors.inkMuted) },
        singleLine = true,
        textStyle = Az.type.body,
        shape = CircleShape,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colors.paperSunk,
                unfocusedContainerColor = colors.paperSunk,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                cursorColor = colors.accent,
            ),
    )
}
