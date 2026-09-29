package io.thernal.pagingkit.paging.api.domain.model

import kotlinx.collections.immutable.ImmutableList

/**
 * The state of one paginated collection.
 *
 * The first page and every later page fail differently on purpose: before anything has loaded
 * there is nothing to keep, so a failure is [Error]; after that the loaded items stay on screen and
 * only [Success.appendStatus] turns [AppendStatus.Failed].
 */
sealed interface PagingState<out T> {
    /** Nothing requested yet, or [reset][io.thernal.pagingkit.paging.api.domain.paginator.Paginator.reset] since. */
    data object Idle : PagingState<Nothing>

    /** The first page is loading. */
    data object Pending : PagingState<Nothing>

    data class Success<T>(
        val items: ImmutableList<T>,
        val totalCount: Int = 0,
        val appendStatus: AppendStatus = AppendStatus.Idle,
    ) : PagingState<T>

    /** The first page failed. */
    data class Error(val throwable: Throwable) : PagingState<Nothing>

    /** What is happening at the end of a [Success] list. */
    sealed interface AppendStatus {
        /** More pages exist and none is loading: the next `fetch` loads one. */
        data object Idle : AppendStatus

        data object Loading : AppendStatus

        /** The last page has loaded. */
        data object Completed : AppendStatus

        /** The next page failed; the next `fetch` retries it. */
        data class Failed(val throwable: Throwable) : AppendStatus
    }
}
