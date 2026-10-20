package sh.saeed.azkarify.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.saeed.azkarify.data.*
import kotlinx.coroutines.launch

@Composable
fun IntroScreen(state: AppState, finish: () -> Unit) {
    val pages =
        listOf(
            Triple("📖", "Browse azkar", "Find a zikr by topic or search for it."),
            Triple(
                "📖",
                "Read and reflect",
                "Open a topic to read its azkar or use the slideshow.",
            ),
            Triple("⭐", "Save favorites", "Keep the topics you return to close at hand."),
            Triple("⚡", "Quick Mode", "Start with morning, evening, sleep, or waking azkar."),
            Triple("📿", "Use the counter", "Tap to count, or count down repeated azkar."),
            Triple("🔔", "Set reminders", "Choose morning and evening times in Settings."),
            Triple(
                "🎨",
                "Make it yours",
                "Choose an accent color. Change language in iOS Settings.",
            ),
            Triple("💛", "Support the app", "Find ways to support future updates in Settings."),
        )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize().safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(
            onClick = finish,
            modifier = Modifier.align(Alignment.End).padding(horizontal = 24.dp),
        ) {
            Text(state.text("Skip"))
        }
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Box(
                    Modifier.size(160.dp)
                        .background(p.secondary.copy(alpha = .06f), CircleShape)
                        .border(1.5.dp, p.accent.copy(alpha = .35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(page.first, fontSize = 60.sp)
                }
                Text(
                    state.text(page.second),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp,
                )
                Text(
                    if (index == 6)
                        state.text("Choose an accent color. Change language in Settings.")
                    else state.text(page.third),
                    fontSize = 20.sp,
                    color = p.secondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                )
            }
        }
        Row(
            Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(pages.size) { i ->
                Box(
                    Modifier.size(if (i == pager.currentPage) 8.dp else 6.dp)
                        .background(
                            if (i == pager.currentPage) p.accent else p.secondary.copy(alpha = .3f),
                            CircleShape,
                        )
                )
            }
        }
        Button(
            shapes = ButtonDefaults.shapes(),
            onClick = {
                if (pager.currentPage == pages.lastIndex) finish()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier =
                Modifier.padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
        ) {
            Text(
                state.text(if (pager.currentPage == pages.lastIndex) "Get started" else "Next"),
                fontSize = 17.sp,
            )
        }
    }
}
