package io.thernal.pagingkit.paging.api.domain.loader

import io.thernal.pagingkit.paging.api.domain.model.Page

/**
 * Loads one page. [page] is zero-based and counts pages the paginator has successfully loaded since
 * it was created or last reset — a failed load is retried with the same number.
 *
 * Throw to fail: the paginator turns any exception except `CancellationException` into an error
 * state, so a loader never has to catch its own transport errors.
 */
fun interface PageLoader<T> {
    suspend fun load(
        page: Int,
        size: Int,
    ): Page<T>
}
