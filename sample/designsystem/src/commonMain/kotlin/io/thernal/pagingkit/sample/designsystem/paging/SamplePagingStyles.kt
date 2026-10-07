package io.thernal.pagingkit.sample.designsystem.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowStyle
import io.thernal.pagingkit.paging.api.presentation.theme.PagingStyles
import io.thernal.pagingkit.sample.designsystem.SampleTheme

/**
 * The design system's tokens mapped onto paging-kit's styles — the one file an app writes in its own
 * design system. The root installs it:
 *
 * ```
 * SampleTheme {
 *     PagingTheme(styles = samplePagingStyles()) {
 *         App()
 *     }
 * }
 * ```
 *
 * Only looks are mapped; behaviour (fetch thresholds, prefetch distance) stays on each list's params.
 */
@Composable
@ReadOnlyComposable
fun samplePagingStyles(): PagingStyles {
    val motion = SampleTheme.motion
    return PagingStyles(
        flowRow = PaginationFlowRowStyle(crossfadeMillis = motion.mediumMillis),
    )
}
