package io.thernal.pagingkit.paging.api.presentation.model

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import io.thernal.pagingkit.paging.api.domain.model.PagingState

/**
 * [PagedItemsParams] for a window whose items are shown under group headers. Items are grouped in
 * order of first appearance, so a group that reappears on a later page is merged into the header
 * already shown rather than getting a second one.
 *
 * @property groupHeader drawn once per group; the one composable slot that is required, because a
 *   grouped window without headers is a plain window.
 * @property hasStickyHeaders pins the current group's header to the top while its items scroll.
 */
@Immutable
data class PagedItemsGroupedParams<T, G>(
    val state: PagingState<T>,
    val key: (T) -> Any,
    val onFetch: () -> Unit,
    val groupBy: (T) -> G,
    val groupKey: (G) -> Any,
    val groupHeader: @Composable LazyItemScope.(G) -> Unit,
    val hasStickyHeaders: Boolean = false,
    val contentType: (T) -> Any? = { null },
    val fetchThreshold: Int = DEFAULT_FETCH_THRESHOLD,
    val windowId: String = DEFAULT_WINDOW_ID,
    val shimmerItemCount: Int = DEFAULT_SHIMMER_ITEM_COUNT,
    val shimmer: (@Composable (ShimmerSlot) -> Unit)? = null,
    val emptyContent: (@Composable ColumnScope.() -> Unit)? = null,
    val errorContent: (@Composable ColumnScope.() -> Unit)? = null,
    val appendErrorContent: (@Composable (Throwable, () -> Unit) -> Unit)? = null,
)
