package io.thernal.pagingkit.paging.impl.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.presentation.components.PaginationListScope
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsGroupedParams
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import io.thernal.pagingkit.paging.api.presentation.model.ShimmerSlot
import kotlinx.collections.immutable.ImmutableList

private const val SHIMMER_CONTENT_TYPE = "paging_shimmer"

/** The slots every window renders the same way, whatever it does with its items. */
private class WindowSlots(
    val windowId: String,
    val onFetch: () -> Unit,
    val shimmerItemCount: Int,
    val shimmer: (@Composable (ShimmerSlot) -> Unit)?,
    val emptyContent: (@Composable ColumnScope.() -> Unit)?,
    val errorContent: (@Composable ColumnScope.() -> Unit)?,
    val appendErrorContent: (@Composable (Throwable, () -> Unit) -> Unit)?,
)

internal class PaginationListScopeImpl(
    lazyListScope: LazyListScope,
) : PaginationListScope,
    LazyListScope by lazyListScope {
    private val claimedWindowIds = mutableSetOf<String>()

    override fun <T> pagedItems(
        params: PagedItemsParams<T>,
        itemContent: @Composable LazyItemScope.(T) -> Unit,
    ) {
        claim(params.windowId)
        val slots = WindowSlots(
            windowId = params.windowId,
            onFetch = params.onFetch,
            shimmerItemCount = params.shimmerItemCount,
            shimmer = params.shimmer,
            emptyContent = params.emptyContent,
            errorContent = params.errorContent,
            appendErrorContent = params.appendErrorContent,
        )
        renderWindow(state = params.state, slots = slots) { rows, appendStatus ->
            itemsIndexed(
                items = rows,
                key = { _, item -> itemKey(windowId = params.windowId, key = params.key(item)) },
                contentType = { _, item -> params.contentType(item) },
            ) { index, item ->
                FetchTrigger(
                    position = index,
                    size = rows.size,
                    threshold = params.fetchThreshold,
                    appendStatus = appendStatus,
                    onFetch = params.onFetch,
                )
                Column(Modifier.animateItem()) {
                    itemContent(item)
                    rows.getOrNull(index + 1)?.let { next -> params.separator?.invoke(item, next) }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun <T, G> pagedItemsGrouped(
        params: PagedItemsGroupedParams<T, G>,
        itemContent: @Composable LazyItemScope.(T) -> Unit,
    ) {
        claim(params.windowId)
        val slots = WindowSlots(
            windowId = params.windowId,
            onFetch = params.onFetch,
            shimmerItemCount = params.shimmerItemCount,
            shimmer = params.shimmer,
            emptyContent = params.emptyContent,
            errorContent = params.errorContent,
            appendErrorContent = params.appendErrorContent,
        )
        renderWindow(state = params.state, slots = slots) { rows, appendStatus ->
            var consumed = 0
            val groupHeader = params.groupHeader
            rows.groupBy(params.groupBy).forEach { (group, groupRows) ->
                val headerKey = "${params.windowId}_group_${params.groupKey(group)}"
                if (params.hasStickyHeaders) {
                    stickyHeader(key = headerKey) { groupHeader(group) }
                } else {
                    item(key = headerKey) { groupHeader(group) }
                }
                val start = consumed
                itemsIndexed(
                    items = groupRows,
                    key = { _, item -> itemKey(windowId = params.windowId, key = params.key(item)) },
                    contentType = { _, item -> params.contentType(item) },
                ) { index, item ->
                    FetchTrigger(
                        position = start + index,
                        size = rows.size,
                        threshold = params.fetchThreshold,
                        appendStatus = appendStatus,
                        onFetch = params.onFetch,
                    )
                    itemContent(item)
                }
                consumed += groupRows.size
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

    private fun <T> renderWindow(
        state: PagingState<T>,
        slots: WindowSlots,
        rows: (ImmutableList<T>, PagingState.AppendStatus) -> Unit,
    ) {
        when (state) {
            PagingState.Idle -> placeholder(key = "${slots.windowId}_empty", content = slots.emptyContent)

            PagingState.Pending -> initialShimmer(slots)

            is PagingState.Error -> placeholder(
                key = "${slots.windowId}_error",
                content = slots.errorContent ?: slots.emptyContent,
            )

            is PagingState.Success -> when {
                state.items.isNotEmpty() -> {
                    rows(state.items, state.appendStatus)
                    appendFooter(status = state.appendStatus, slots = slots)
                }

                state.appendStatus == PagingState.AppendStatus.Loading -> initialShimmer(slots)

                else -> placeholder(key = "${slots.windowId}_empty", content = slots.emptyContent)
            }
        }
    }

    private fun initialShimmer(slots: WindowSlots) {
        val shimmer = slots.shimmer ?: return
        items(
            count = slots.shimmerItemCount,
            key = { index -> "${slots.windowId}_shimmer_$index" },
            contentType = { SHIMMER_CONTENT_TYPE },
        ) { index ->
            shimmer(ShimmerSlot.Initial(index))
        }
    }

    private fun appendFooter(
        status: PagingState.AppendStatus,
        slots: WindowSlots,
    ) {
        when (status) {
            PagingState.AppendStatus.Loading -> {
                val shimmer = slots.shimmer ?: return
                item(key = "${slots.windowId}_append_shimmer", contentType = SHIMMER_CONTENT_TYPE) {
                    shimmer(ShimmerSlot.Append)
                }
            }

            is PagingState.AppendStatus.Failed -> {
                val appendError = slots.appendErrorContent ?: return
                item(key = "${slots.windowId}_append_error") {
                    appendError(status.throwable, slots.onFetch)
                }
            }

            PagingState.AppendStatus.Idle, PagingState.AppendStatus.Completed -> Unit
        }
    }

    // Nothing to place is placing nothing: a window with no empty or error slot is simply absent.
    private fun placeholder(
        key: String,
        content: (@Composable ColumnScope.() -> Unit)?,
    ) {
        content ?: return
        item(key = key) {
            Column(modifier = Modifier.fillParentMaxSize(), content = content)
        }
    }

    private fun claim(windowId: String) {
        require(claimedWindowIds.add(windowId)) { "Duplicate pagination windowId: $windowId" }
    }
}

// A String rather than a pair: a lazy list key has to fit in an Android Bundle. Namespaced by
// window, so two windows showing the same entity do not collide in one list.
private fun itemKey(
    windowId: String,
    key: Any,
): String {
    return "${windowId}_item_$key"
}

/**
 * Asks for the next page when an item within [threshold] of the end is composed while more pages
 * exist. Several items can qualify in one frame; the paginator loads the page once regardless.
 */
@Composable
private fun FetchTrigger(
    position: Int,
    size: Int,
    threshold: Int,
    appendStatus: PagingState.AppendStatus,
    onFetch: () -> Unit,
) {
    if (position >= size - threshold && appendStatus == PagingState.AppendStatus.Idle) {
        LaunchedEffect(size) { onFetch() }
    }
}
