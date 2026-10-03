@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package com.stringsaeed.azkarify.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stringsaeed.azkarify.R
import com.stringsaeed.azkarify.data.*
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AzkarifyApp(model: AppViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    AzkarTheme(state) {
        val direction = if (state.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            val p = LocalPalette.current
            val context = LocalContext.current
            SideEffect {
                (context as? Activity)?.let { activity ->
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .apply {
                            isAppearanceLightStatusBars = !p.dark
                            isAppearanceLightNavigationBars = !p.dark
                        }
                }
            }
            val initialFocus = remember { FocusRequester() }
            val windowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(state.loading, state.introSeen, windowFocused) {
                if (!state.loading && windowFocused) {
                    // Older Android versions restore focus to the first input when the window
                    // opens.
                    withFrameNanos {}
                    initialFocus.requestFocus()
                }
            }
            Surface(
                Modifier.fillMaxSize().focusRequester(initialFocus).focusable(),
                color = p.background,
            ) {
                if (!state.introSeen && !state.loading) IntroScreen(state, model::finishIntro)
                else AppNavigation(state, model)
            }
        }
    }
}

@Composable
private fun AppNavigation(state: AppState, model: AppViewModel) {
    val routes = rememberDestinations()
    var points by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var counterTitle by rememberSaveable { mutableStateOf("") }
    var counterInitial by rememberSaveable { mutableIntStateOf(0) }
    var counterOpen by rememberSaveable { mutableStateOf(false) }
    var slideshow by rememberSaveable { mutableStateOf(false) }
    val route = routes.last()
    val focusManager = LocalFocusManager.current
    fun go(value: Destination) {
        focusManager.clearFocus()
        if (value is Destination.Category && routes.last() is Destination.Category)
            routes.removeLastOrNull()
        routes.add(value)
        if (value !is Destination.Category) search = ""
    }
    fun back() {
        if (routes.size > 1) routes.removeLastOrNull()
        slideshow = false
    }
    BackHandler(routes.size > 1) { back() }
    val category =
        if (route is Destination.Category) state.categories.firstOrNull { it.id == route.id }
        else null
    val journey =
        if (route is Destination.Journey) state.journeys.firstOrNull { it.id == route.id } else null
    val title =
        category?.title
            ?: journey?.title
            ?: when (route) {
                Destination.Home -> state.text("Husn")
                Destination.Favorites -> state.text("Favorites")
                Destination.Counter -> state.text("Counter")
                Destination.Quick -> state.text("Quick Mode")
                Destination.Settings -> state.text("Settings")
                Destination.Journeys -> state.text("Journeys")
                Destination.Prayer -> state.text("Prayer times")
                else -> "Azkarify"
            }
    val p = LocalPalette.current
    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    LaunchedEffect(Unit) {
        while (true) {
            day = LocalDate.now().toString()
            delay(30_000.milliseconds)
        }
    }
    val listRoute =
        routes.lastOrNull { it == Destination.Home || it == Destination.Favorites }
            ?: Destination.Home
    val listFocused = route == Destination.Home || route == Destination.Favorites
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val baseDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(adaptiveInfo)
    val directive =
        PaneScaffoldDirective(
            maxHorizontalPartitions =
                if (adaptiveInfo.windowPosture.isTabletop) 1
                else minOf(2, baseDirective.maxHorizontalPartitions),
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
                if (adaptiveInfo.windowPosture.isTabletop)
                    AdaptStrategy.Reflow(ListDetailPaneScaffoldRole.Detail)
                else AdaptStrategy.Hide
        )
    val paneValue =
        calculateThreePaneScaffoldValue(
            maxHorizontalPartitions = directive.maxHorizontalPartitions,
            maxVerticalPartitions =
                if (adaptiveInfo.windowPosture.isTabletop) directive.maxVerticalPartitions else 1,
            adaptStrategies = strategies,
            currentDestination =
                ThreePaneScaffoldDestinationItem<Destination>(
                    if (listFocused) ListDetailPaneScaffoldRole.List
                    else ListDetailPaneScaffoldRole.Detail,
                    route,
                ),
        )
    ListDetailPaneScaffold(
        directive = directive,
        value = paneValue,
        modifier = Modifier.fillMaxSize().imePadding(),
        listPane = {
            AnimatedPane {
                Scaffold(
                    modifier = Modifier.testTag("azkarListPane"),
                    containerColor = p.background,
                    topBar = {
                        Column {
                            NavigationAppBar(
                                state,
                                listRoute,
                                state.text(
                                    if (listRoute == Destination.Favorites) "Favorites" else "Husn"
                                ),
                                model.progress.points,
                                ::back,
                                ::go,
                                { points = true },
                                {},
                            )
                            ListSearchBar(
                                query = search,
                                onQueryChange = { search = it },
                                state = state,
                            )
                        }
                    },
                ) { insets ->
                    Box(Modifier.padding(insets)) {
                        HomeScreen(
                            state,
                            model,
                            search,
                            listRoute == Destination.Favorites,
                            { go(it) },
                            category?.id,
                            showOverview =
                                !adaptiveInfo.windowPosture.isTabletop && !keyboardVisible,
                        )
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                Scaffold(
                    modifier = Modifier.testTag("azkarDetailPane"),
                    containerColor = p.background,
                    topBar = {
                        if (!listFocused)
                            NavigationAppBar(
                                state,
                                route,
                                title,
                                model.progress.points,
                                ::back,
                                ::go,
                                { points = true },
                                { slideshow = true },
                            )
                    },
                ) { insets ->
                    Box(Modifier.padding(insets).fillMaxSize()) {
                        when {
                            category != null ->
                                EntriesScreen(
                                    category,
                                    state,
                                    model,
                                    slideshow,
                                    { slideshow = false },
                                ) { count, text ->
                                    counterInitial = count
                                    counterTitle = text
                                    counterOpen = true
                                }
                            journey != null ->
                                JourneyDetail(state, model, journey, day) {
                                    go(Destination.Category(it))
                                }
                            route == Destination.Counter -> CounterContent(0, "", state)
                            route == Destination.Quick ->
                                QuickScreen(state, model) { count, text ->
                                    counterInitial = count
                                    counterTitle = text
                                    counterOpen = true
                                }
                            route == Destination.Settings -> SettingsScreen(state, model) { go(it) }
                            route == Destination.Journeys ->
                                JourneysScreen(state, model, day) { go(it) }
                            route == Destination.Prayer -> PrayerSettingsScreen(state, model)
                            else ->
                                Column(
                                    Modifier.fillMaxSize().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.MenuBook,
                                        null,
                                        Modifier.size(48.dp),
                                        tint = p.accent,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        state.text("Choose a topic to read its azkar"),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                        }
                    }
                }
            }
        },
    )
    state.progressFailure?.let { failure ->
        AlertDialog(
            onDismissRequest = model::dismissProgressFailure,
            title = { Text(state.text("Progress unavailable")) },
            text = {
                Text(
                    if (failure == ProgressFailure.Load)
                        state.text("Could not load your saved progress. Please try again.")
                    else state.text("Could not save your progress. Please try again.")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (failure == ProgressFailure.Load) model.loadProgress()
                        else model.dismissProgressFailure()
                    }
                ) {
                    Text(state.text(if (failure == ProgressFailure.Load) "Retry" else "OK"))
                }
            },
        )
    }
    if (counterOpen)
        ModalBottomSheet(
            onDismissRequest = { counterOpen = false },
            sheetState =
                rememberBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                ),
            containerColor = p.background,
        ) {
            CounterContent(counterInitial, counterTitle, state)
        }
    if (points)
        ModalBottomSheet(onDismissRequest = { points = false }, containerColor = p.background) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painterResource(R.drawable.mosaic_currency),
                    null,
                    Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                )
                Text(state.text("Your points"), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${model.progress.points}",
                    fontSize = 48.sp,
                    color = p.accent,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    state.text("Earned today: ") + model.progress.todayPoints(day),
                    fontSize = 17.sp,
                )
                Text(
                    state.text("10 points for each step, and 25 for completing a journey."),
                    color = p.secondary,
                )
            }
        }
    if (state.introSeen && !state.locationPromptSeen && !state.loading) {
        ModalBottomSheet(
            onDismissRequest = model::finishLocationPrompt,
            containerColor = p.background,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    state.text("Prayer times, wherever you are"),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    state.text(
                        "Set your location to see prayer times alongside your daily journeys."
                    ),
                    fontSize = 17.sp,
                )
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        model.finishLocationPrompt()
                        go(Destination.Prayer)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(state.text("Set location"))
                }
                TextButton(
                    onClick = model::finishLocationPrompt,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(state.text("Not now"))
                }
            }
        }
    }
}

@Composable
private fun NavigationAppBar(
    state: AppState,
    headerRoute: Destination,
    headerTitle: String,
    totalPoints: Int,
    onBack: () -> Unit,
    onNavigate: (Destination) -> Unit,
    onPoints: () -> Unit,
    onSlideshow: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val p = LocalPalette.current
    TopAppBar(
        title = {
            Column {
                Text(headerTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (
                    (headerRoute == Destination.Home || headerRoute == Destination.Journeys) &&
                        state.prayerConfiguration != null
                ) {
                    Text(
                        state.prayerConfiguration.locationName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { onNavigate(Destination.Prayer) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            if (headerRoute != Destination.Home) {
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, state.text("Back"))
                }
            }
        },
        actions = {
            if (
                headerRoute == Destination.Home ||
                    headerRoute == Destination.Journeys ||
                    headerRoute is Destination.Journey
            ) {
                TextButton(onClick = { onPoints() }) {
                    Image(painterResource(R.drawable.mosaic_currency), null, Modifier.size(22.dp))
                    Text(" $totalPoints ", fontWeight = FontWeight.Bold)
                }
            }
            if (headerRoute is Destination.Category) {
                IconButton(onClick = { onSlideshow() }) {
                    Icon(Icons.Outlined.ViewCarousel, state.text("Slideshow"))
                }
            }
            if (headerRoute == Destination.Home) {
                IconButton(onClick = { onNavigate(Destination.Settings) }) {
                    Icon(Icons.Outlined.Settings, state.text("Settings"))
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, state.text("Menu"))
                    }
                    AppDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        AppDropdownMenuItem(
                            text = { Text(state.text("Favorites")) },
                            leadingIcon = { Icon(Icons.Outlined.StarBorder, null) },
                            onClick = {
                                menu = false
                                onNavigate(Destination.Favorites)
                            },
                        )
                        AppDropdownMenuItem(
                            text = { Text(state.text("Counter")) },
                            leadingIcon = { Icon(Icons.Outlined.TouchApp, null) },
                            onClick = {
                                menu = false
                                onNavigate(Destination.Counter)
                            },
                        )
                        AppDropdownMenuItem(
                            text = { Text(state.text("Quick Mode")) },
                            leadingIcon = { Icon(Icons.Outlined.Bolt, null) },
                            onClick = {
                                menu = false
                                onNavigate(Destination.Quick)
                            },
                        )
                        AppDropdownMenuItem(
                            text = { Text(state.text("Journeys")) },
                            leadingIcon = { Icon(Icons.Outlined.Route, null) },
                            onClick = {
                                menu = false
                                onNavigate(Destination.Journeys)
                            },
                        )
                        AppMenuDivider()
                        AppDropdownMenuItem(
                            text = { Text(state.text("Prayer times")) },
                            leadingIcon = { Icon(Icons.Outlined.Schedule, null) },
                            onClick = {
                                menu = false
                                onNavigate(Destination.Prayer)
                            },
                        )
                    }
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = p.background,
                navigationIconContentColor = p.accent,
                actionIconContentColor = p.accent,
            ),
    )
}

@Composable
private fun ListSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    state: AppState,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val palette = LocalPalette.current

    val containerColor =
        remember(palette.accent, palette.background) {
            palette.accent.copy(alpha = 0.08f).compositeOver(palette.background)
        }

    val textFieldState = rememberTextFieldState(initialText = query)
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val currentQuery by rememberUpdatedState(query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { value -> if (value != currentQuery) currentOnQueryChange(value) }
    }

    // The collapsed SearchBar disables the IME for an expanded search dialog.
    // This list filters inline, so its native input lives directly in a Material surface.
    Surface(
        color = containerColor,
        shape = SearchBarDefaults.inputFieldShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
    ) {
        InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {
                keyboard?.hide()
                focusManager.clearFocus()
                scope.launch { searchBarState.animateToCollapsed() }
            },
            placeholder = { Text(state.text("Search azkar")) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = state.text("Clear search"))
                    }
                }
            },
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentDescription = state.text("Search azkar")
                },
        )
    }
}
