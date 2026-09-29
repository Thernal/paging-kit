package io.thernal.pagingkit.paging.api.presentation.components

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsGroupedParams
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import kotlinx.collections.immutable.ImmutableList

/**
 * The receiver of a [PaginationList]'s content. A `LazyListScope`, so plain `item`/`items` mix
 * freely with any number of paged windows.
 */
interface PaginationListScope : LazyListScope {
    /** One paged window: placeholders, items, the append footer and the fetch trigger. */
    fun <T> pagedItems(
        params: PagedItemsParams<T>,
        itemContent: @Composable LazyItemScope.(T) -> Unit,
    )

    /** One paged window whose items sit under group headers. */
    fun <T, G> pagedItemsGrouped(
        params: PagedItemsGroupedParams<T, G>,
        itemContent: @Composable LazyItemScope.(T) -> Unit,
    )

    /**
     * Runs [block] only once [state] has loaded at least one item — for a section header that
     * should not appear above a shimmer or an empty window.
     */
    fun <T> ifLoaded(
        state: PagingState<T>,
        block: PaginationListScope.(ImmutableList<T>) -> Unit,
    )
}
