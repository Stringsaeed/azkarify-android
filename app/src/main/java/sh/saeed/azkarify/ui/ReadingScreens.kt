@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.data.Category
import sh.saeed.azkarify.data.Entry
import sh.saeed.azkarify.ui.components.AzButton
import sh.saeed.azkarify.ui.components.AzButtonVariant
import sh.saeed.azkarify.ui.components.AzCountButton
import sh.saeed.azkarify.ui.components.AzEmptyState
import sh.saeed.azkarify.ui.components.AzEyebrow
import sh.saeed.azkarify.ui.components.AzIconButton
import sh.saeed.azkarify.ui.components.AzIconButtonVariant
import sh.saeed.azkarify.ui.components.AzPageDots
import sh.saeed.azkarify.ui.components.AzProgressTrack
import sh.saeed.azkarify.ui.components.AzReaderSizeControl
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.components.AzZikrCard
import sh.saeed.azkarify.ui.components.scaled
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzSpace

/** Entries for one topic, loaded off the main thread and cached by the repository. */
@Composable
internal fun rememberEntries(category: Category, model: AppViewModel): Pair<List<Entry>?, () -> Unit> {
    var entries by remember(category.path) { mutableStateOf<List<Entry>?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(category.path, retry) {
        entries = runCatching { model.entries(category) }.getOrElse { emptyList() }
    }
    return entries to { retry++ }
}

@Composable
internal fun CountButton(entry: Entry, state: AppState, onCount: (Int, String) -> Unit) {
    if (entry.repetitions > 1)
        AzCountButton(
            state.times(entry.repetitions),
            state.text("Count down from ") + state.number(entry.repetitions),
            { onCount(entry.repetitions, entry.arabic) },
        )
}

/** The reader: every zikr of a topic as a calm list of cards. */
@Composable
internal fun ReaderScreen(
    category: Category,
    state: AppState,
    model: AppViewModel,
    onBack: (() -> Unit)?,
    onCount: (Int, String) -> Unit,
    onFocus: (Int) -> Unit,
) {
    val colors = Az.colors
    val (entries, retry) = rememberEntries(category, model)
    var sizeSheet by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val current by remember { derivedStateOf { list.firstVisibleItemIndex } }
    Scaffold(
        containerColor = colors.paper,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        category.title,
                        style = Az.type.headline.copy(fontSize = 20.sp, lineHeight = 26.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { onBack?.let { AzIconButton(AzSymbol.back, state.text("Back"), it) } },
                actions = {
                    AzIconButton(AzSymbol.textSize, state.text("Text size"), { sizeSheet = true })
                    AzIconButton(
                        AzSymbol.focus,
                        state.text("Focus mode"),
                        { onFocus(current) },
                        enabled = !entries.isNullOrEmpty(),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.paper, titleContentColor = colors.ink),
            )
        },
    ) { insets ->
        when {
            entries == null -> Box(Modifier.padding(insets).fillMaxSize(), Alignment.Center) { LoadingIndicator() }
            entries.isEmpty() ->
                AzEmptyState(
                    state.text("Could not load azkar"),
                    state.text("Please try again."),
                    Modifier.padding(insets),
                    ornament = null,
                    symbol = AzSymbol.info,
                    action = state.text("Retry"),
                    onAction = retry,
                )
            else ->
                Column(Modifier.padding(top = insets.calculateTopPadding()).fillMaxSize()) {
                    Row(
                        Modifier.padding(horizontal = AzSpace.gutter, vertical = AzSpace.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AzSpace.sm),
                    ) {
                        AzProgressTrack((current + 1f) / entries.size, Modifier.weight(1f), thickness = 6.dp)
                        Text(
                            state.position(current + 1, entries.size),
                            style = Az.type.footnote.copy(fontFeatureSettings = "tnum"),
                            color = colors.inkMuted,
                        )
                    }
                    LazyColumn(
                        Modifier.fillMaxSize().testTag("azkarEntriesList"),
                        state = list,
                        contentPadding =
                            PaddingValues(
                                start = AzSpace.md,
                                end = AzSpace.md,
                                top = AzSpace.xs,
                                bottom = insets.calculateBottomPadding() + AzSpace.xxl,
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        itemsIndexed(entries, key = { _, it -> it.id }, contentType = { _, _ -> "zikr" }) { index, entry ->
                            AzZikrCard(
                                arabic = entry.arabic,
                                translation = entry.translated.takeIf { state.language != "ar" && it.isNotBlank() },
                                transliteration = entry.transliteration.takeIf { state.transliteration && it.isNotBlank() },
                                position = state.position(index + 1, entries.size),
                                scale = state.readerScale,
                                modifier = Modifier.widthIn(max = 640.dp),
                                count = { CountButton(entry, state, onCount) },
                            )
                        }
                    }
                }
        }
    }
    if (sizeSheet)
        ModalBottomSheet(onDismissRequest = { sizeSheet = false }, containerColor = colors.paper) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = AzSpace.gutter).padding(bottom = AzSpace.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AzSpace.md),
            ) {
                Text(state.text("Text size"), style = Az.type.title3, color = colors.ink)
                AzReaderSizeControl(
                    state.readerScale,
                    model::setReaderScale,
                    state.text("Smaller text"),
                    state.text("Larger text"),
                )
            }
        }
}

/**
 * Focus mode: one zikr per page, large and centred, with previous and next. Finishing the
 * last page earns the slideshow points (unchanged from Quick Mode).
 */
@Composable
internal fun FocusScreen(
    category: Category,
    state: AppState,
    model: AppViewModel,
    start: Int,
    onClose: () -> Unit,
    onCount: (Int, String) -> Unit,
) {
    val colors = Az.colors
    val (entries, _) = rememberEntries(category, model)
    var sizeSheet by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(colors.paper).safeDrawingPadding().testTag("focusMode")) {
        if (entries.isNullOrEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
            return@Box
        }
        val pager = rememberPagerState(initialPage = start.coerceIn(0, entries.lastIndex)) { entries.size }
        val scope = rememberCoroutineScope()
        val page = pager.currentPage
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AzSpace.md, vertical = AzSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AzIconButton(AzSymbol.close, state.text("Close"), onClose, variant = AzIconButtonVariant.Filled)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    AzEyebrow(category.title.take(40))
                    Text(
                        state.position(page + 1, entries.size),
                        style = Az.type.footnote.copy(fontFeatureSettings = "tnum"),
                        color = colors.inkMuted,
                    )
                }
                AzIconButton(
                    AzSymbol.textSize,
                    state.text("Text size"),
                    { sizeSheet = true },
                    variant = AzIconButtonVariant.Filled,
                )
            }
            HorizontalPager(pager, Modifier.weight(1f).testTag("focusPager"), key = { entries[it].id }) { index ->
                val entry = entries[index]
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = AzSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(AzSpace.xl, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(AzSpace.xl)) {
                            Text(
                                entry.arabic,
                                Modifier.fillMaxWidth(),
                                style =
                                    Az.type.readingArabic
                                        .copy(fontSize = 36.sp, lineHeight = 64.sp, textDirection = TextDirection.Rtl)
                                        .scaled(state.readerScale),
                                color = colors.ink,
                                textAlign = TextAlign.Center,
                            )
                            if (state.language != "ar" && entry.translated.isNotBlank())
                                Text(
                                    entry.translated,
                                    Modifier.fillMaxWidth(),
                                    style =
                                        Az.type.readingTranslation.copy(fontSize = 20.sp, lineHeight = 31.sp).scaled(state.readerScale),
                                    color = colors.ink,
                                    textAlign = TextAlign.Center,
                                )
                        }
                    }
                    CountButton(entry, state, onCount)
                }
            }
            Column(
                Modifier.padding(horizontal = AzSpace.gutter).padding(bottom = AzSpace.xl, top = AzSpace.sm),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // A window of five dots keeps long topics readable.
                val window = 5
                val first = (page - window / 2).coerceIn(0, (entries.size - window).coerceAtLeast(0))
                AzPageDots(minOf(window, entries.size), page - first)
                Row(horizontalArrangement = Arrangement.spacedBy(AzSpace.sm)) {
                    AzButton(
                        state.text("Previous"),
                        { scope.launch { pager.animateScrollToPage(page - 1) } },
                        Modifier.weight(1f).testTag("slideshowPrevious"),
                        AzButtonVariant.Secondary,
                        enabled = page > 0,
                        leading = AzSymbol.back,
                    )
                    val last = page == entries.lastIndex
                    AzButton(
                        state.text(if (last) "Done" else "Next"),
                        {
                            if (last) {
                                model.completeSlideshow(category.id)
                                onClose()
                            } else scope.launch { pager.animateScrollToPage(page + 1) }
                        },
                        Modifier.weight(1f).testTag("slideshowNextOrDone"),
                        trailing = if (last) null else AzSymbol.forward,
                    )
                }
            }
        }
    }
    if (sizeSheet)
        ModalBottomSheet(onDismissRequest = { sizeSheet = false }, containerColor = colors.paper) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = AzSpace.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AzReaderSizeControl(
                    state.readerScale,
                    model::setReaderScale,
                    state.text("Smaller text"),
                    state.text("Larger text"),
                )
            }
        }
}
