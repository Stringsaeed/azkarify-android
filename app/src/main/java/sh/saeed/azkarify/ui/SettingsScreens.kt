@file:OptIn(ExperimentalMaterial3Api::class)

package sh.saeed.azkarify.ui

import android.Manifest
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import sh.saeed.azkarify.data.*
import sh.saeed.azkarify.services.*
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import sh.saeed.azkarify.ui.components.*
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzAccent
import sh.saeed.azkarify.ui.theme.AzRadius
import sh.saeed.azkarify.ui.theme.AzSpace

/** A titled group on a settings screen: `eyebrow` label, then the rows. */
@Composable
internal fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AzSpace.xs)) {
        AzEyebrow(title, Modifier.padding(horizontal = AzSpace.md), color = Az.colors.inkMuted)
        AzListGroup(content = content)
    }
}

internal fun accentName(accent: AzAccent, state: AppState) =
    when (accent) {
        AzAccent.Terracotta -> state.text("Terracotta")
        AzAccent.Ember -> state.text("Ember")
        AzAccent.Teal -> state.text("Teal")
        AzAccent.Dusk -> state.text("Dusk")
        AzAccent.Forest -> state.text("Forest")
    }

@Composable
fun SettingsScreen(
    state: AppState,
    model: AppViewModel,
    onPrayerTimes: () -> Unit,
    onLocation: () -> Unit,
    onIntro: () -> Unit,
) {
    val colors = Az.colors
    val uriHandler = LocalUriHandler.current
    var offering by remember { mutableStateOf<Offering?>(null) }
    var paywallError by remember { mutableStateOf<String?>(null) }
    var loadingPaywall by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }
    val accent = AzAccent.of(state.accent)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = AzSpace.md)
            .padding(top = AzSpace.md, bottom = AzSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AzSpace.lg),
    ) {
        Text(
            state.text("Settings"),
            Modifier.padding(horizontal = AzSpace.xxs).semantics { heading() },
            style = Az.type.title1,
            color = colors.ink,
        )
        SettingsSection(state.text("Appearance")) {
            Column(Modifier.padding(AzSpace.md), verticalArrangement = Arrangement.spacedBy(AzSpace.sm)) {
                Text(state.text("Theme"), style = Az.type.rowTitle, color = colors.ink)
                AzSegmented(
                    listOf(
                        "system" to state.text("System"),
                        "light" to state.text("Day"),
                        "dark" to state.text("Night"),
                    ),
                    state.theme,
                    model::setTheme,
                )
            }
            Column(
                Modifier.dividerAbove(colors.line).padding(AzSpace.md),
                verticalArrangement = Arrangement.spacedBy(AzSpace.sm),
            ) {
                Row {
                    Text(state.text("Accent"), Modifier.weight(1f), style = Az.type.rowTitle, color = colors.ink)
                    Text(accentName(accent, state), style = Az.type.callout, color = colors.inkMuted)
                }
                AzSwatchRow(
                    AzAccent.entries.map { AzSwatch(it, accentName(it, state)) },
                    accent,
                    { model.setAccent(it.id) },
                )
            }
            Box {
                AzListRow(
                    state.text("Language"),
                    onClick = { languageMenu = true },
                    subtitle = if (state.language == "ar") "العربية" else "English",
                    divider = true,
                    trailing = { AzIcon(AzSymbol.expand, null, tint = colors.inkSubtle) },
                )
                AppDropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                    listOf("en" to "English", "ar" to "العربية").forEach { (id, title) ->
                        AppDropdownMenuItem(
                            text = { Text(title) },
                            selected = id == state.language,
                            onClick = {
                                languageMenu = false
                                model.setLanguage(id)
                            },
                        )
                    }
                }
            }
        }
        SettingsSection(state.text("Reading")) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AzSpace.md, vertical = AzSpace.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.text("Text size"), Modifier.weight(1f), style = Az.type.rowTitle, color = colors.ink)
                AzReaderSizeControl(
                    state.readerScale,
                    model::setReaderScale,
                    state.text("Smaller text"),
                    state.text("Larger text"),
                )
            }
            AzListRow(
                state.text("Transliteration"),
                Modifier.toggleable(state.transliteration, role = Role.Switch, onValueChange = model::setTransliteration),
                subtitle = state.text("Shown under the Arabic in English."),
                divider = true,
                trailing = { AzSwitch(state.transliteration, null) },
            )
        }
        SettingsSection(state.text("Prayer times")) {
            AzListRow(
                state.text("Location"),
                onClick = onLocation,
                subtitle = state.prayerConfiguration?.displayLocationName(state.language) ?: state.text("Not set"),
                leading = { AzIconWell(AzSymbol.location, small = true) },
                inset = 64.dp,
                trailing = { AzChevron() },
            )
            AzListRow(
                state.text("Calculation method"),
                onClick = onPrayerTimes,
                subtitle =
                    state.prayerConfiguration?.let { config ->
                        prayerMethods.firstOrNull { it.first == config.method }?.let { state.text(it.second) }
                    } ?: state.text("Muslim World League"),
                divider = true,
                inset = 64.dp,
                leading = { AzIconWell(AzSymbol.schedule, small = true) },
                trailing = { AzChevron() },
            )
        }
        SettingsSection(state.text("Reminders")) {
            ReminderRow("morning", state, model)
            ReminderRow("evening", state, model, divider = true)
        }
        SettingsSection(state.text("About")) {
            AzListRow(
                state.text("View introduction"),
                onClick = onIntro,
                leading = { AzIconWell(AzSymbol.info, small = true) },
                inset = 64.dp,
                trailing = { AzChevron() },
            )
            AzListRow(
                state.text("Support the developer"),
                onClick = {
                    if (!RevenueCatService.isConfigured || loadingPaywall) return@AzListRow
                    loadingPaywall = true
                    Purchases.sharedInstance.getOfferings(
                        object : ReceiveOfferingsCallback {
                            override fun onError(error: PurchasesError) {
                                loadingPaywall = false
                                paywallError = error.message
                            }

                            override fun onReceived(offerings: Offerings) {
                                loadingPaywall = false
                                val current = offerings.current
                                if (current == null)
                                    paywallError = state.text("No support offering is available yet.")
                                else offering = current
                            }
                        }
                    )
                },
                divider = true,
                inset = 64.dp,
                leading = { AzIconWell(AzSymbol.favorite, small = true) },
                trailing = { AzChevron() },
            )
            AzListRow(
                state.text("Privacy policy"),
                onClick = { uriHandler.openUri("https://azkarify.saeed.sh/android/policy") },
                divider = true,
                inset = 64.dp,
                leading = { AzIconWell(AzSymbol.privacy, small = true) },
                trailing = { AzChevron() },
            )
        }
        AzCard(tone = AzTone.Forest, compact = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AzSpace.sm)) {
                AzOrnament(AzOrnamentShape.Sparkle, colors.onForest, size = 22.dp)
                Text(
                    state.text("Your progress stays on this device."),
                    style = Az.type.callout,
                    color = colors.onForest,
                )
            }
        }
    }
    offering?.let { current ->
        PaywallDialog(
            paywallDialogOptions =
                PaywallDialogOptions.Builder()
                    .setOffering(current)
                    .setDismissRequest { offering = null }
                    .build()
        )
    }
    paywallError?.let { error ->
        AlertDialog(
            onDismissRequest = { paywallError = null },
            title = { Text(state.text("Paywall unavailable")) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { paywallError = null }) { Text(state.text("OK")) }
            },
        )
    }
}

/** Hairline above a block inside a list group. */
internal fun Modifier.dividerAbove(color: androidx.compose.ui.graphics.Color) =
    drawBehind { drawLine(color, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }

@Composable
fun ChoiceRow(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    divider: Boolean = false,
    change: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AzListRow(
            label,
            onClick = { expanded = true },
            subtitle = options.firstOrNull { it.first == value }?.second ?: value,
            divider = divider,
            trailing = { AzIcon(AzSymbol.expand, null, tint = Az.colors.inkSubtle) },
        )
        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, title) ->
                AppDropdownMenuItem(
                    text = { Text(title) },
                    selected = id == value,
                    onClick = {
                        change(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Settings › Prayer times: location, calculation method, madhab and manual coordinates. */
@Composable
fun PrayerSettingsScreen(state: AppState, model: AppViewModel, onBack: () -> Unit) {
    Scaffold(
        containerColor = Az.colors.paper,
        topBar = {
            TopAppBar(
                title = { Text(state.text("Prayer times"), style = Az.type.headline) },
                navigationIcon = { AzIconButton(AzSymbol.back, state.text("Back"), onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Az.colors.paper, titleContentColor = Az.colors.ink),
            )
        },
    ) { insets ->
        PrayerLocationForm(state, model, compact = false, Modifier.padding(insets))
    }
}

/** The prayer location sheet: device location or a city, saved on this device only. */
@Composable
fun PrayerLocationSheet(state: AppState, model: AppViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = Az.colors.paper,
    ) {
        Row(Modifier.padding(horizontal = AzSpace.gutter), verticalAlignment = Alignment.CenterVertically) {
            Text(state.text("Prayer location"), Modifier.weight(1f), style = Az.type.title2, color = Az.colors.ink)
            AzIconButton(AzSymbol.close, state.text("Close"), onDismiss, variant = AzIconButtonVariant.Filled)
        }
        PrayerLocationForm(state, model, compact = true, onSaved = onDismiss)
    }
}

@Composable
private fun PrayerLocationForm(
    state: AppState,
    model: AppViewModel,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onSaved: () -> Unit = {},
) {
    val config = state.prayerConfiguration
    var name by rememberSaveable { mutableStateOf(config?.locationName ?: "") }
    var lat by rememberSaveable { mutableStateOf(config?.latitude?.toString() ?: "") }
    var lon by rememberSaveable { mutableStateOf(config?.longitude?.toString() ?: "") }
    var zone by rememberSaveable { mutableStateOf(config?.timeZone ?: ZoneId.systemDefault().id) }
    var method by rememberSaveable { mutableStateOf(config?.method ?: "MUSLIM_WORLD_LEAGUE") }
    var hanafi by rememberSaveable { mutableStateOf(config?.hanafi ?: false) }
    var adjustment by rememberSaveable { mutableStateOf(config?.ishaAdjustment?.toString() ?: "0") }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bundledCities = remember(context) { PrayerCityCatalog.bundled(context.assets) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf<PrayerCityOption?>(null) }
    val scope = rememberCoroutineScope()
    var signal by remember { mutableStateOf<CancellationSignal?>(null) }
    DisposableEffect(Unit) { onDispose { signal?.cancel() } }
    fun located(latitude: Double, longitude: Double) {
        lat = latitude.toString()
        lon = longitude.toString()
        name = state.text("Current location")
        zone = ZoneId.systemDefault().id
        method = PrayerSchedule.recommendedMethod(zone)
        locating = false
    }
    fun locate() {
        locating = true
        error = null
        val manager = context.getSystemService(LocationManager::class.java)
        val provider =
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                LocationManager.GPS_PROVIDER
            else LocationManager.NETWORK_PROVIDER
        if (!manager.isProviderEnabled(provider)) {
            locating = false
            error = state.text("Enable location services and try again.")
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val token = CancellationSignal()
                signal = token
                manager.getCurrentLocation(provider, token, context.mainExecutor) { location ->
                    if (location == null) {
                        locating = false
                        error =
                            state.text(
                                "Could not get your location. Try again or enter coordinates."
                            )
                    } else located(location.latitude, location.longitude)
                }
                scope.launch {
                    kotlinx.coroutines.delay(20_000.milliseconds)
                    if (locating) {
                        token.cancel()
                        locating = false
                        error = state.text("Location request timed out.")
                    }
                }
            } else {
                val location = manager.getLastKnownLocation(provider)
                if (location != null && System.currentTimeMillis() - location.time < 300_000)
                    located(location.latitude, location.longitude)
                else {
                    locating = false
                    error =
                        state.text(
                            "No recent location. Enter coordinates or try again after opening Maps."
                        )
                }
            }
        } catch (_: SecurityException) {
            locating = false
            error = state.text("Location permission is required.")
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            result ->
            if (result.values.any { it }) locate()
            else
                error =
                    state.text(
                        "Location permission was denied. You can enter coordinates manually."
                    )
        }
    val savedCity = config?.let { saved ->
        val id = saved.cityId
        val code = saved.countryCode
        val english = saved.englishLocationName
        if (id == null || code == null || english == null) null
        else PrayerCityOption(
            name = english,
            arabicName = saved.arabicLocationName ?: english,
            countryCode = code,
            latitude = saved.latitude,
            longitude = saved.longitude,
            timeZone = saved.timeZone,
            method = saved.method,
            id = id,
        )
    }
    val matchedCity =
        (listOfNotNull(selectedCity, savedCity) + bundledCities).firstOrNull { city ->
            name.trim() in setOf(city.name, city.arabicName) &&
                lat.toDoubleOrNull() == city.latitude &&
                lon.toDoubleOrNull() == city.longitude &&
                zone.trim() == city.timeZone &&
                method == city.method
        }
    val draft =
        lat.toDoubleOrNull()?.let { latitude ->
            lon.toDoubleOrNull()?.let { longitude ->
                adjustment.toIntOrNull()?.let {
                    PrayerConfiguration(
                        name.trim(),
                        latitude,
                        longitude,
                        zone.trim(),
                        method,
                        hanafi,
                        it,
                        countryCode = matchedCity?.countryCode,
                        cityId = matchedCity?.id,
                        englishLocationName = matchedCity?.name,
                        arabicLocationName = matchedCity?.arabicName,
                        usesDeviceLocation =
                            matchedCity == null && name.trim() in setOf("Current location", "الموقع الحالي"),
                    )
                }
            }
        }
    val colors = Az.colors
    fun save() {
        draft?.takeIf { it.isValid }?.let {
            model.savePrayer(it)
            saved = true
            error = null
            onSaved()
        }
    }
    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.paperRaised,
            unfocusedContainerColor = colors.paperRaised,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.lineStrong,
            focusedLabelColor = colors.accent,
            cursorColor = colors.accent,
        )
    @Composable
    fun Field(value: String, label: String, onChange: (String) -> Unit, hint: String? = null) =
        OutlinedTextField(
            value,
            {
                onChange(it)
                saved = false
            },
            label = { Text(label) },
            supportingText = hint?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(AzRadius.sm),
            colors = fieldColors,
        )
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AzSpace.gutter)
            .padding(top = AzSpace.md, bottom = AzSpace.xxl)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(AzSpace.lg),
    ) {
        AzCard(tone = AzTone.Sage, compact = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AzIcon(AzSymbol.privacy, null, tint = colors.inkSoft)
                Text(
                    state.text("Prayer times are calculated on your phone. Your location is never uploaded."),
                    style = Az.type.callout,
                    color = colors.inkSoft,
                )
            }
        }
        AzButton(
            state.text(if (locating) "Finding your location…" else "Use current location"),
            {
                permission.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
            },
            Modifier.fillMaxWidth(),
            enabled = !locating,
            leading = AzSymbol.myLocation,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AzSpace.sm)) {
            HorizontalDivider(Modifier.weight(1f), color = colors.line)
            Text(state.text("or choose a city"), style = Az.type.footnote, color = colors.inkMuted)
            HorizontalDivider(Modifier.weight(1f), color = colors.line)
        }
        AzListGroup {
            AzListRow(
                matchedCity?.displayName(state.language) ?: name.ifBlank { state.text("Choose a city") },
                onClick = { showCityPicker = true },
                subtitle = state.text("City"),
                subtitleFirst = true,
                selected = matchedCity != null,
                inset = 64.dp,
                leading = {
                    AzIconWell(
                        AzSymbol.location,
                        small = true,
                        state = if (matchedCity != null) AzWellState.On else AzWellState.Idle,
                    )
                },
                trailing = { AzChevron() },
            )
        }
        if (!compact) {
            SettingsSection(state.text("Calculation")) {
                ChoiceRow(
                    state.text("Calculation method"),
                    method,
                    prayerMethods.map { it.first to state.text(it.second) },
                ) {
                    method = it
                    saved = false
                }
                ChoiceRow(
                    state.text("Asr calculation"),
                    if (hanafi) "hanafi" else "shafi",
                    listOf(
                        "shafi" to state.text("Shafi, Maliki, Hanbali"),
                        "hanafi" to state.text("Hanafi, later Asr"),
                    ),
                    divider = true,
                ) {
                    hanafi = it == "hanafi"
                    saved = false
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(AzSpace.xs)) {
                AzEyebrow(state.text("Location"), Modifier.padding(horizontal = AzSpace.md), color = colors.inkMuted)
                Field(name, state.text("Location name"), { name = it })
                Field(lat, state.text("Latitude"), { lat = it })
                Field(lon, state.text("Longitude"), { lon = it })
                // The zone is explicit because location alone does not determine a timezone.
                Field(zone, state.text("Time zone"), { zone = it }, "Asia/Dubai · Africa/Cairo · Europe/London")
                Field(adjustment, state.text("Isha adjustment (minutes)"), { adjustment = it }, "−60 … +60")
            }
        } else {
            Text(
                state.text("City coordinates represent the city centre. You can change the calculation method in Settings › Prayer times."),
                Modifier.padding(horizontal = AzSpace.xxs),
                style = Az.type.footnote,
                color = colors.inkMuted,
            )
        }
        error?.let { Text(it, style = Az.type.callout, color = MaterialTheme.colorScheme.error) }
        AzButton(
            when {
                saved -> state.text("Saved")
                compact -> state.text("Save location")
                else -> state.text("Save")
            },
            ::save,
            Modifier.fillMaxWidth(),
            AzButtonVariant.Accent,
            enabled = draft?.isValid == true,
        )
        if (draft?.isValid != true && !compact)
            Text(
                state.text("Enter a name, valid coordinates, time zone, and adjustment to save."),
                style = Az.type.footnote,
                color = colors.inkMuted,
            )
        if (config != null && !compact) {
            val times = remember(config) { PrayerSchedule.calculate(config, Date()) }
            SettingsSection(state.text("Today's prayer times")) {
                times.forEachIndexed { index, prayer ->
                    AzListRow(
                        prayerTitle(prayer.id, state),
                        divider = index > 0,
                        trailing = {
                            Text(
                                PrayerSchedule.format(prayer.time, config, state.language),
                                style = Az.type.headline.copy(fontFeatureSettings = "tnum"),
                                color = colors.ink,
                            )
                        },
                    )
                }
            }
            AzButton(
                state.text("Clear location"),
                {
                    model.clearPrayer()
                    saved = false
                },
                variant = AzButtonVariant.Text,
            )
        }
    }
    if (showCityPicker)
        PrayerCityPicker(
            state = state,
            bundledCities = bundledCities,
            initialCountry = selectedCity?.countryCode ?: config?.countryCode,
            onSelect = { city ->
                selectedCity = city
                name = city.displayName(state.language)
                lat = city.latitude.toString()
                lon = city.longitude.toString()
                zone = city.timeZone
                method = city.method
                saved = false
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false },
        )
}

private fun prayerTitle(id: String, state: AppState) =
    when (id) {
        "fajr" -> state.text("Fajr")
        "dhuhr" -> state.text("Dhuhr")
        "asr" -> state.text("Asr")
        "maghrib" -> state.text("Maghrib")
        else -> state.text("Isha")
    }

@Composable
private fun ReminderRow(kind: String, state: AppState, model: AppViewModel, divider: Boolean = false) {
    val context = LocalContext.current
    val prefs = model.preferences
    var enabled by remember { mutableStateOf(prefs.getBoolean("reminder.$kind.enabled", false)) }
    var hour by remember {
        mutableIntStateOf(prefs.getInt("reminder.$kind.hour", if (kind == "morning") 8 else 18))
    }
    var minute by remember { mutableIntStateOf(prefs.getInt("reminder.$kind.minute", 0)) }
    var denied by remember { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    fun set(value: Boolean) {
        enabled = value
        prefs.edit { putBoolean("reminder.$kind.enabled", value) }
        Reminders.schedule(context, kind)
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
            if (allowed) set(true) else denied = true
        }
    val colors = Az.colors
    AzListRow(
        state.text(if (kind == "morning") "Morning azkar" else "Evening azkar"),
        divider = divider,
        inset = 64.dp,
        leading = { AzIconWell(if (kind == "morning") AzSymbol.sunrise else AzSymbol.night, small = true) },
        trailing = {
            AssistChip(
                onClick = { showTimePicker = true },
                label = {
                    Text(
                        String.format(numberLocale(state.language), "%02d:%02d", hour, minute),
                        style = Az.type.callout.copy(fontFeatureSettings = "tnum"),
                    )
                },
                shape = RoundedCornerShape(AzRadius.xs + 2.dp),
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = colors.paperRaised,
                        labelColor = if (enabled) colors.ink else colors.inkMuted,
                    ),
                border = BorderStroke(1.dp, colors.lineStrong),
            )
            AzSwitch(
                enabled,
                { value ->
                    if (!value) set(false)
                    else if (Reminders.allowed(context)) set(true)
                    else if (Build.VERSION.SDK_INT >= 33)
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else denied = true
                },
            )
        },
    )
    if (showTimePicker) {
        val picker =
            rememberTimePickerState(
                hour,
                minute,
                android.text.format.DateFormat.is24HourFormat(context),
            )
        val compactHeight = LocalConfiguration.current.screenHeightDp < 400
        var inputMode by rememberSaveable { mutableStateOf(compactHeight) }
        val displayMode =
            if (inputMode || compactHeight) TimePickerDisplayMode.Input
            else TimePickerDisplayMode.Picker
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { TimePickerDialogDefaults.Title(displayMode) },
            confirmButton = {
                Button(
                    onClick = {
                        hour = picker.hour
                        minute = picker.minute
                        prefs.edit {
                            putInt("reminder.$kind.hour", hour)
                                .putInt("reminder.$kind.minute", minute)
                        }
                        Reminders.schedule(context, kind)
                        showTimePicker = false
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(state.text("Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(state.text("Close")) }
            },
            modeToggleButton =
                if (compactHeight) null
                else
                    ({
                        TimePickerDialogDefaults.DisplayModeToggle(
                            onDisplayModeChange = { inputMode = !inputMode },
                            displayMode = displayMode,
                        )
                    }),
        ) {
            if (displayMode == TimePickerDisplayMode.Input) TimeInput(picker)
            else TimePicker(picker)
        }
    }
    if (denied)
        AlertDialog(
            onDismissRequest = { denied = false },
            title = { Text(state.text("Notifications are disabled")) },
            text = {
                Text(state.text("Enable notifications in Android Settings to use reminders."))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        denied = false
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                ) {
                    Text(state.text("Settings"))
                }
            },
            dismissButton = {
                TextButton(onClick = { denied = false }) { Text(state.text("Close")) }
            },
        )
}
