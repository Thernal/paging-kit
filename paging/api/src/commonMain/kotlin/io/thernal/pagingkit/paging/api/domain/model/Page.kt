package io.thernal.pagingkit.paging.api.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One page as a [PageLoader][io.thernal.pagingkit.paging.api.domain.loader.PageLoader] returns it.
 * Only what the paginator reads: a backend's own page envelope is mapped onto this at the edge.
 *
 * @property totalCount the size of the whole collection as the backend reports it, or `0` when it
 *   does not; the paginator only passes it through to [PagingState.Success.totalCount].
 * @property isLastPage `true` stops the paginator: no further `fetch` reaches the loader until a
 *   `reset`.
 */
data class Page<T>(
    val items: ImmutableList<T> = persistentListOf(),
    val totalCount: Int = 0,
    val isLastPage: Boolean = false,
)
