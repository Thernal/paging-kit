@file:Suppress("TooGenericExceptionCaught")

package io.thernal.pagingkit.paging.impl.domain.paginator

import io.thernal.pagingkit.paging.api.domain.loader.PageLoader
import io.thernal.pagingkit.paging.api.domain.model.Page
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.model.hasMore
import io.thernal.pagingkit.paging.api.domain.model.itemsOrEmpty
import io.thernal.pagingkit.paging.api.domain.paginator.Paginator
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [page] counts successfully loaded pages; [generation] counts resets. A load remembers the
 * generation it started in and is discarded if a reset happened while it was away — whatever it
 * returns, success, failure or cancellation.
 */
class PaginatorImpl<T>(
    private val loader: PageLoader<T>,
    private val identity: (T) -> Any,
) : Paginator<T> {
    private val state = MutableStateFlow<PagingState<T>>(PagingState.Idle)
    private val mutex = Mutex()
    private var page = 0
    private var generation = 0

    override fun observe(): Flow<PagingState<T>> {
        return state.asStateFlow()
    }

    override suspend fun fetch(size: Int): Result<Boolean> {
        // Read before queueing on the lock: a list asks for the next page from every item near its
        // end, and those calls must load it once rather than load the next three pages.
        val requestedPage = page
        val requestedGeneration = generation
        return mutex.withLock {
            if (page != requestedPage || generation != requestedGeneration) {
                return@withLock Result.success(state.value.hasMore)
            }
            val before = state.value
            if (before is PagingState.Success && before.appendStatus == PagingState.AppendStatus.Completed) {
                return@withLock Result.success(false)
            }
            state.value = loadingState(before)
            load(before = before, size = size)
        }
    }

    private suspend fun load(
        before: PagingState<T>,
        size: Int,
    ): Result<Boolean> {
        val startGeneration = generation
        return try {
            val loaded = loader.load(page = page, size = size)
            if (generation != startGeneration) {
                return Result.success(false)
            }
            page += 1
            state.value = loadedState(loaded)
            Result.success(!loaded.isLastPage)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            if (generation == startGeneration) {
                state.value = failedState(error)
            }
            Result.failure(error)
        } finally {
            // Still loading here means the load was cancelled. Left as it was, the state would show
            // a load that nobody is running any more.
            if (generation == startGeneration && state.value.isLoading()) {
                state.value = rolledBackState(before)
            }
        }
    }

    private fun PagingState<T>.isLoading(): Boolean {
        return when (this) {
            PagingState.Pending -> true
            is PagingState.Success -> appendStatus == PagingState.AppendStatus.Loading
            PagingState.Idle, is PagingState.Error -> false
        }
    }

    private fun loadingState(current: PagingState<T>): PagingState<T> {
        if (current is PagingState.Success && current.items.isNotEmpty()) {
            return current.copy(appendStatus = PagingState.AppendStatus.Loading)
        }
        return PagingState.Pending
    }

    // Built on the state as it is now, not as it was when the load started: a prepend or remove
    // made while the page was in flight must survive the page arriving.
    private fun loadedState(loaded: Page<T>): PagingState<T> {
        val current = state.value.itemsOrEmpty()
        return PagingState.Success(
            items = (current + loaded.items).distinctBy(identity).toImmutableList(),
            totalCount = loaded.totalCount,
            appendStatus = if (loaded.isLastPage) {
                PagingState.AppendStatus.Completed
            } else {
                PagingState.AppendStatus.Idle
            },
        )
    }

    private fun failedState(error: Throwable): PagingState<T> {
        val current = state.value
        if (current is PagingState.Success && current.items.isNotEmpty()) {
            return current.copy(appendStatus = PagingState.AppendStatus.Failed(error))
        }
        return PagingState.Error(error)
    }

    private fun rolledBackState(before: PagingState<T>): PagingState<T> {
        val current = state.value
        if (current is PagingState.Success && before is PagingState.Success) {
            return current.copy(appendStatus = before.appendStatus)
        }
        return before
    }

    override fun prepend(item: T) {
        insertAt(item = item, index = 0)
    }

    override fun insertAt(
        item: T,
        index: Int,
    ) {
        state.update { current ->
            if (current !is PagingState.Success) {
                return@update current
            }
            val id = identity(item)
            val wasPresent = current.items.any { identity(it) == id }
            val items = current.items.filterNot { identity(it) == id }.toMutableList()
            items.add(index = index.coerceIn(0, items.size), element = item)
            current.copy(
                items = items.toImmutableList(),
                totalCount = if (wasPresent) {
                    current.totalCount
                } else {
                    current.totalCount + 1
                },
            )
        }
    }

    override fun remove(identity: Any) {
        state.update { current ->
            if (current !is PagingState.Success) {
                return@update current
            }
            val items = current.items.filterNot { this.identity(it) == identity }.toImmutableList()
            current.copy(
                items = items,
                totalCount = (current.totalCount - (current.items.size - items.size)).coerceAtLeast(0),
            )
        }
    }

    override fun reset() {
        generation += 1
        page = 0
        state.value = PagingState.Idle
    }
}
