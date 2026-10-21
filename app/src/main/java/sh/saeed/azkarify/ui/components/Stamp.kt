package sh.saeed.azkarify.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzDuration
import sh.saeed.azkarify.ui.theme.azTween

/**
 * The hard offset shadow of a stamped element, mirrored in right-to-left layouts.
 *
 * With an [interactionSource], pressing moves the element onto its shadow (the press feedback
 * of every stamped control). The shadow stays put outside the moving layer, so the moved
 * element covers it; the movement is read in the layer and draw phases only.
 */
@Composable
fun Modifier.azStamp(
    offset: DpOffset,
    shape: Shape,
    interactionSource: InteractionSource? = null,
    color: Color = Az.colors.stampShadow,
): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shadow =
        drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val dx = offset.x.toPx() * if (rtl) -1 else 1
            val dy = offset.y.toPx()
            onDrawBehind { translate(dx, dy) { drawOutline(outline, color) } }
        }
    if (interactionSource == null) return this.then(shadow)
    val pressed by interactionSource.collectIsPressedAsState()
    val progress =
        animateFloatAsState(
            if (pressed) 1f else 0f,
            azTween(if (Az.reduceMotion) 0 else AzDuration.press),
            label = "stamp",
        )
    return this.then(shadow).graphicsLayer {
        val p = progress.value
        translationX = offset.x.toPx() * p * if (rtl) -1 else 1
        translationY = offset.y.toPx() * p
    }
}
