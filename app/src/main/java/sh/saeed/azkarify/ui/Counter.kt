package sh.saeed.azkarify.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.ui.components.AzButton
import sh.saeed.azkarify.ui.components.AzButtonVariant
import sh.saeed.azkarify.ui.components.AzCounterDial
import sh.saeed.azkarify.ui.components.AzPointsBadge
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzSpace

class CounterCue {
    private var closed = false
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 15) }.getOrNull()

    fun play(cue: Int) {
        if (closed) return
        tone?.stopTone()
        tone?.startTone(
            when (cue) {
                1 -> ToneGenerator.TONE_PROP_BEEP2
                2 -> ToneGenerator.TONE_PROP_ACK
                else -> ToneGenerator.TONE_PROP_BEEP
            },
            if (cue == 2) 180 else 45,
        )
    }

    fun close() {
        if (closed) return
        closed = true
        tone?.release()
    }
}

@Composable
fun rememberCounterCue(): CounterCue {
    val cue = remember { CounterCue() }
    DisposableEffect(cue) { onDispose { cue.close() } }
    return cue
}

/**
 * The tasbih. With [initial] > 0 it counts down from a zikr's repeat count (the counter sheet);
 * otherwise it counts up (the Counter tab).
 */
@Composable
fun CounterContent(
    initial: Int,
    title: String,
    state: AppState,
    toNextPoint: Int,
    onCounted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Az.colors
    var count by rememberSaveable(initial, title) { mutableIntStateOf(initial) }
    val cue = rememberCounterCue()
    val haptic = LocalHapticFeedback.current
    fun step(delta: Int) {
        val next = (count + delta).coerceAtLeast(0)
        if (next == count) return
        count = next
        onCounted()
        val done = initial > 0 && next == 0
        haptic.performHapticFeedback(if (done) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick)
        cue.play(if (done) 2 else 0)
    }
    val tap = { step(if (initial > 0) -1 else 1) }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AzSpace.gutter, vertical = AzSpace.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        if (title.isNotEmpty())
            SelectionContainer {
                Text(
                    title,
                    Modifier.fillMaxWidth(),
                    style = Az.type.readingArabic.copy(fontSize = Az.type.readingArabic.fontSize * 0.8f, lineHeight = Az.type.readingArabic.lineHeight * 0.8f),
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        Text(
            state.text(if (initial > 0) "Tap anywhere on the circle to count down." else "Tap anywhere on the circle to count."),
            style = Az.type.callout,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        AzCounterDial(
            state.number(count),
            state.text("Count: ") + state.number(count),
            tap,
            Modifier.padding(vertical = AzSpace.xs),
            fraction = if (initial > 0) count.toFloat() / initial else null,
            onIncrease = { step(1) },
            onDecrease = { step(-1) },
            increaseLabel = state.text("Add one"),
            decreaseLabel = state.text("Remove one"),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (initial == 0)
                Text(
                    state.text("%s to your next point").replace("%s", state.number(toNextPoint)),
                    style = Az.type.headline,
                    color = colors.ink,
                )
            Text(state.text("1 point for every 33 counts"), style = Az.type.footnote, color = colors.inkMuted)
        }
        AzButton(
            state.text("Reset"),
            {
                if (count != initial) {
                    count = initial
                    cue.play(1)
                }
            },
            variant = AzButtonVariant.Text,
        )
    }
}

/** The Counter tab: the free counter, full screen. */
@Composable
fun CounterScreen(state: AppState, points: Int, toNextPoint: Int, onCounted: () -> Unit, onPoints: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = AzSpace.gutter, end = AzSpace.xs, top = AzSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.text("Counter"),
                Modifier.weight(1f).semantics { heading() },
                style = Az.type.title1,
                color = Az.colors.ink,
            )
            AzPointsBadge(state.number(points), state.text("Your points"), onPoints)
        }
        CounterContent(0, "", state, toNextPoint, onCounted, Modifier.weight(1f).padding(top = AzSpace.xxl))
    }
}
