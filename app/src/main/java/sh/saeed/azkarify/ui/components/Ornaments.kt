package sh.saeed.azkarify.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.ui.theme.Az

/** The website's ✳ ✦ ☾ ☀ glyphs, drawn on a 24×24 grid. Decorative only. */
enum class AzOrnamentShape {
    Star,
    Sparkle,
    Crescent,
    Sun,
}

private fun ornament(name: String, build: ImageVector.Builder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(build).build()

private val black = SolidColor(Color.Black)

// One stadium (x 10.1, y 0.5, 3.8 × 23, r 1.9), repeated at 0°, 45°, 90° and 135°.
private const val Bar = "M10.1 2.4A1.9 1.9 0 0 1 13.9 2.4V21.6A1.9 1.9 0 0 1 10.1 21.6Z"

private val StarVector by lazy {
    ornament("Star") {
        for (angle in listOf(0f, 45f, 90f, 135f)) {
            group(rotate = angle, pivotX = 12f, pivotY = 12f) {
                addPath(addPathNodes(Bar), fill = black)
            }
        }
    }
}

private val SparkleVector by lazy {
    ornament("Sparkle") {
        addPath(
            addPathNodes(
                "M12 0C12.8 7.2 16.8 11.2 24 12C16.8 12.8 12.8 16.8 12 24C11.2 16.8 7.2 12.8 0 12C7.2 11.2 11.2 7.2 12 0Z"
            ),
            fill = black,
        )
    }
}

private val CrescentVector by lazy {
    ornament("Crescent") {
        addPath(addPathNodes("M15.2 1.6A10.6 10.6 0 1 0 22.4 18.9 8.8 8.8 0 0 1 15.2 1.6Z"), fill = black)
    }
}

private val SunVector by lazy {
    ornament("Sun") {
        addPath(addPathNodes("M6.8 12A5.2 5.2 0 1 0 17.2 12A5.2 5.2 0 1 0 6.8 12Z"), fill = black)
        path(stroke = black, strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 1.5f); verticalLineToRelative(2.6f)
            moveTo(12f, 19.9f); verticalLineToRelative(2.6f)
            moveTo(1.5f, 12f); horizontalLineToRelative(2.6f)
            moveTo(19.9f, 12f); horizontalLineToRelative(2.6f)
            moveTo(4.6f, 4.6f); lineToRelative(1.8f, 1.8f)
            moveTo(17.6f, 17.6f); lineToRelative(1.8f, 1.8f)
            moveTo(4.6f, 19.4f); lineToRelative(1.8f, -1.8f)
            moveTo(17.6f, 6.4f); lineToRelative(1.8f, -1.8f)
        }
    }
}

fun AzOrnamentShape.vector(): ImageVector =
    when (this) {
        AzOrnamentShape.Star -> StarVector
        AzOrnamentShape.Sparkle -> SparkleVector
        AzOrnamentShape.Crescent -> CrescentVector
        AzOrnamentShape.Sun -> SunVector
    }

/**
 * An ornament tinted with a colour token. Ornaments never mirror in right-to-left layouts.
 * [spin] turns the star once per 18 s (Today and the intro only); it stops under reduce motion,
 * and the rotation is applied in the layer so it never recomposes.
 */
@Composable
fun AzOrnament(
    shape: AzOrnamentShape,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    spin: Boolean = false,
) {
    val spinning = spin && !Az.reduceMotion
    val layer =
        if (spinning) {
            val transition = rememberInfiniteTransition("ornament")
            val rotation =
                transition.animateFloat(
                    0f,
                    360f,
                    infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
                    "rotation",
                )
            Modifier.graphicsLayer { rotationZ = rotation.value }
        } else Modifier
    Image(
        shape.vector(),
        contentDescription = null,
        modifier = modifier.size(size).then(layer),
        colorFilter = ColorFilter.tint(color),
    )
}
