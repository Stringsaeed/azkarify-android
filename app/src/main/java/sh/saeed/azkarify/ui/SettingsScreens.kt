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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import sh.saeed.azkarify.ui.theme.AzAccent

@Composable
fun SettingsScreen(state: AppState, model: AppViewModel, go: (Destination) -> Unit) {
    val p = LocalPalette.current
    val uriHandler = LocalUriHandler.current
    var accentPicker by remember { mutableStateOf(false) }
    var intro by remember { mutableStateOf(false) }
    var offering by remember { mutableStateOf<Offering?>(null) }
    var paywallError by remember { mutableStateOf<String?>(null) }
    var loadingPaywall by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SettingsGroup {
            ChoiceRow(
                state.text("Theme"),
                state.theme,
                listOf(
                    "system" to state.text("System"),
                    "light" to state.text("Light"),
                    "dark" to state.text("Dark"),
                ),
                inlineValue = true,
                change = model::setTheme,
            )
            HorizontalDivider(color = p.secondary.copy(alpha = .15f))
            ChoiceRow(
                state.text("Language"),
                state.language,
                listOf("en" to state.text("English"), "ar" to state.text("Arabic")),
                inlineValue = true,
                change = model::setLanguage,
            )
            HorizontalDivider(color = p.secondary.copy(alpha = .15f))
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clickable { accentPicker = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.text("Accent color"), Modifier.weight(1f), fontSize = 17.sp)
                Box(Modifier.size(22.dp).background(p.accent, CircleShape))
            }
        }
        Text(state.text("Reminders"), fontSize = 14.sp, color = p.secondary)
        SettingsGroup {
            ReminderRow("morning", state, model)
            HorizontalDivider(color = p.secondary.copy(alpha = .15f))
            ReminderRow("evening", state, model)
        }
        Text(state.text("Daily journeys"), fontSize = 14.sp, color = p.secondary)
        SettingsGroup {
            Text(
                state.text("Prayer times"),
                Modifier.fillMaxWidth().clickable { go(Destination.Prayer) }.padding(16.dp),
                fontSize = 17.sp,
                color = p.accent,
            )
        }
        SettingsGroup {
            Text(
                state.text("View introduction"),
                Modifier.fillMaxWidth().clickable { intro = true }.padding(16.dp),
                fontSize = 17.sp,
                color = p.accent,
            )
            HorizontalDivider(color = p.secondary.copy(alpha = .15f))
            TextButton(
                onClick = {
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
                                    paywallError =
                                        state.text("No support offering is available yet.")
                                else offering = current
                            }
                        }
                    )
                },
                enabled = RevenueCatService.isConfigured && !loadingPaywall,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
            ) {
                Text(
                    state.text("Support the developer"),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 17.sp,
                )
            }
        }
        SettingsGroup {
            TextButton(
                onClick = { uriHandler.openUri("https://azkarify.saeed.sh/android/policy") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
            ) {
                Text(
                    state.text("Privacy policy"),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 17.sp,
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
    if (accentPicker)
        ModalBottomSheet(
            onDismissRequest = { accentPicker = false },
            containerColor = p.background,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(state.text("Accent color"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                listOf(
                        "brown" to "Brown",
                        "saffron" to "Saffron",
                        "teal" to "Teal",
                        "blue" to "Blue",
                        "green" to "Green",
                    )
                    .forEach { (id, title) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable {
                                model.setAccent(id)
                                accentPicker = false
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(24.dp)
                                    .background(AzAccent.of(id).let { if (p.dark) it.night else it.day }, CircleShape)
                            )
                            Text(
                                state.text(title),
                                Modifier.padding(start = 12.dp).weight(1f),
                                fontSize = 17.sp,
                            )
                            if (state.accent == id) Text("✓", color = p.accent, fontSize = 22.sp)
                        }
                    }
            }
        }
    if (intro)
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { intro = false },
            properties =
                androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(color = p.background, modifier = Modifier.fillMaxSize()) {
                IntroScreen(state) { intro = false }
            }
        }
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    AppCard(content = content)
}

@Composable
fun ChoiceRow(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    inlineValue: Boolean = false,
    change: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ListItem(
            onClick = { expanded = true },
            supportingContent =
                if (inlineValue) null
                else ({ Text(options.firstOrNull { it.first == value }?.second ?: value) }),
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (inlineValue) {
                        Text(
                            options.firstOrNull { it.first == value }?.second ?: value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalPalette.current.secondary,
                        )
                    }
                    Icon(Icons.Outlined.ArrowDropDown, null)
                }
            },
            colors = ListItemDefaults.colors(containerColor = LocalPalette.current.background),
        ) {
            Text(label)
        }
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

@Composable
private fun ReminderRow(kind: String, state: AppState, model: AppViewModel) {
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
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            state.text(if (kind == "morning") "🌅 Morning reminder" else "🌙 Evening reminder"),
            Modifier.weight(1f),
            fontSize = 17.sp,
        )
        AssistChip(
            onClick = { showTimePicker = true },
            label = {
                Text(
                    String.format(Locale.forLanguageTag(state.language), "%02d:%02d", hour, minute)
                )
            },
        )
        Switch(
            enabled,
            onCheckedChange = { value ->
                if (!value) set(false)
                else if (Reminders.allowed(context)) set(true)
                else if (Build.VERSION.SDK_INT >= 33)
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else denied = true
            },
        )
    }
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

@Composable
fun PrayerSettingsScreen(state: AppState, model: AppViewModel) {
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
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = { showCityPicker = true }) {
            Text(state.text("Choose a city"))
        }
        TextButton(
            onClick = {
                permission.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                )
            },
            enabled = !locating,
        ) {
            Text(state.text(if (locating) "Finding your location…" else "Use current location"))
        }
        Text(state.text("Location"), fontSize = 14.sp, color = p.secondary)
        OutlinedTextField(
            name,
            {
                name = it
                saved = false
            },
            label = { Text(state.text("Location name")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            lat,
            {
                lat = it
                saved = false
            },
            label = { Text(state.text("Latitude")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            lon,
            {
                lon = it
                saved = false
            },
            label = { Text(state.text("Longitude")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        // The zone is explicit because location alone does not determine a timezone.
        OutlinedTextField(
            zone,
            {
                zone = it
                saved = false
            },
            label = { Text(state.text("Time zone")) },
            supportingText = { Text("Asia/Dubai · Africa/Cairo · Europe/London") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        SettingsGroup {
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
            ) {
                hanafi = it == "hanafi"
                saved = false
            }
        }
        OutlinedTextField(
            adjustment,
            {
                adjustment = it
                saved = false
            },
            label = { Text(state.text("Isha adjustment (minutes)")) },
            supportingText = { Text("−60 … +60") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            shapes = ButtonDefaults.shapes(),
            onClick = {
                draft
                    ?.takeIf { it.isValid }
                    ?.let {
                        model.savePrayer(it)
                        saved = true
                        error = null
                    }
            },
            enabled = draft?.isValid == true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(state.text(if (saved) "Saved" else "Save"))
        }
        if (draft?.isValid != true)
            Text(
                state.text("Enter a name, valid coordinates, time zone, and adjustment to save."),
                fontSize = 13.sp,
                color = p.secondary,
            )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (config != null) {
            Text(state.text("Today's prayer times"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            val times = remember(config) { PrayerSchedule.calculate(config, Date()) }
            times.forEach { prayer ->
                Row(Modifier.fillMaxWidth()) {
                    Text(prayerTitle(prayer.id, state), Modifier.weight(1f))
                    Text(
                        PrayerSchedule.format(prayer.time, config, state.language),
                        color = p.accent,
                    )
                }
            }
            TextButton(
                onClick = {
                    model.clearPrayer()
                    saved = false
                }
            ) {
                Text(state.text("Clear location"))
            }
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
