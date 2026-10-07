package io.thernal.pagingkit.sample.shared.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SHIMMER_PULSE_MILLIS = 700

private const val SHIMMER_MIN_ALPHA = 0.35f

/** A pulsing grey block; the kit draws no placeholder of its own, so the sample brings one. */
@Composable
fun ShimmerBlock(
    width: Dp?,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = SHIMMER_MIN_ALPHA,
        animationSpec = infiniteRepeatable(animation = tween(SHIMMER_PULSE_MILLIS), repeatMode = RepeatMode.Reverse),
        label = "shimmer-alpha",
    )
    val sized = if (width == null) {
        modifier.fillMaxWidth()
    } else {
        modifier.width(width)
    }
    Box(
        modifier = sized
            .height(height)
            .alpha(alpha)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)),
    )
}
