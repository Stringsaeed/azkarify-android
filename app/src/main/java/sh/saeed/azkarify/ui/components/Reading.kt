package sh.saeed.azkarify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzBorder
import sh.saeed.azkarify.ui.theme.AzRadius
import sh.saeed.azkarify.ui.theme.AzSpace

/** The reader's text-size steps; scales `reading-arabic` and `reading-translation` together. */
val AzReaderScales = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)

fun TextStyle.scaled(scale: Float): TextStyle =
    if (scale == 1f) this
    else copy(fontSize = fontSize * scale, lineHeight = lineHeight.times(scale))

private fun TextUnit.times(scale: Float) = if (this == TextUnit.Unspecified) this else this * scale

/**
 * One zikr in the reader: the Arabic, right-aligned in every UI language, then the meaning in
 * English UI, then a footer with its position and the count button. Text stays selectable.
 * No shadow: reading screens stay calm.
 */
@Composable
fun AzZikrCard(
    arabic: String,
    translation: String?,
    position: String,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    transliteration: String? = null,
    count: (@Composable () -> Unit)? = null,
) {
    val colors = Az.colors
    val type = Az.type
    val shape = RoundedCornerShape(AzRadius.lg)
    val arabicStyle =
        remember(type, scale) {
            type.readingArabic.scaled(scale).copy(textAlign = TextAlign.Right, textDirection = TextDirection.Rtl)
        }
    val translationStyle = remember(type, scale) { type.readingTranslation.scaled(scale) }
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.paperRaised, shape)
            .border(AzBorder.hairline, colors.lineStrong, shape)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(AzSpace.sm)) {
                Text(arabic, Modifier.fillMaxWidth(), style = arabicStyle, color = colors.ink)
                transliteration?.let {
                    Text(it, style = type.callout.copy(fontStyle = FontStyle.Italic), color = colors.inkMuted)
                }
                translation?.let { Text(it, style = translationStyle, color = colors.ink) }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(colors.line, Offset.Zero, Offset(size.width, 0f), AzBorder.hairline.toPx())
                }
                .padding(top = AzSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(position, style = type.footnote.copy(fontFeatureSettings = "tnum"), color = colors.inkMuted)
            count?.invoke()
        }
    }
}

/**
 * The tasbih: one big round target. The whole dial counts; with [fraction] a `spark` arc shows
 * countdown progress from 12 o'clock, clockwise in every layout direction.
 */
@Composable
fun AzCounterDial(
    count: String,
    accessibilityLabel: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    fraction: Float? = null,
    onIncrease: (() -> Unit)? = null,
    onDecrease: (() -> Unit)? = null,
    increaseLabel: String = "",
    decreaseLabel: String = "",
) {
    val colors = Az.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(256.dp)
            .clickable(interaction, null, role = Role.Button, onClick = onTap)
            .semantics {
                contentDescription = accessibilityLabel
                stateDescription = count
                customActions =
                    listOfNotNull(
                        onIncrease?.let { CustomAccessibilityAction(increaseLabel) { it(); true } },
                        onDecrease?.let { CustomAccessibilityAction(decreaseLabel) { it(); true } },
                    )
            }
            .drawBehind {
                val face = 232.dp.toPx() / 2
                val c = center
                // Halo and outer ring, then the face and its rim.
                drawCircle(colors.dialRing, face + 11.dp.toPx(), c)
                drawCircle(colors.peach, face + 10.dp.toPx(), c)
                drawCircle(colors.dialRim, face, c)
                drawCircle(colors.blush, face - AzBorder.hairline.toPx(), c)
                if (fraction != null && fraction > 0f) {
                    val r = face + 5.dp.toPx()
                    drawArc(
                        colors.spark,
                        -90f,
                        360f * fraction.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(count, style = Az.type.numeral, color = colors.ink, maxLines = 1)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset((-8).dp, (-8).dp)
                .size(56.dp)
                .background(colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AzIcon(AzSymbol.add, null, size = 26.dp, tint = colors.paper)
        }
    }
}

/** The أA control: smaller and larger reading text, around the Arabic-and-Latin mark. */
@Composable
fun AzReaderSizeControl(
    scale: Float,
    onScale: (Float) -> Unit,
    smallerLabel: String,
    largerLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = Az.colors
    val index = AzReaderScales.indexOf(scale).takeIf { it >= 0 } ?: 1
    Row(
        modifier
            .background(colors.paperRaised, CircleShape)
            .border(AzBorder.hairline, colors.lineStrong, CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AzIconButton(
            AzSymbol.remove,
            smallerLabel,
            { onScale(AzReaderScales[index - 1]) },
            variant = AzIconButtonVariant.Filled,
            enabled = index > 0,
        )
        Text(
            "أA",
            Modifier.widthIn(min = 44.dp),
            style = Az.type.title3.copy(fontFamily = Az.type.readingArabic.fontFamily),
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        AzIconButton(
            AzSymbol.add,
            largerLabel,
            { onScale(AzReaderScales[index + 1]) },
            variant = AzIconButtonVariant.Filled,
            enabled = index < AzReaderScales.lastIndex,
        )
    }
}
