package io.thernal.pagingkit.paging.api.presentation.model

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.thernal.pagingkit.paging.api.domain.model.PagingState

/**
 * Everything optional about a
 * [PaginationFlowRow][io.thernal.pagingkit.paging.api.presentation.components.PaginationFlowRow].
 *
 * @property prefetchDistance how close to the bottom of its own scroll the row has to get before
 *   [onFetch] is called. A `FlowRow` composes every item, so "an item near the end was composed"
 *   says nothing about what the user can see; distance does. Content shorter than the viewport keeps
 *   fetching until it fills it or the last page arrives.
 * @property header scrolls with the items, above them, in every state.
 * @property shimmerContent drawn [shimmerItemCount] times with its index while the first page
 *   loads, and once with `null` as a footer while a later page loads.
 * @property separator drawn between two adjacent items, inside the flow.
 */
@Immutable
data class PaginationFlowRowParams<T>(
    val state: PagingState<T>,
    val key: (T) -> Any,
    val onFetch: () -> Unit = {},
    val prefetchDistance: Dp = DEFAULT_PREFETCH_DISTANCE,
    val shimmerItemCount: Int = DEFAULT_SHIMMER_ITEM_COUNT,
    val contentType: (T) -> Any? = { null },
    val header: (@Composable () -> Unit)? = null,
    val shimmerContent: (@Composable (Int?) -> Unit)? = null,
    val separator: (@Composable () -> Unit)? = null,
    val errorContent: (@Composable ColumnScope.() -> Unit)? = null,
    val emptyContent: (@Composable ColumnScope.() -> Unit)? = null,
    val appendErrorContent: (@Composable (Throwable, () -> Unit) -> Unit)? = null,
)

private val DEFAULT_PREFETCH_DISTANCE = 240.dp
