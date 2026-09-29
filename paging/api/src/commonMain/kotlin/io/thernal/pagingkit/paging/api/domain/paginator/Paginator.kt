package io.thernal.pagingkit.paging.api.domain.paginator

import io.thernal.pagingkit.paging.api.domain.model.PagingState
import kotlinx.coroutines.flow.Flow

/**
 * One paginated collection: its state, and the commands that move it. Stateful and private to its
 * owner — obtain one from [PaginatorFactory], never inject or share one.
 *
 * Call it from one thread, which in practice is the owning ViewModel's main dispatcher.
 */
interface Paginator<T> {
    /** Hot: a new collector receives the current state immediately. */
    fun observe(): Flow<PagingState<T>>

    /**
     * Loads the next page, or retries the one that failed. Answers whether more pages remain.
     *
     * Safe to call as often as a list asks: a call made while another is loading the same page
     * returns without loading it again, and a call after the last page returns `false` without
     * reaching the loader. A load that fails is reported as `Result.failure` **and** in the state.
     */
    suspend fun fetch(size: Int = DEFAULT_PAGE_SIZE): Result<Boolean>

    /** Inserts at the top, or moves an item with the same identity there. No reload. */
    fun prepend(item: T)

    /** Inserts at [index] (clamped), or moves an item with the same identity there. No reload. */
    fun insertAt(
        item: T,
        index: Int,
    )

    /** Removes every item whose identity equals [identity]. No reload. */
    fun remove(identity: Any)

    /**
     * Back to [PagingState.Idle] and page `0`. A load in flight is discarded when it returns. Nothing
     * loads until the owner calls [fetch] again.
     */
    fun reset()

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
