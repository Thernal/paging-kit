package io.thernal.pagingkit.sample.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSampleTokens = staticCompositionLocalOf { SampleTokens() }

/** Material 3 plus [tokens], for everything below — the sample's screens and, through the mapping, its lists. */
@Composable
fun SampleTheme(
    tokens: SampleTokens = SampleTokens(),
    content: @Composable () -> Unit,
) {
    MaterialTheme {
        CompositionLocalProvider(LocalSampleTokens provides tokens) {
            content()
        }
    }
}

object SampleTheme {
    val motion: SampleMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleTokens.current.motion
}
