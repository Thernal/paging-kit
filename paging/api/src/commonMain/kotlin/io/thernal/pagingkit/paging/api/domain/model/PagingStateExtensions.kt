package io.thernal.pagingkit.paging.api.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** The loaded items, or none outside [PagingState.Success]. */
fun <T> PagingState<T>.itemsOrEmpty(): ImmutableList<T> {
    return (this as? PagingState.Success)?.items ?: persistentListOf()
}

/** A `fetch` now would load the next page rather than retry, wait or do nothing. */
val PagingState<*>.canLoadMore: Boolean
    get() {
        return this is PagingState.Success && appendStatus is PagingState.AppendStatus.Idle
    }

val PagingState<*>.isLoadingMore: Boolean
    get() {
        return this is PagingState.Success && appendStatus is PagingState.AppendStatus.Loading
    }

/** Loaded, and the last page has not been reached — whether or not the next one is loading or failed. */
val PagingState<*>.hasMore: Boolean
    get() {
        return this is PagingState.Success && appendStatus !is PagingState.AppendStatus.Completed
    }

/**
 * Maps every item and keeps the state's shape. For turning domain items into UI models between a
 * paginator and a screen.
 */
fun <T, R> PagingState<T>.map(transform: (T) -> R): PagingState<R> {
    return when (this) {
        PagingState.Idle -> PagingState.Idle

        PagingState.Pending -> PagingState.Pending

        is PagingState.Error -> this

        is PagingState.Success -> PagingState.Success(
            items = items.map(transform).toImmutableList(),
            totalCount = totalCount,
            appendStatus = appendStatus,
        )
    }
}

/**
 * Rewrites the item list of a [PagingState.Success] — a filter, a local edit — and leaves every
 * other state alone. It changes a copy the caller holds, never the paginator's own state.
 */
fun <T> PagingState<T>.mapItems(transform: (ImmutableList<T>) -> ImmutableList<T>): PagingState<T> {
    return if (this is PagingState.Success) {
        copy(items = transform(items))
    } else {
        this
    }
}
