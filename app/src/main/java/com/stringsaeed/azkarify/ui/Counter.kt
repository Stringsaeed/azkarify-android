package com.stringsaeed.azkarify.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stringsaeed.azkarify.data.AppState
import java.text.NumberFormat
import java.util.Locale

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

@Composable
fun CounterContent(initial: Int, title: String, state: AppState) {
    val p = LocalPalette.current
    var count by rememberSaveable(initial, title) { mutableIntStateOf(initial) }
    val cue = rememberCounterCue()
    val number =
        remember(state.language) {
            NumberFormat.getIntegerInstance(Locale.forLanguageTag(state.language))
        }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (title.isNotEmpty())
            SelectionContainer {
                Text(
                    title,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        Text(
            state.text(
                if (initial > 0) "Tap the number to count down" else "Tap the number to count up"
            ),
            fontSize = 15.sp,
        )
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            if (initial > 0) {
                CircularWavyProgressIndicator(
                    progress = { count.toFloat() / initial },
                    modifier = Modifier.fillMaxSize(),
                    color = p.accent,
                    trackColor = p.accent.copy(alpha = .18f),
                )
            }
            FilledTonalButton(
                onClick = {
                    val next = if (initial > 0) (count - 1).coerceAtLeast(0) else count + 1
                    if (next != count) {
                        count = next
                        cue.play(if (initial > 0 && next == 0) 2 else 0)
                    }
                },
                shapes =
                    ButtonDefaults.shapes(
                        shape = CircleShape,
                        pressedShape = MaterialTheme.shapes.extraLarge,
                    ),
                modifier =
                    Modifier.fillMaxSize().padding(8.dp).semantics {
                        contentDescription = state.text("Count: ") + number.format(count)
                        stateDescription = number.format(count)
                    },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(number.format(count), fontSize = 60.sp, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(
            onClick = {
                if (count != initial) {
                    count = initial
                    cue.play(1)
                }
            }
        ) {
            Text(state.text("Reset"), fontSize = 17.sp)
        }
    }
}
