@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.data.AppViewModel
import sh.saeed.azkarify.data.Category
import sh.saeed.azkarify.ui.components.AzChip
import sh.saeed.azkarify.ui.components.AzEmptyState
import sh.saeed.azkarify.ui.components.AzIcon
import sh.saeed.azkarify.ui.components.AzIconButton
import sh.saeed.azkarify.ui.components.AzIconWell
import sh.saeed.azkarify.ui.components.AzListRow
import sh.saeed.azkarify.ui.components.AzOrnamentShape
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.components.azGroupItem
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzSpace

/** Quick start sets: Morning & Evening, Before Sleep, Upon Waking (they replace Quick Mode). */
internal val quickStarts = listOf(27 to "Morning & Evening", 28 to "Before Sleep", 1 to "Upon Waking")

@Composable
internal fun AzkarScreen(
    state: AppState,
    model: AppViewModel,
    search: String,
    onSearch: (String) -> Unit,
    favorites: Boolean,
    onFavorites: (Boolean) -> Unit,
    onOpen: (Category) -> Unit,
    selectedCategoryId: Int?,
) {
    val colors = Az.colors
    val haptic = LocalHapticFeedback.current
    val results =
        remember(state.categories, state.favorites, search, favorites) {
            val query = search.trim()
            state.categories.filter {
                (!favorites || it.id in state.favorites) && it.title.contains(query, ignoreCase = true)
            }
        }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            state.text("Azkar"),
            Modifier
                .padding(horizontal = AzSpace.gutter)
                .padding(top = AzSpace.md, bottom = AzSpace.sm)
                .semantics { heading() },
            style = Az.type.title1,
            color = colors.ink,
        )
        ListSearchBar(search, onSearch, state)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AzSpace.gutter, vertical = AzSpace.sm),
            horizontalArrangement = Arrangement.spacedBy(AzSpace.xs),
        ) {
            AzChip(state.text("All"), !favorites, { onFavorites(false) })
            AzChip(
                state.text("Favourites"),
                favorites,
                { onFavorites(true) },
                symbol = if (favorites) AzSymbol.starFilled else AzSymbol.star,
            )
            quickStarts.forEach { (id, title) ->
                state.categories.firstOrNull { it.id == id }?.let { category ->
                    AzChip(state.text(title), category.id == selectedCategoryId, { onOpen(category) })
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().testTag("azkarCategoryList"),
            contentPadding =
                PaddingValues(start = AzSpace.md, end = AzSpace.md, top = AzSpace.xs, bottom = AzSpace.xxl),
        ) {
            when {
                state.loading -> item { Box(Modifier.fillMaxWidth().padding(AzSpace.xl)) { LoadingIndicator() } }
                state.error != null ->
                    item {
                        AzEmptyState(
                            state.text("Could not load azkar"),
                            state.error,
                            ornament = null,
                            symbol = AzSymbol.info,
                            action = state.text("Retry"),
                            onAction = model::load,
                        )
                    }
                results.isEmpty() ->
                    item {
                        if (favorites && search.isBlank())
                            AzEmptyState(
                                state.text("No favourites yet"),
                                state.text("Tap the star on any zikr to keep it here."),
                                action = state.text("Browse azkar"),
                                onAction = { onFavorites(false) },
                            )
                        else
                            AzEmptyState(
                                state.text("No results"),
                                state.text("Try another word."),
                                ornament = null,
                                symbol = AzSymbol.search,
                            )
                    }
            }
            itemsIndexed(results, key = { _, it -> it.id }, contentType = { _, _ -> "category" }) { index, category ->
                val favorite = category.id in state.favorites
                AzListRow(
                    category.title,
                    Modifier.azGroupItem(index, results.size, colors),
                    onClick = { onOpen(category) },
                    selected = category.id == selectedCategoryId,
                    divider = index > 0,
                    leading = { AzIconWell(AzSymbol.category(category.id)) },
                    trailing = {
                        IconToggleButton(
                            checked = favorite,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                model.toggleFavorite(category.id)
                            },
                        ) {
                            AzIcon(
                                if (favorite) AzSymbol.starFilled else AzSymbol.star,
                                state.text(if (favorite) "Remove from favorites" else "Add to favorites"),
                                tint = colors.accent,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ListSearchBar(query: String, onQueryChange: (String) -> Unit, state: AppState) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val colors = Az.colors
    val textFieldState = rememberTextFieldState(initialText = query)
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val currentQuery by rememberUpdatedState(query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) textFieldState.setTextAndPlaceCursorAtEnd(query)
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { value -> if (value != currentQuery) currentOnQueryChange(value) }
    }
    // The collapsed SearchBar disables the IME for an expanded search dialog. This list filters
    // inline, so its native input lives directly in a surface.
    Surface(
        color = colors.paperSunk,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = AzSpace.md).heightIn(min = 56.dp),
    ) {
        InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {
                keyboard?.hide()
                focusManager.clearFocus()
                scope.launch { searchBarState.animateToCollapsed() }
            },
            placeholder = { Text(state.text("Search azkar"), style = Az.type.body, color = colors.inkMuted) },
            leadingIcon = { AzIcon(AzSymbol.search, null, tint = colors.inkMuted) },
            trailingIcon = {
                if (query.isNotEmpty())
                    AzIconButton(AzSymbol.close, state.text("Clear search"), { onQueryChange("") })
            },
            colors =
                SearchBarDefaults.inputFieldColors(
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    cursorColor = colors.accent,
                ),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = state.text("Search azkar") },
        )
    }
}

/** The detail pane before a topic is chosen (tablets). */
@Composable
internal fun AzkarPlaceholder(state: AppState) {
    Box(Modifier.fillMaxSize().padding(AzSpace.xxl)) {
        AzEmptyState(
            state.text("Choose a topic to read its azkar"),
            state.text("Your place is kept while you read."),
            Modifier.padding(top = 120.dp),
            ornament = AzOrnamentShape.Sparkle,
        )
    }
}
