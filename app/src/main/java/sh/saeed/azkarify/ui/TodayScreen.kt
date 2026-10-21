@file:OptIn(ExperimentalMaterial3Api::class)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.data.Journey
import sh.saeed.azkarify.services.PrayerSchedule
import sh.saeed.azkarify.services.ScheduledPrayer
import sh.saeed.azkarify.ui.components.AzButton
import sh.saeed.azkarify.ui.components.AzButtonVariant
import sh.saeed.azkarify.ui.components.AzCard
import sh.saeed.azkarify.ui.components.AzEyebrow
import sh.saeed.azkarify.ui.components.AzIcon
import sh.saeed.azkarify.ui.components.AzJourneyCard
import sh.saeed.azkarify.ui.components.AzOrnament
import sh.saeed.azkarify.ui.components.AzOrnamentShape
import sh.saeed.azkarify.ui.components.AzPointsBadge
import sh.saeed.azkarify.ui.components.AzPrayerStrip
import sh.saeed.azkarify.ui.components.AzPrayerTime
import sh.saeed.azkarify.ui.components.AzProgressTrack
import sh.saeed.azkarify.ui.components.AzSectionHeader
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.components.AzTone
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzSpace

/** A minute ticker for screens that show prayer times; it stops when the screen leaves. */
@Composable
internal fun rememberMinute(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((60_000 - System.currentTimeMillis() % 60_000).milliseconds)
            now = System.currentTimeMillis()
        }
    }
    return now
}

@Immutable
internal data class PrayerDay(
    val times: List<ScheduledPrayer>,
    /** The next prayer, possibly tomorrow's Fajr. */
    val next: ScheduledPrayer?,
)

internal fun prayerDay(state: AppState, now: Long): PrayerDay? {
    val config = state.prayerConfiguration ?: return null
    val times = PrayerSchedule.calculate(config, Date(now))
    if (times.isEmpty()) return null
    val next =
        times.firstOrNull { it.time.time > now }
            ?: PrayerSchedule.calculate(config, Date(now + 86_400_000)).firstOrNull()
    return PrayerDay(times, next)
}

internal fun prayerName(id: String, state: AppState) =
    when (id) {
        "fajr" -> state.text("Fajr")
        "dhuhr" -> state.text("Dhuhr")
        "asr" -> state.text("Asr")
        "maghrib" -> state.text("Maghrib")
        else -> state.text("Isha")
    }

internal fun journeySymbol(journey: Journey) =
    when (journey.prayer ?: journey.id) {
        "morning", "fajr", "maghrib" -> AzSymbol.sunrise
        "going-out" -> AzSymbol.door
        "evening" -> AzSymbol.sunrise
        "sleep", "isha" -> AzSymbol.night
        else -> AzSymbol.sunny
    }

@Composable
internal fun TodayScreen(
    state: AppState,
    model: AppViewModel,
    day: String,
    onJourney: (Journey) -> Unit,
    onLocation: () -> Unit,
    onPrayerTimes: () -> Unit,
    onPoints: () -> Unit,
) {
    val colors = Az.colors
    val now = rememberMinute()
    val prayers = remember(state.prayerConfiguration, now) { prayerDay(state, now) }
    val is24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val progress = model.progress
    // Read so a saved step refreshes the screen.
    state.progressVersion
    val completed = state.journeys.sumOf { progress.completedCount(day, it) }
    val total = state.journeys.sumOf { it.requiredSteps.size }
    val nextJourney =
        prayers
            ?.times
            ?.firstOrNull { it.time.time > now }
            ?.let { prayer ->
                state.journeys.firstOrNull { it.prayer == prayer.id && !progress.complete(day, it) }
            }
    Scaffold(
        containerColor = colors.paper,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(
                        onClick = onLocation,
                        modifier = Modifier.padding(start = 4.dp),
                        shape = CircleShape,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AzIcon(AzSymbol.location, null, size = 20.dp, tint = colors.ink)
                            Text(
                                state.prayerConfiguration?.displayLocationName(state.language)
                                    ?: state.text("Set location"),
                                style = Az.type.callout.copy(fontWeight = Az.type.headline.fontWeight),
                                color = colors.ink,
                                maxLines = 1,
                            )
                            AzIcon(AzSymbol.expand, null, size = 18.dp, tint = colors.ink)
                        }
                    }
                },
                actions = {
                    AzPointsBadge(
                        state.number(progress.points),
                        state.text("Your points"),
                        onPoints,
                        Modifier.padding(end = AzSpace.xs),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.paper),
            )
        },
    ) { insets ->
        LazyColumn(
            Modifier.fillMaxSize().testTag("todayList"),
            contentPadding =
                PaddingValues(
                    start = AzSpace.gutter,
                    end = AzSpace.gutter,
                    top = insets.calculateTopPadding() + AzSpace.xs,
                    bottom = insets.calculateBottomPadding() + AzSpace.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(AzSpace.md),
        ) {
            item(key = "hero", contentType = "hero") {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AzSpace.xs)) {
                        AzEyebrow(state.dateEyebrow(LocalDate.parse(day)))
                        Text(
                            state.text("A little,\nevery day."),
                            Modifier.semantics { heading() },
                            style = Az.type.display,
                            color = colors.ink,
                        )
                    }
                    AzOrnament(
                        AzOrnamentShape.Star,
                        colors.spark,
                        Modifier.padding(top = 14.dp),
                        size = 64.dp,
                        spin = true,
                    )
                }
            }
            item(key = "progress", contentType = "progress") {
                val percent = if (total > 0) completed * 100 / total else 0
                Column(
                    Modifier
                        .padding(top = AzSpace.xs)
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AzSpace.xs)) {
                        Text(
                            state.percent(percent),
                            style = Az.type.title2.copy(fontFeatureSettings = "tnum"),
                            color = colors.ink,
                        )
                        Text(
                            state.text("of today's journeys complete"),
                            Modifier.padding(bottom = 2.dp),
                            style = Az.type.callout,
                            color = colors.inkMuted,
                        )
                    }
                    AzProgressTrack(if (total > 0) completed.toFloat() / total else 0f)
                }
            }
            item(key = "prayer", contentType = "prayer") {
                if (prayers?.next != null && state.prayerConfiguration != null) {
                    val next = prayers.next
                    AzPrayerStrip(
                        eyebrow =
                            state.text("Next prayer") + " · " +
                                state.prayerConfiguration.displayLocationName(state.language),
                        nextName = prayerName(next.id, state),
                        remaining = state.remaining(((next.time.time - now) / 60_000).coerceAtLeast(0)),
                        times =
                            prayers.times.map {
                                AzPrayerTime(
                                    prayerName(it.id, state),
                                    state.clock(it.time, state.prayerConfiguration.timeZone, is24),
                                    past = it.time.time <= now && it.id != next.id,
                                    next = it.id == next.id,
                                )
                            },
                        onClick = onLocation,
                        modifier = Modifier.padding(top = AzSpace.xs),
                    )
                } else {
                    AzCard(
                        Modifier.padding(top = AzSpace.xs),
                        tone = AzTone.Sage,
                        stamped = true,
                        onClick = onLocation,
                    ) {
                        AzEyebrow(state.text("Prayer times"), color = colors.inkSoft)
                        Text(
                            state.text("Set your location for prayer times"),
                            style = Az.type.title3,
                            color = colors.ink,
                        )
                        AzButton(
                            state.text("Set location"),
                            onLocation,
                            Modifier.padding(top = AzSpace.xxs),
                            AzButtonVariant.Secondary,
                            small = true,
                            leading = AzSymbol.location,
                        )
                    }
                }
            }
            journeySection(
                state,
                model,
                day,
                prayers,
                nextJourney,
                key = "morning",
                eyebrow = state.text("01 / Morning"),
                title = state.text("Begin your day"),
                journeys = state.journeys.filter { it.prayer == null }.take(2),
                onJourney = onJourney,
            )
            journeySection(
                state,
                model,
                day,
                prayers,
                nextJourney,
                key = "prayers",
                eyebrow = state.text("02 / Through the day"),
                title = state.text("Your prayer journeys"),
                action = state.text("Prayer times") to onPrayerTimes,
                journeys = state.journeys.filter { it.prayer != null },
                onJourney = onJourney,
            )
            journeySection(
                state,
                model,
                day,
                prayers,
                nextJourney,
                key = "evening",
                eyebrow = state.text("03 / Evening"),
                title = state.text("As the day settles"),
                journeys = state.journeys.filter { it.prayer == null }.drop(2),
                onJourney = onJourney,
                ornament = true,
            )
        }
    }
}

private fun LazyListScope.journeySection(
    state: AppState,
    model: AppViewModel,
    day: String,
    prayers: PrayerDay?,
    nextJourney: Journey?,
    key: String,
    eyebrow: String,
    title: String,
    journeys: List<Journey>,
    onJourney: (Journey) -> Unit,
    action: Pair<String, () -> Unit>? = null,
    ornament: Boolean = false,
) {
    if (journeys.isEmpty()) return
    item(key = "header-$key", contentType = "header") {
        Row(Modifier.padding(top = AzSpace.lg), verticalAlignment = Alignment.Bottom) {
            AzSectionHeader(
                title,
                Modifier.weight(1f),
                eyebrow = eyebrow,
                action = action?.first,
                onAction = action?.second ?: {},
            )
            if (ornament) AzOrnament(AzOrnamentShape.Crescent, Az.colors.clay, size = 28.dp)
        }
    }
    journeys.forEach { journey ->
        item(key = journey.id, contentType = "journey") {
            val progress = model.progress
            val count = progress.completedCount(day, journey)
            val required = journey.requiredSteps.size
            val complete = progress.complete(day, journey)
            val time =
                journey.prayer?.let { id ->
                    prayers?.times?.firstOrNull { it.id == id }?.let {
                        PrayerSchedule.format(it.time, state.prayerConfiguration!!, state.language)
                    }
                }
            AzJourneyCard(
                title = journey.title,
                detail = time ?: journey.subtitle,
                symbol = journeySymbol(journey),
                fraction = if (required > 0) count.toFloat() / required else 0f,
                progressLabel = if (complete) state.text("Complete") else state.count(count, required),
                accessibilityValue =
                    if (complete) state.text("Complete")
                    else state.text("%1\$s of %2\$s steps completed")
                        .replace("%1\$s", state.number(count))
                        .replace("%2\$s", state.number(required)),
                onClick = { onJourney(journey) },
                complete = complete,
                detailIsTime = time != null,
                nextLabel = if (journey == nextJourney) state.text("Next") else null,
            )
        }
    }
}
