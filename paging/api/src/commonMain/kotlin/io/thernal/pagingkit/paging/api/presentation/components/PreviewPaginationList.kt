package io.thernal.pagingkit.paging.api.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsGroupedParams
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import io.thernal.pagingkit.paging.api.presentation.model.PaginationListParams
import io.thernal.pagingkit.paging.api.presentation.model.ShimmerSlot
import kotlinx.collections.immutable.ImmutableList

/**
 * What a `PaginationList` looks like in a preview: the state it was handed, drawn once. It never
 * fetches, keys nothing and animates nothing — `impl` owns all of that — so a preview shows what a
 * screen looks like in a state, not how it pages.
 */
@Composable
internal fun PreviewPaginationList(
    params: PaginationListParams,
    modifier: Modifier,
    content: PaginationListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = params.listState?.lazyListState ?: rememberLazyListState(),
        contentPadding = params.contentPadding,
    ) {
        PreviewPaginationListScope(this).apply(content)
    }
}

private class PreviewPaginationListScope(
    lazyListScope: LazyListScope,
) : PaginationListScope,
    LazyListScope by lazyListScope {
    override fun <T> pagedItems(
        params: PagedItemsParams<T>,
        itemContent: @Composable LazyItemScope.(T) -> Unit,
    ) {
        val slots = PreviewSlots(
            shimmerItemCount = params.shimmerItemCount,
            shimmer = params.shimmer,
            emptyContent = params.emptyContent,
            errorContent = params.errorContent,
            appendErrorContent = params.appendErrorContent,
        )
        previewWindow(state = params.state, slots = slots) { rows ->
            itemsIndexed(items = rows) { index, item ->
                itemContent(item)
                rows.getOrNull(index + 1)?.let { next -> params.separator?.invoke(item, next) }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun <T, G> pagedItemsGrouped(
        params: PagedItemsGroupedParams<T, G>,
        itemContent: @Composable LazyItemScope.(T) -> Unit,
    ) {
        val slots = PreviewSlots(
            shimmerItemCount = params.shimmerItemCount,
            shimmer = params.shimmer,
            emptyContent = params.emptyContent,
            errorContent = params.errorContent,
            appendErrorContent = params.appendErrorContent,
        )
        previewWindow(state = params.state, slots = slots) { rows ->
            val groupHeader = params.groupHeader
            rows.groupBy(params.groupBy).forEach { (group, groupRows) ->
                if (params.hasStickyHeaders) {
                    stickyHeader { groupHeader(group) }
                } else {
                    item { groupHeader(group) }
                }
                itemsIndexed(items = groupRows) { _, item -> itemContent(item) }
            }
        }
    }

    override fun <T> ifLoaded(
        state: PagingState<T>,
        block: PaginationListScope.(ImmutableList<T>) -> Unit,
    ) {
        val success = state as? PagingState.Success ?: return
        if (success.items.isNotEmpty()) {
            block(success.items)
        }
    }

    private fun <T> previewWindow(
        state: PagingState<T>,
        slots: PreviewSlots,
        rows: (ImmutableList<T>) -> Unit,
    ) {
        when (state) {
            PagingState.Idle -> fullViewport(slots.emptyContent)

            PagingState.Pending -> initialShimmer(slots)

            is PagingState.Error -> fullViewport(slots.errorContent ?: slots.emptyContent)

            is PagingState.Success -> when {
                state.items.isNotEmpty() -> {
                    rows(state.items)
                    footer(status = state.appendStatus, slots = slots)
                }

                state.appendStatus == PagingState.AppendStatus.Loading -> initialShimmer(slots)

                else -> fullViewport(slots.emptyContent)
            }
        }
    }

    private fun initialShimmer(slots: PreviewSlots) {
        val shimmer = slots.shimmer ?: return
        items(count = slots.shimmerItemCount) { index -> shimmer(ShimmerSlot.Initial(index)) }
    }

    private fun footer(
        status: PagingState.AppendStatus,
        slots: PreviewSlots,
    ) {
        when (status) {
            PagingState.AppendStatus.Loading -> {
                val shimmer = slots.shimmer ?: return
                item { shimmer(ShimmerSlot.Append) }
            }

            is PagingState.AppendStatus.Failed -> {
                val appendError = slots.appendErrorContent ?: return
                item { appendError(status.throwable) {} }
            }

            PagingState.AppendStatus.Idle, PagingState.AppendStatus.Completed -> Unit
        }
    }

    private fun fullViewport(content: (@Composable ColumnScope.() -> Unit)?) {
        content ?: return
        item { Column(modifier = Modifier.fillParentMaxSize(), content = content) }
    }
}

/** The slots a window draws the same way whatever it does with its items. */
private class PreviewSlots(
    val shimmerItemCount: Int,
    val shimmer: (@Composable (ShimmerSlot) -> Unit)?,
    val emptyContent: (@Composable ColumnScope.() -> Unit)?,
    val errorContent: (@Composable ColumnScope.() -> Unit)?,
    val appendErrorContent: (@Composable (Throwable, () -> Unit) -> Unit)?,
)
