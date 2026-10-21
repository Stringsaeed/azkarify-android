package sh.saeed.azkarify.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.ui.components.AzButton
import sh.saeed.azkarify.ui.components.AzButtonVariant
import sh.saeed.azkarify.ui.components.AzEyebrow
import sh.saeed.azkarify.ui.components.AzIcon
import sh.saeed.azkarify.ui.components.AzJourneyArch
import sh.saeed.azkarify.ui.components.AzOrnament
import sh.saeed.azkarify.ui.components.AzOrnamentShape
import sh.saeed.azkarify.ui.components.AzPageDots
import sh.saeed.azkarify.ui.components.AzSticker
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzSpace

private class IntroPage(val eyebrow: String, val title: String, val body: String, val art: Int)

/** First launch: three pages over a mist disc, then Today. */
@Composable
fun IntroScreen(state: AppState, finish: () -> Unit) {
    val colors = Az.colors
    val pages =
        listOf(
            IntroPage(
                state.text("Welcome to Azkarify"),
                state.text("Small moments.\nDeeper connection."),
                state.text("Daily azkar, gently paced through your day."),
                0,
            ),
            IntroPage(
                state.text("Journeys"),
                state.text("A path through\nyour day."),
                state.text("Morning, prayers and evening, one step at a time. Progress starts fresh each day."),
                1,
            ),
            IntroPage(
                state.text("Read and count"),
                state.text("Read, count,\nreturn."),
                state.text("Tap to count each zikr. Reminders and your progress stay on this device."),
                2,
            ),
        )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    Column(Modifier.fillMaxSize().background(colors.paper).safeDrawingPadding()) {
        AzButton(
            state.text("Skip"),
            finish,
            Modifier.align(Alignment.End).padding(horizontal = AzSpace.xs),
            AzButtonVariant.Text,
        )
        HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 0) { index ->
            val page = pages[index]
            // Centred in the space above the dots; it still scrolls when the page is too short.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AzSpace.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.size(AzSpace.md))
                IntroArt(page.art, state, current = index == pager.currentPage)
                Spacer(Modifier.size(AzSpace.xl))
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AzSpace.sm),
                ) {
                    AzEyebrow(page.eyebrow)
                    Text(page.title, Modifier.semantics { heading() }, style = Az.type.display, color = colors.ink)
                    Text(page.body, style = Az.type.body, color = colors.inkSoft)
                }
            }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AzSpace.gutter)
                .padding(top = AzSpace.md, bottom = AzSpace.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AzPageDots(pages.size, pager.currentPage, Modifier.weight(1f).clearAndSetSemantics {})
            AzButton(
                state.text(if (last) "Get started" else "Next"),
                {
                    if (last) finish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                trailing = if (last) null else AzSymbol.forward,
            )
        }
    }
}

/** Hero art: a mist disc with two orbits, and the page's motif in the middle. */
@Composable
private fun IntroArt(art: Int, state: AppState, current: Boolean) {
    val colors = Az.colors
    Box(Modifier.size(280.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(colors.mist, radius = size.minDimension * 0.42f)
            val stroke = Stroke(1.dp.toPx())
            drawCircle(colors.orbit, radius = size.minDimension * 0.47f, style = stroke)
            drawCircle(colors.orbit.copy(alpha = 0.5f), radius = size.minDimension * 0.33f, style = stroke)
        }
        when (art) {
            0 -> {
                AzJourneyArch(width = 132.dp, height = 176.dp) {
                    Text("ذكر", style = Az.type.readingArabic.copy(fontSize = Az.type.display.fontSize), color = colors.ink)
                }
                AzSticker(
                    state.text("Less scrolling. More soul."),
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    large = true,
                )
            }
            1 -> {
                Box(Modifier.size(112.dp).background(colors.sage, CircleShape), contentAlignment = Alignment.Center) {
                    AzIcon(AzSymbol.sunrise, null, size = 48.dp, tint = colors.ink)
                }
                AzOrnament(AzOrnamentShape.Sun, colors.spark, Modifier.align(Alignment.TopEnd).padding(36.dp), 32.dp)
                AzOrnament(AzOrnamentShape.Crescent, colors.forest, Modifier.align(Alignment.BottomStart).padding(40.dp), 28.dp)
            }
            else -> {
                Box(Modifier.size(132.dp).background(colors.peach, CircleShape), contentAlignment = Alignment.Center) {
                    Text(state.number(33), style = Az.type.numeral, color = colors.ink)
                }
                AzOrnament(AzOrnamentShape.Star, colors.spark, Modifier.align(Alignment.TopStart).padding(40.dp), 36.dp, spin = current)
                AzOrnament(AzOrnamentShape.Sparkle, colors.forest, Modifier.align(Alignment.BottomEnd).padding(44.dp), 24.dp)
            }
        }
    }
}
