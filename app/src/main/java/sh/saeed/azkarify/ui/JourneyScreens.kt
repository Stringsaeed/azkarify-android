@file:OptIn(ExperimentalMaterial3Api::class)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Date
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.data.Category
import sh.saeed.azkarify.data.Journey
import sh.saeed.azkarify.data.JourneyProgress
import sh.saeed.azkarify.data.Step
import sh.saeed.azkarify.services.PrayerSchedule
import sh.saeed.azkarify.ui.components.AzButton
import sh.saeed.azkarify.ui.components.AzCard
import sh.saeed.azkarify.ui.components.AzChevron
import sh.saeed.azkarify.ui.components.AzChip
import sh.saeed.azkarify.ui.components.AzEyebrow
import sh.saeed.azkarify.ui.components.AzIcon
import sh.saeed.azkarify.ui.components.AzIconButton
import sh.saeed.azkarify.ui.components.AzPill
import sh.saeed.azkarify.ui.components.AzPillTone
import sh.saeed.azkarify.ui.components.AzPointsBadge
import sh.saeed.azkarify.ui.components.AzProgressTrack
import sh.saeed.azkarify.ui.components.AzRail
import sh.saeed.azkarify.ui.components.AzStepMarker
import sh.saeed.azkarify.ui.components.AzStepRow
import sh.saeed.azkarify.ui.components.AzStepState
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzSpace

/** The section a journey sits in on Today, for its eyebrow. */
private fun section(journey: Journey, state: AppState): String {
    val index = state.journeys.filter { it.prayer == null }.indexOf(journey)
    return when {
        journey.prayer != null -> state.text("02 / Through the day")
        index in 0..1 -> state.text("01 / Morning")
        else -> state.text("03 / Evening")
    }
}

/**
 * One journey's timeline. Completing the last required step reports the points it earned so the
 * completion sheet can celebrate it once.
 */
@Composable
internal fun JourneyScreen(
    state: AppState,
    model: AppViewModel,
    journey: Journey,
    day: String,
    onBack: () -> Unit,
    onPoints: () -> Unit,
    onCategory: (Int) -> Unit,
    onRead: (Int) -> Unit,
    onComplete: (Journey, Int) -> Unit,
) {
    val colors = Az.colors
    // Read so a saved step refreshes the screen.
    state.progressVersion
    val progress = model.progress
    val count = progress.completedCount(day, journey)
    val required = journey.requiredSteps.size
    val cue = rememberCounterCue()
    val nextStep = journey.steps.firstOrNull { !it.optional && !progress.completed(day, journey, it) }
    val time =
        remember(state.prayerConfiguration, journey.prayer, day) {
            journey.prayer?.let { id ->
                state.prayerConfiguration?.let { config ->
                    PrayerSchedule.calculate(config, Date()).firstOrNull { it.id == id }?.let {
                        PrayerSchedule.format(it.time, config, state.language)
                    }
                }
            }
        }
    Scaffold(
        containerColor = colors.paper,
        topBar = {
            TopAppBar(
                title = { Text(journey.title, style = Az.type.headline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { AzIconButton(AzSymbol.back, state.text("Back"), onBack) },
                actions = {
                    AzPointsBadge(state.number(progress.points), state.text("Your points"), onPoints, Modifier.padding(end = AzSpace.xs))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.paper, titleContentColor = colors.ink),
            )
        },
    ) { insets ->
        LazyColumn(
            Modifier.fillMaxSize().testTag("journeySteps"),
            contentPadding =
                PaddingValues(
                    start = AzSpace.gutter,
                    end = AzSpace.gutter,
                    top = insets.calculateTopPadding() + AzSpace.md,
                    bottom = insets.calculateBottomPadding() + AzSpace.xxl,
                ),
        ) {
            item(key = "header") {
                Column(Modifier.padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AzEyebrow(
                        section(journey, state) + " · " +
                            state.text("%s steps").replace("%s", state.number(journey.steps.size))
                    )
                    Text(journey.subtitle, style = Az.type.title1, color = colors.ink)
                    time?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AzIcon(AzSymbol.schedule, null, size = 18.dp, tint = colors.inkMuted)
                            Text(it, style = Az.type.callout.copy(fontFeatureSettings = "tnum"), color = colors.inkMuted)
                        }
                    }
                    Text(
                        state.text("Complete each step when you're ready. Your progress starts fresh each day."),
                        style = Az.type.footnote,
                        color = colors.inkMuted,
                    )
                    Row(
                        Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AzSpace.sm),
                    ) {
                        val complete = progress.complete(day, journey)
                        AzProgressTrack(if (required > 0) count.toFloat() / required else 0f, Modifier.weight(1f), complete = complete)
                        Text(
                            if (complete) state.text("Journey complete") else state.count(count, required),
                            style = Az.type.footnote.copy(fontWeight = Az.type.headline.fontWeight),
                            color = if (complete) colors.forest else colors.ink,
                        )
                    }
                }
            }
            itemsIndexed(journey.steps, key = { _, step -> step.id }) { index, step ->
                StepItem(
                    state,
                    model,
                    journey,
                    step,
                    index,
                    day,
                    isNext = step == nextStep,
                    last = index == journey.steps.lastIndex,
                    onCategory = onCategory,
                    onRead = onRead,
                    onToggle = {
                        val before = progress.points
                        val wasComplete = progress.complete(day, journey)
                        model.toggleStep(day, journey, step) { saved: JourneyProgress ->
                            val done = saved.complete(day, journey)
                            cue.play(if (done) 2 else if (saved.completed(day, journey, step)) 0 else 1)
                            // Celebrate once a day: completing again after an undo earns nothing new.
                            if (done && !wasComplete && saved.points > before) onComplete(journey, saved.points - before)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StepItem(
    state: AppState,
    model: AppViewModel,
    journey: Journey,
    step: Step,
    index: Int,
    day: String,
    isNext: Boolean,
    last: Boolean,
    onCategory: (Int) -> Unit,
    onRead: (Int) -> Unit,
    onToggle: () -> Unit,
) {
    val colors = Az.colors
    val progress = model.progress
    val completed = progress.completed(day, journey, step)
    val choice = progress.choice(day, journey, step)
    val needsChoice = step.options.isNotEmpty() && choice.isEmpty()
    val categories: List<Category> = step.categories.mapNotNull { id -> state.categories.firstOrNull { it.id == id } }
    AzStepRow(
        marker = {
            AzStepMarker(
                state.number(index + 1),
                when {
                    completed -> AzStepState.Done
                    isNext && !needsChoice -> AzStepState.Next
                    else -> AzStepState.Todo
                },
                accessibilityLabel =
                    if (needsChoice) state.text("Choose the prayer for this routine.")
                    else state.text(if (completed) "Mark incomplete: " else "Mark complete: ") + step.title,
                accessibilityState = state.text(if (completed) "Complete" else "Incomplete"),
                onClick = onToggle,
                enabled = state.progressReady && !needsChoice,
            )
        },
        rail =
            when {
                last -> AzRail.None
                completed -> AzRail.Solid
                else -> AzRail.Dashed
            },
    ) {
        val body: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AzSpace.xs)) {
                    Text(step.title, Modifier.weight(1f), style = Az.type.headline, color = colors.ink)
                    when {
                        isNext && !completed -> AzPill(state.text("Up next"), tone = AzPillTone.Accent)
                        step.optional -> AzPill(state.text("Optional"))
                    }
                }
                if (step.note.isNotEmpty()) Text(step.note, style = Az.type.footnote, color = colors.inkMuted)
                if (step.options.isNotEmpty()) {
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(AzSpace.xs)) {
                        step.options.forEach { (id, title) ->
                            AzChip(title, choice == id, { if (state.progressReady) model.choose(day, journey, step, id) })
                        }
                    }
                }
                categories.forEach { category ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable(role = Role.Button) { onCategory(category.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AzIcon(AzSymbol.azkar, null, size = 18.dp, tint = colors.accent)
                        Text(category.title, Modifier.weight(1f), style = Az.type.callout, color = colors.accent)
                        if (isNext && !completed) AzChevron()
                    }
                }
                if (isNext && !completed && categories.isNotEmpty())
                    AzButton(
                        state.text("Start reading"),
                        { onRead(categories.first().id) },
                        Modifier.padding(top = 4.dp),
                        small = true,
                    )
            }
        }
        if (isNext && !completed)
            AzCard(compact = true) { body() }
        else body()
    }
}
