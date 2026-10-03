package com.stringsaeed.azkarify.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stringsaeed.azkarify.R
import com.stringsaeed.azkarify.data.*
import com.stringsaeed.azkarify.services.PrayerSchedule
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun JourneysScreen(state: AppState, model: AppViewModel, day: String, go: (Destination) -> Unit) {
    val p = LocalPalette.current
    val completed = state.journeys.sumOf { model.progress.completedCount(day, it) }
    val total = state.journeys.sumOf { it.requiredSteps.size }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(30_000.milliseconds)
        }
    }
    val schedule =
        remember(state.prayerConfiguration, day, now.time / 60_000) {
            state.prayerConfiguration?.let { PrayerSchedule.calculate(it, now) }
        }
    val next =
        schedule
            ?.firstOrNull {
                it.time > now &&
                    state.journeys
                        .firstOrNull { j -> j.prayer == it.id }
                        ?.let { j -> !model.progress.complete(day, j) } == true
            }
            ?.id
    Box {
        BotanicalBackground(2)
        LazyColumn(
            Modifier.fillMaxSize().testTag("azkarJourneyList"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            state.text("A little, every day"),
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                        )
                        Text(
                            state.text("Follow a journey at your own pace."),
                            fontSize = 16.sp,
                            color = p.secondary,
                        )
                    }
                    Image(
                        painterResource(R.drawable.journey_crescent),
                        null,
                        Modifier.size(width = 84.dp, height = 110.dp),
                    )
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(p.accent.copy(alpha = .07f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Eco, null, tint = p.accent)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                state.text("Today's progress"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.accent,
                            )
                            Text(
                                state.text("of your day complete"),
                                fontSize = 13.sp,
                                color = p.secondary,
                            )
                        }
                        Text(
                            "${if(total>0) completed*100/total else 0}%",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.accent,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (total > 0) completed.toFloat() / total else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = p.accent,
                        trackColor = p.accent.copy(alpha = .15f),
                    )
                }
            }
            state.journeys.forEachIndexed { index, journey ->
                if (index == 0 || index == 2 || index == 7)
                    item {
                        Row(
                            Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                when (index) {
                                    0 -> state.text("Begin your day")
                                    2 -> state.text("Your prayer journeys")
                                    else -> state.text("As the day settles")
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            if (index == 2)
                                IconButton(onClick = { go(Destination.Prayer) }) {
                                    Icon(
                                        Icons.Outlined.Schedule,
                                        state.text("Set prayer times"),
                                        tint = p.accent,
                                    )
                                }
                        }
                        if (index == 2 && state.prayerConfiguration == null)
                            TextButton(onClick = { go(Destination.Prayer) }) {
                                Text(
                                    state.text("Set your location for prayer times"),
                                    fontSize = 14.sp,
                                )
                            }
                    }
                item(key = journey.id) {
                    val count = model.progress.completedCount(day, journey)
                    val complete = model.progress.complete(day, journey)
                    val time =
                        schedule
                            ?.firstOrNull { it.id == journey.prayer }
                            ?.let {
                                PrayerSchedule.format(
                                    it.time,
                                    state.prayerConfiguration!!,
                                    state.language,
                                )
                            }
                    Column(
                        Modifier.fillMaxWidth()
                            .background(p.background.copy(alpha = .92f), RoundedCornerShape(22.dp))
                            .border(
                                if (next == journey.prayer) 2.dp else 1.dp,
                                p.accent.copy(
                                    alpha =
                                        if (next == journey.prayer) .8f
                                        else if (complete) .3f else .12f
                                ),
                                RoundedCornerShape(22.dp),
                            )
                            .clickable { go(Destination.Journey(journey.id)) }
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (next == journey.prayer && journey.prayer != null)
                            Text(
                                state.text("Next prayer"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.accent,
                            )
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                if (complete) Icons.Outlined.Check
                                else
                                    when (journey.id) {
                                        "morning" -> Icons.Outlined.WbTwilight
                                        "going-out" -> Icons.Outlined.DoorFront
                                        "evening" -> Icons.Outlined.WbTwilight
                                        "sleep" -> Icons.Outlined.NightsStay
                                        else -> Icons.Outlined.WbSunny
                                    },
                                null,
                                tint = p.accent,
                                modifier =
                                    Modifier.size(44.dp)
                                        .background(p.accent.copy(alpha = .08f), CircleShape)
                                        .padding(11.dp),
                            )
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(journey.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    time ?: journey.subtitle,
                                    fontSize = if (time == null) 13.sp else 14.sp,
                                    color = if (time == null) p.secondary else p.accent,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                null,
                                Modifier.padding(top = 14.dp).size(15.dp),
                                tint = p.secondary,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LinearProgressIndicator(
                                progress = { count.toFloat() / journey.requiredSteps.size },
                                modifier = Modifier.weight(1f),
                                color = p.accent,
                                trackColor = p.accent.copy(alpha = .15f),
                            )
                            Text(
                                if (complete) state.text("Complete")
                                else "$count / ${journey.requiredSteps.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JourneyDetail(
    state: AppState,
    model: AppViewModel,
    journey: Journey,
    day: String,
    open: (Int) -> Unit,
) {
    val p = LocalPalette.current
    val count = model.progress.completedCount(day, journey)
    val cue = rememberCounterCue()
    Box {
        BotanicalBackground(3)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(journey.subtitle, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    journey.prayer?.let { prayer ->
                        state.prayerConfiguration?.let { config ->
                            PrayerSchedule.calculate(config, Date())
                                .firstOrNull { it.id == prayer }
                                ?.let {
                                    Text(
                                        PrayerSchedule.format(it.time, config, state.language),
                                        fontSize = 16.sp,
                                        color = p.accent,
                                    )
                                }
                        }
                    }
                    Text(
                        state.text(
                            "Complete each step when you're ready. Your progress starts fresh each day."
                        ),
                        fontSize = 14.sp,
                        color = p.secondary,
                    )
                    Row {
                        Text(
                            if (model.progress.complete(day, journey))
                                state.text("Journey complete")
                            else state.text("Your progress"),
                            fontWeight = FontWeight.Bold,
                            color = p.accent,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "$count / ${journey.requiredSteps.size}",
                            fontSize = 14.sp,
                            color = p.accent,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { count.toFloat() / journey.requiredSteps.size },
                        modifier = Modifier.fillMaxWidth(),
                        color = p.accent,
                        trackColor = p.accent.copy(alpha = .15f),
                    )
                }
            }
            items(journey.steps, key = { it.id }) { step ->
                val completed = model.progress.completed(day, journey, step)
                val choice = model.progress.choice(day, journey, step)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp)
                            .padding(3.dp)
                            .background(if (completed) p.accent else p.background, CircleShape)
                            .border(1.dp, p.accent.copy(alpha = .55f), CircleShape)
                            .clickable(
                                enabled =
                                    state.progressReady &&
                                        (step.options.isEmpty() || choice.isNotEmpty())
                            ) {
                                model.toggleStep(day, journey, step) { saved ->
                                    cue.play(
                                        if (saved.complete(day, journey)) 2
                                        else if (saved.completed(day, journey, step)) 0 else 1
                                    )
                                }
                            }
                            .semantics {
                                contentDescription =
                                    state.text(
                                        if (completed) "Mark incomplete: " else "Mark complete: "
                                    ) + step.title
                                selected = completed
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completed)
                            Icon(
                                Icons.Outlined.Check,
                                null,
                                tint = p.background,
                                modifier = Modifier.size(18.dp),
                            )
                        else
                            Text(
                                "${journey.steps.indexOf(step)+1}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.accent,
                            )
                    }
                    Column(
                        Modifier.weight(1f)
                            .background(p.background.copy(alpha = .92f), RoundedCornerShape(18.dp))
                            .border(
                                1.dp,
                                p.accent.copy(alpha = if (completed) .3f else .1f),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(step.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (step.optional)
                            Text(state.text("Optional"), fontSize = 12.sp, color = p.accent)
                        if (step.note.isNotEmpty())
                            Text(step.note, fontSize = 14.sp, color = p.secondary)
                        step.options.forEach { (id, title) ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(
                                    enabled = state.progressReady
                                ) {
                                    model.choose(day, journey, step, id)
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = choice == id,
                                    enabled = state.progressReady,
                                    onClick = { model.choose(day, journey, step, id) },
                                )
                                Text(title, fontSize = 15.sp, color = p.accent)
                            }
                        }
                        step.categories.forEach { id ->
                            state.categories
                                .firstOrNull { it.id == id }
                                ?.let { category ->
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable {
                                            open(id)
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.MenuBook,
                                            null,
                                            Modifier.size(16.dp),
                                            tint = p.accent,
                                        )
                                        Text(
                                            category.title,
                                            fontSize = 14.sp,
                                            color = p.accent,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            null,
                                            Modifier.size(14.dp),
                                            tint = p.accent,
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}
