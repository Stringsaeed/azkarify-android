@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package sh.saeed.azkarify.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.saeed.azkarify.R
import sh.saeed.azkarify.data.*

@Composable
internal fun HomeScreen(
    state: AppState,
    model: AppViewModel,
    search: String,
    favorites: Boolean,
    go: (Destination) -> Unit,
    selectedCategoryId: Int? = null,
    showOverview: Boolean = true,
) {
    val p = LocalPalette.current
    val results =
        remember(state.categories, state.favorites, search, favorites) {
            state.categories.filter {
                (!favorites || it.id in state.favorites) &&
                    it.title.contains(search.trim(), ignoreCase = true)
            }
        }
    Box(Modifier.fillMaxSize()) {
        BotanicalBackground()
        LazyColumn(
            Modifier.fillMaxSize().testTag("azkarCategoryList"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showOverview && !favorites && search.isBlank()) {
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(p.background.copy(alpha = .9f))
                            .border(1.dp, p.accent.copy(alpha = .18f), RoundedCornerShape(24.dp))
                            .clickable { go(Destination.Journeys) }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                state.text("Your daily journeys"),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                state.text("Small steps, from morning to night"),
                                fontSize = 15.sp,
                                color = p.secondary,
                            )
                            Row(
                                Modifier.padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForward,
                                    null,
                                    Modifier.padding(end = 6.dp).size(16.dp),
                                    tint = p.accent,
                                )
                                Text(
                                    state.text("Explore journeys"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.accent,
                                )
                            }
                        }
                        Image(
                            painterResource(R.drawable.journey_crescent),
                            null,
                            Modifier.size(width = 68.dp, height = 92.dp),
                        )
                    }
                }
                item {
                    Text(
                        state.text("Find the right zikr faster"),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                item {
                    Text(state.text("All azkar"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (state.loading) item { LoadingIndicator(Modifier.padding(24.dp)) }
            else if (state.error != null)
                item {
                    Text(state.text("Could not load azkar"))
                    Text(state.error)
                    TextButton(onClick = model::load) { Text(state.text("Retry")) }
                }
            else if (results.isEmpty())
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (favorites) "⭐" else "⌕", fontSize = 40.sp)
                        Text(
                            state.text(if (favorites) "No favorites yet" else "No results"),
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            items(results, key = { it.id }, contentType = { "category" }) {
                CategoryRow(it, state, model, it.id == selectedCategoryId) {
                    go(Destination.Category(it.id))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    state: AppState,
    model: AppViewModel,
    selected: Boolean,
    open: () -> Unit,
) {
    val p = LocalPalette.current
    AppCard {
        ListItem(
            selected = selected,
            onClick = open,
            leadingContent = { Text(emoji(category.id), fontSize = 21.sp) },
            trailingContent = {
                IconToggleButton(
                    checked = category.id in state.favorites,
                    onCheckedChange = { model.toggleFavorite(category.id) },
                ) {
                    Icon(
                        if (category.id in state.favorites) Icons.Outlined.Star
                        else Icons.Outlined.StarBorder,
                        state.text(
                            if (category.id in state.favorites) "Remove from favorites"
                            else "Add to favorites"
                        ),
                        tint = p.accent,
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = p.background),
        ) {
            Text(category.title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
