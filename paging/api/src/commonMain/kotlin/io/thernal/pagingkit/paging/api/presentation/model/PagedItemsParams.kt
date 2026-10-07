package io.thernal.pagingkit.paging.api.presentation.model

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import io.thernal.pagingkit.paging.api.domain.model.PagingState

/**
 * Everything optional about one paged window of a
 * [PaginationList][io.thernal.pagingkit.paging.api.presentation.components.PaginationList].
 *
 * @property key a stable identity per item. The list namespaces it by [windowId], so two windows
 *   may show the same item. Its `toString()` must be unique within the window.
 * @property onFetch called when an item within [fetchThreshold] of the end is composed while more
 *   pages exist. Wire it straight to `Paginator.fetch`, which ignores repeats.
 * @property windowId unique per window within one list; it keys the window's placeholders.
 * @property shimmer drawn [shimmerItemCount] times while the first page loads, and once as a
 *   footer while a later page loads. `null` draws no placeholder.
 * @property separator drawn between two adjacent items.
 * @property emptyContent fills the viewport when the list is idle or loaded empty.
 * @property errorContent fills the viewport when the first page failed; falls back to
 *   [emptyContent].
 * @property appendErrorContent the footer when a later page failed, handed the error and a retry
 *   callback. `null` draws nothing — and nothing retries until the owner fetches again.
 */
@Immutable
data class PagedItemsParams<T>(
    val state: PagingState<T>,
    val key: (T) -> Any,
    val onFetch: () -> Unit,
    val contentType: (T) -> Any? = { null },
    val fetchThreshold: Int = DEFAULT_FETCH_THRESHOLD,
    val windowId: String = DEFAULT_WINDOW_ID,
    val shimmerItemCount: Int = DEFAULT_SHIMMER_ITEM_COUNT,
    val shimmer: (@Composable (ShimmerSlot) -> Unit)? = null,
    val separator: (@Composable (before: T, after: T) -> Unit)? = null,
    val emptyContent: (@Composable ColumnScope.() -> Unit)? = null,
    val errorContent: (@Composable ColumnScope.() -> Unit)? = null,
    val appendErrorContent: (@Composable (Throwable, () -> Unit) -> Unit)? = null,
) {
    /** The defaults every paged params type shares. */
    companion object {
        internal const val DEFAULT_FETCH_THRESHOLD = 3
        internal const val DEFAULT_WINDOW_ID = "paged"
        internal const val DEFAULT_SHIMMER_ITEM_COUNT = 15
    }
}
