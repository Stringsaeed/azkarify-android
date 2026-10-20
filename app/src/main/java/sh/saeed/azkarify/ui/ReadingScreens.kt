@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.saeed.azkarify.data.*
import kotlinx.coroutines.launch

@Composable
internal fun EntriesScreen(
    category: Category,
    state: AppState,
    model: AppViewModel,
    slideshow: Boolean = false,
    close: () -> Unit = {},
    counter: (Int, String) -> Unit,
) {
    var entries by remember(category.path) { mutableStateOf<List<Entry>>(emptyList()) }
    var error by remember(category.path) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(category.path, retry) {
        error = null
        runCatching { model.entries(category) }
            .onSuccess { entries = it }
            .onFailure { error = it.localizedMessage }
    }
    if (entries.isEmpty())
        Column(Modifier.padding(24.dp)) {
            if (error == null) LoadingIndicator()
            else {
                Text(state.text("Could not load azkar"))
                Text(error!!)
                TextButton(onClick = { retry++ }) { Text(state.text("Retry")) }
            }
        }
    LazyColumn(
        Modifier.fillMaxSize().testTag("azkarEntriesList"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { it.id }, contentType = { "zikr" }) { entry ->
            AppCard {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SelectionContainer {
                        Text(
                            entry.text(state.language),
                            fontSize = if (state.language == "ar") 20.sp else 21.sp,
                            lineHeight = 30.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                        )
                    }
                    if (entry.repetitions > 1)
                        TextButton(
                            onClick = { counter(entry.repetitions, entry.text(state.language)) },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                "📿  ${entry.repetitions}",
                                fontSize = 21.sp,
                                modifier =
                                    Modifier.semantics {
                                        contentDescription =
                                            state.text("Count down from ") + entry.repetitions
                                    },
                            )
                        }
                }
            }
        }
    }
    if (slideshow && entries.isNotEmpty())
        ModalBottomSheet(
            onDismissRequest = close,
            sheetState =
                rememberBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                ),
            containerColor = LocalPalette.current.background,
        ) {
            val pager = rememberPagerState(pageCount = { entries.size })
            val scope = rememberCoroutineScope()
            val current = entries[pager.currentPage]
            Column(Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        category.title,
                        Modifier.weight(1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (current.repetitions > 1)
                        TextButton(
                            onClick = {
                                counter(current.repetitions, current.text(state.language))
                            }
                        ) {
                            Text("📿 ${current.repetitions}")
                        }
                    IconButton(onClick = close) { Icon(Icons.Outlined.Close, state.text("Close")) }
                }
                HorizontalPager(pager, Modifier.weight(1f)) { page ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                entries[page].text(state.language),
                                fontSize = 30.sp,
                                lineHeight = 42.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Text(
                    "${pager.currentPage+1} / ${entries.size}",
                    Modifier.align(Alignment.CenterHorizontally).padding(20.dp),
                    color = LocalPalette.current.accent,
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (entries.size > 1) {
                        TextButton(
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                            enabled = pager.currentPage > 0,
                            modifier = Modifier.testTag("slideshowPrevious"),
                        ) {
                            Text(state.text("Previous"))
                        }
                    }
                    TextButton(
                        onClick = {
                            if (pager.currentPage == entries.lastIndex) {
                                model.completeSlideshow(category.id)
                                close()
                            } else {
                                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                            }
                        },
                        modifier = Modifier.testTag("slideshowNextOrDone"),
                    ) {
                        Text(state.text(if (pager.currentPage == entries.lastIndex) "Done" else "Next"))
                    }
                }
            }
        }
}

@Composable
internal fun QuickScreen(state: AppState, model: AppViewModel, counter: (Int, String) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(27) }
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Text(
            state.text("Choose a set to start counting with one tap."),
            Modifier.padding(horizontal = 16.dp),
        )
        Row(
            Modifier.padding(vertical = 16.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                    Triple(27, "Morning & Evening", "🌅"),
                    Triple(28, "Before Sleep", "🌙"),
                    Triple(1, "Upon Waking", "☀️"),
                )
                .forEach { (id, title, emoji) ->
                    Text(
                        "$emoji ${state.text(title)}",
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected == id) p.accent.copy(alpha = .18f)
                                else p.secondary.copy(alpha = .08f)
                            )
                            .clickable { selected = id }
                            .padding(16.dp)
                            .semantics { this.selected = selected == id },
                    )
                }
        }
        state.categories
            .firstOrNull { it.id == selected }
            ?.let { EntriesScreen(it, state, model, counter = counter) }
    }
}
