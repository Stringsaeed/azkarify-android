@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package sh.saeed.azkarify.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.data.ProgressFailure
import sh.saeed.azkarify.ui.components.AzButton
import sh.saeed.azkarify.ui.components.AzButtonVariant
import sh.saeed.azkarify.ui.components.AzIcon
import sh.saeed.azkarify.ui.components.AzIconButton
import sh.saeed.azkarify.ui.components.AzIconButtonVariant
import sh.saeed.azkarify.ui.components.AzOrnament
import sh.saeed.azkarify.ui.components.AzOrnamentShape
import sh.saeed.azkarify.ui.components.AzPointsBadge
import sh.saeed.azkarify.ui.components.AzSticker
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzBorder
import sh.saeed.azkarify.ui.theme.AzDuration
import sh.saeed.azkarify.ui.theme.azTween
import sh.saeed.azkarify.ui.theme.AzSpace

@Composable
fun AzkarifyApp(model: AppViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    AzkarTheme(state) {
        val direction = if (state.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            val colors = Az.colors
            val view = androidx.compose.ui.platform.LocalView.current
            SideEffect {
                (view.context as? Activity)?.let { activity ->
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                        isAppearanceLightStatusBars = !colors.isDark
                        isAppearanceLightNavigationBars = !colors.isDark
                    }
                }
            }
            val initialFocus = remember { FocusRequester() }
            val windowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(state.loading, state.introSeen, windowFocused) {
                if (!state.loading && windowFocused) {
                    // Older Android versions restore focus to the first input when the window opens.
                    withFrameNanos {}
                    initialFocus.requestFocus()
                }
            }
            var replayIntro by rememberSaveable { mutableStateOf(false) }
            Surface(Modifier.fillMaxSize().focusRequester(initialFocus).focusable(), color = colors.paper) {
                if ((!state.introSeen && !state.loading) || replayIntro)
                    IntroScreen(state) {
                        replayIntro = false
                        model.finishIntro()
                    }
                else AppNavigation(state, model) { replayIntro = true }
            }
        }
    }
}

private class Completed(val journeyId: String, val earned: Int)

private fun Tab.symbol(selected: Boolean) =
    when (this) {
        Tab.Today -> if (selected) AzSymbol.todayFilled else AzSymbol.today
        Tab.Azkar -> AzSymbol.azkar
        Tab.Counter -> AzSymbol.counter
        Tab.Settings -> AzSymbol.settings
    }

private fun Tab.label(state: AppState) =
    when (this) {
        Tab.Today -> state.text("Today")
        Tab.Azkar -> state.text("Azkar")
        Tab.Counter -> state.text("Counter")
        Tab.Settings -> state.text("Settings")
    }

@Composable
private fun AppNavigation(state: AppState, model: AppViewModel, onIntro: () -> Unit) {
    val colors = Az.colors
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    val stacks =
        mapOf(
            Tab.Today to rememberDestinations(Destination.Today),
            Tab.Azkar to rememberDestinations(Destination.Azkar),
            Tab.Counter to rememberDestinations(Destination.Counter),
            Tab.Settings to rememberDestinations(Destination.Settings),
        )
    val stack = stacks.getValue(tab)
    val focusManager = LocalFocusManager.current

    var points by rememberSaveable { mutableStateOf(false) }
    var location by rememberSaveable { mutableStateOf(false) }
    // The per-zikr counter sheet survives recreation, like the count inside it.
    var countingFrom by rememberSaveable { mutableIntStateOf(0) }
    var countingTitle by rememberSaveable { mutableStateOf("") }
    var completed by remember { mutableStateOf<Completed?>(null) }
    var focusCategory by rememberSaveable { mutableStateOf<Int?>(null) }
    var focusStart by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var favorites by rememberSaveable { mutableStateOf(false) }

    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    LaunchedEffect(Unit) {
        while (true) {
            day = LocalDate.now().toString()
            delay(30_000.milliseconds)
        }
    }

    fun go(value: Destination) {
        focusManager.clearFocus()
        // Opening another topic from a topic replaces it, so back returns to the list.
        if (value is Destination.Category && stack.last() is Destination.Category) stack.removeLastOrNull()
        if (stack.last() != value) stack.add(value)
    }
    fun back() {
        if (stack.size > 1) stack.removeLastOrNull()
    }
    fun select(next: Tab) {
        focusManager.clearFocus()
        // Re-selecting a tab returns to its root, as on iOS.
        if (next == tab) stacks.getValue(next).let { while (it.size > 1) it.removeLastOrNull() }
        tab = next
    }
    val onCount: (Int, String) -> Unit = { count, text ->
        countingFrom = count
        countingTitle = text
    }
    val onFocus: (Int, Int) -> Unit = { category, index ->
        focusCategory = category
        focusStart = index
    }

    BackHandler(focusCategory == null && stack.size > 1) { back() }
    BackHandler(focusCategory == null && stack.size == 1 && tab != Tab.Today) { tab = Tab.Today }

    val itemColors =
        NavigationSuiteDefaults.itemColors(
            navigationBarItemColors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.ink,
                    selectedTextColor = colors.ink,
                    unselectedIconColor = colors.inkMuted,
                    unselectedTextColor = colors.inkMuted,
                    indicatorColor = Color.Transparent,
                ),
            navigationRailItemColors =
                NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.ink,
                    selectedTextColor = colors.ink,
                    unselectedIconColor = colors.inkMuted,
                    unselectedTextColor = colors.inkMuted,
                    indicatorColor = Color.Transparent,
                ),
        )
    Box(Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                Tab.entries.forEach { item ->
                    val selected = item == tab
                    item(
                        selected = selected,
                        onClick = { select(item) },
                        icon = { NavIcon(item.symbol(selected), selected) },
                        label = { Text(item.label(state), style = Az.type.footnote) },
                        modifier = Modifier.testTag("tab.${item.name}"),
                        colors = itemColors,
                    )
                }
            },
            navigationSuiteColors =
                NavigationSuiteDefaults.colors(
                    navigationBarContainerColor = colors.paperRaised,
                    navigationRailContainerColor = colors.paperRaised,
                    navigationBarContentColor = colors.ink,
                    navigationRailContentColor = colors.ink,
                ),
            containerColor = colors.paper,
            contentColor = colors.ink,
        ) {
            val holder = rememberSaveableStateHolder()
            val top = stack.last()
            AnimatedContent(
                tab to top,
                Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(azTween(AzDuration.fast)) togetherWith fadeOut(azTween(AzDuration.fast))
                },
                contentKey = { (t, d) -> if (t == Tab.Azkar) "azkar" else "$t/$d" },
                label = "destination",
            ) { (current, destination) ->
                // The Azkar tab keeps one list-detail layout for its whole stack.
                val key = if (current == Tab.Azkar) "azkar" else "$current/$destination"
                holder.SaveableStateProvider(key) {
                    when {
                        current == Tab.Azkar ->
                            AzkarTab(
                                state,
                                model,
                                stacks.getValue(Tab.Azkar),
                                search,
                                { search = it },
                                favorites,
                                { favorites = it },
                                ::go,
                                ::back,
                                onCount,
                                onFocus,
                            )
                        else ->
                            DestinationContent(
                                destination,
                                state,
                                model,
                                day,
                                canGoBack = stack.size > 1,
                                go = ::go,
                                back = ::back,
                                onPoints = { points = true },
                                onLocation = { location = true },
                                onCount = onCount,
                                onFocus = onFocus,
                                onIntro = onIntro,
                                onComplete = { journey, earned -> completed = Completed(journey.id, earned) },
                            )
                    }
                }
            }
        }
        val focusTopic = focusCategory?.let { id -> state.categories.firstOrNull { it.id == id } }
        AnimatedVisibility(focusTopic != null, enter = fadeIn(azTween(AzDuration.base)), exit = fadeOut(azTween(AzDuration.fast))) {
            val topic = focusTopic ?: return@AnimatedVisibility
            BackHandler { focusCategory = null }
            FocusScreen(topic, state, model, focusStart, onClose = { focusCategory = null }, onCount = onCount)
        }
    }

    state.progressFailure?.let { failure ->
        AlertDialog(
            onDismissRequest = model::dismissProgressFailure,
            containerColor = colors.paperRaised,
            title = { Text(state.text("Progress unavailable"), style = Az.type.title3, color = colors.ink) },
            text = {
                Text(
                    if (failure == ProgressFailure.Load)
                        state.text("Could not load your saved progress. Please try again.")
                    else state.text("Could not save your progress. Please try again."),
                    style = Az.type.body,
                    color = colors.inkSoft,
                )
            },
            confirmButton = {
                AzButton(
                    state.text(if (failure == ProgressFailure.Load) "Retry" else "OK"),
                    {
                        if (failure == ProgressFailure.Load) model.loadProgress()
                        else model.dismissProgressFailure()
                    },
                    variant = AzButtonVariant.Text,
                )
            },
        )
    }
    if (countingFrom > 0)
        ModalBottomSheet(
            onDismissRequest = { countingFrom = 0 },
            sheetState = rememberBottomSheetState(SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
            containerColor = colors.paper,
        ) {
            CounterContent(
                countingFrom,
                countingTitle,
                state,
                model.progress.countsToNextPoint(),
                model::recordCounterTap,
                Modifier.padding(bottom = AzSpace.xl),
            )
        }
    if (points) PointsSheet(state, model, day) { points = false }
    if (location || (state.introSeen && !state.locationPromptSeen && !state.loading))
        PrayerLocationSheet(state, model) {
            location = false
            if (!state.locationPromptSeen) model.finishLocationPrompt()
        }
    completed?.let { done ->
        state.journeys.firstOrNull { it.id == done.journeyId }?.let { journey ->
            CompletionSheet(state, journey.title, done.earned, model.progress.points) { completed = null }
        }
    }
}

/** Every destination outside the Azkar tab. */
@Composable
private fun DestinationContent(
    destination: Destination,
    state: AppState,
    model: AppViewModel,
    day: String,
    canGoBack: Boolean,
    go: (Destination) -> Unit,
    back: () -> Unit,
    onPoints: () -> Unit,
    onLocation: () -> Unit,
    onCount: (Int, String) -> Unit,
    onFocus: (Int, Int) -> Unit,
    onIntro: () -> Unit,
    onComplete: (sh.saeed.azkarify.data.Journey, Int) -> Unit,
) {
    when (destination) {
        Destination.Today ->
            TodayScreen(
                state,
                model,
                day,
                onJourney = { go(Destination.Journey(it.id)) },
                onLocation = onLocation,
                onPrayerTimes = { go(Destination.PrayerTimes) },
                onPoints = onPoints,
            )
        Destination.Counter ->
            CounterScreen(
                state,
                model.progress.points,
                model.progress.countsToNextPoint(),
                model::recordCounterTap,
                onPoints,
            )
        Destination.Settings ->
            SettingsScreen(state, model, { go(Destination.PrayerTimes) }, onLocation, onIntro)
        Destination.PrayerTimes -> PrayerSettingsScreen(state, model, back)
        is Destination.Journey -> {
            val journey = state.journeys.firstOrNull { it.id == destination.id }
            if (journey == null) Missing(state, back)
            else
                JourneyScreen(
                    state,
                    model,
                    journey,
                    day,
                    onBack = back,
                    onPoints = onPoints,
                    onCategory = { go(Destination.Category(it)) },
                    onRead = { go(Destination.Category(it)) },
                    onComplete = onComplete,
                )
        }
        is Destination.Category -> {
            val category = state.categories.firstOrNull { it.id == destination.id }
            if (category == null) Missing(state, back)
            else
                ReaderScreen(
                    category,
                    state,
                    model,
                    onBack = back.takeIf { canGoBack },
                    onCount = onCount,
                    onFocus = { onFocus(category.id, it) },
                )
        }
        Destination.Azkar -> Unit
    }
}

@Composable
private fun Missing(state: AppState, back: () -> Unit) {
    // A saved stack can outlive its content while azkar reload; step back rather than show nothing.
    if (!state.loading) LaunchedEffect(Unit) { back() }
}

/** Azkar: the topic list, with the reader beside it on wide windows. */
@Composable
private fun AzkarTab(
    state: AppState,
    model: AppViewModel,
    stack: NavBackStack<Destination>,
    search: String,
    onSearch: (String) -> Unit,
    favorites: Boolean,
    onFavorites: (Boolean) -> Unit,
    go: (Destination) -> Unit,
    back: () -> Unit,
    onCount: (Int, String) -> Unit,
    onFocus: (Int, Int) -> Unit,
) {
    val route = stack.last()
    val category = (route as? Destination.Category)?.let { r -> state.categories.firstOrNull { it.id == r.id } }
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val tabletop = adaptiveInfo.windowPosture.isTabletop
    val baseDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(adaptiveInfo)
    val directive =
        PaneScaffoldDirective(
            maxHorizontalPartitions = if (tabletop) 1 else minOf(2, baseDirective.maxHorizontalPartitions),
            horizontalPartitionSpacerSize = baseDirective.horizontalPartitionSpacerSize,
            maxVerticalPartitions = baseDirective.maxVerticalPartitions,
            verticalPartitionSpacerSize = baseDirective.verticalPartitionSpacerSize,
            defaultPanePreferredWidth = baseDirective.defaultPanePreferredWidth,
            defaultPanePreferredHeight = baseDirective.defaultPanePreferredHeight,
            excludedBounds = baseDirective.excludedBounds,
            // Focusing a destination pane selects its first text input, including list search.
            shouldAutoFocusCurrentDestination = false,
        )
    val strategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(
            listPaneAdaptStrategy =
                if (tabletop) AdaptStrategy.Reflow(ListDetailPaneScaffoldRole.Detail) else AdaptStrategy.Hide
        )
    val paneValue =
        calculateThreePaneScaffoldValue(
            maxHorizontalPartitions = directive.maxHorizontalPartitions,
            maxVerticalPartitions = if (tabletop) directive.maxVerticalPartitions else 1,
            adaptStrategies = strategies,
            currentDestination =
                ThreePaneScaffoldDestinationItem<Destination>(
                    if (category == null) ListDetailPaneScaffoldRole.List else ListDetailPaneScaffoldRole.Detail,
                    route,
                ),
        )
    val twoPanes = directive.maxHorizontalPartitions > 1
    ListDetailPaneScaffold(
        directive = directive,
        value = paneValue,
        modifier = Modifier.fillMaxSize().imePadding(),
        listPane = {
            AnimatedPane(Modifier.testTag("azkarListPane")) {
                AzkarScreen(
                    state,
                    model,
                    search,
                    onSearch,
                    favorites,
                    onFavorites,
                    { go(Destination.Category(it.id)) },
                    category?.id,
                )
            }
        },
        detailPane = {
            AnimatedPane(Modifier.testTag("azkarDetailPane")) {
                if (category == null) AzkarPlaceholder(state)
                else
                    ReaderScreen(
                        category,
                        state,
                        model,
                        // Side by side, the list stays visible, so the reader needs no back button.
                        onBack = back.takeUnless { twoPanes },
                        onCount = onCount,
                        onFocus = { onFocus(category.id, it) },
                    )
            }
        },
    )
}

/** A tab icon over the lime pill (`nav indicator`), outlined in ink when selected. */
@Composable
private fun NavIcon(symbol: Int, selected: Boolean) {
    val colors = Az.colors
    AzIcon(
        symbol,
        null,
        Modifier.drawBehind {
            if (!selected) return@drawBehind
            val pill = Size(56.dp.toPx(), 32.dp.toPx())
            val topLeft = Offset((size.width - pill.width) / 2, (size.height - pill.height) / 2)
            val radius = CornerRadius(pill.height / 2)
            drawRoundRect(colors.lime, topLeft, pill, radius)
            val line = AzBorder.hairline.toPx()
            drawRoundRect(
                colors.ink,
                topLeft + Offset(line / 2, line / 2),
                Size(pill.width - line, pill.height - line),
                radius,
                style = Stroke(line),
            )
        },
    )
}

@Composable
private fun PointsSheet(state: AppState, model: AppViewModel, day: String, onDismiss: () -> Unit) {
    val colors = Az.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.paper) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AzSpace.gutter).padding(bottom = AzSpace.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AzSpace.sm),
        ) {
            AzOrnament(AzOrnamentShape.Star, colors.spark, size = 48.dp, spin = true)
            Text(state.text("Your points"), style = Az.type.title2, color = colors.ink)
            Text(state.number(model.progress.points), style = Az.type.numeral, color = colors.ink)
            Text(
                state.text("Earned today: ") + state.number(model.progress.todayPoints(day)),
                style = Az.type.headline,
                color = colors.forest,
            )
            Text(
                state.text("10 points for each step, and 25 for completing a journey."),
                style = Az.type.callout,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompletionSheet(state: AppState, title: String, earned: Int, total: Int, onDismiss: () -> Unit) {
    val colors = Az.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.paper) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AzSpace.gutter).padding(bottom = AzSpace.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AzSpace.md),
        ) {
            Box(Modifier.padding(top = AzSpace.sm), contentAlignment = Alignment.Center) {
                Box(Modifier.background(colors.forest, androidx.compose.foundation.shape.CircleShape).padding(28.dp)) {
                    AzOrnament(AzOrnamentShape.Sparkle, colors.lime, size = 56.dp)
                }
            }
            AzSticker(state.text("Journey complete"), large = true)
            Text(title, style = Az.type.title1, color = colors.ink, textAlign = TextAlign.Center)
            Text(
                state.text("All steps, done for today."),
                style = Az.type.body,
                color = colors.inkSoft,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AzSpace.sm)) {
                AzPointsBadge("+" + state.number(earned), state.text("Points earned"), null)
                Text(
                    state.text("%s points in total").replace("%s", state.number(total)),
                    style = Az.type.callout,
                    color = colors.inkMuted,
                )
            }
            AzButton(state.text("Done"), onDismiss, Modifier.fillMaxWidth().padding(top = AzSpace.sm))
        }
    }
}
