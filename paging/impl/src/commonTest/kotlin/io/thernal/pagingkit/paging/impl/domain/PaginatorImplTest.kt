package io.thernal.pagingkit.paging.impl.domain

import io.thernal.pagingkit.paging.api.domain.loader.PageLoader
import io.thernal.pagingkit.paging.api.domain.model.Page
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.impl.domain.paginator.PaginatorImpl
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PaginatorImplTest {
    @Test
    fun loadsNumberedPagesUntilTheLastPage() {
        runTest {
            var calls = 0
            val paginator = PaginatorImpl(
                loader = PageLoader { page, size ->
                    calls++
                    assertEquals(20, size)
                    if (page == 0) {
                        Page(items = persistentListOf(1, 2), totalCount = 3)
                    } else {
                        Page(items = persistentListOf(3), totalCount = 3, isLastPage = true)
                    }
                },
                identity = { it },
            )

            assertTrue(paginator.fetch().getOrThrow())
            assertEquals(PagingState.Success(persistentListOf(1, 2), totalCount = 3), paginator.observe().first())

            assertFalse(paginator.fetch().getOrThrow())
            assertEquals(
                PagingState.Success(
                    items = persistentListOf(1, 2, 3),
                    totalCount = 3,
                    appendStatus = PagingState.AppendStatus.Completed,
                ),
                paginator.observe().first(),
            )

            assertFalse(paginator.fetch().getOrThrow())
            assertEquals(2, calls)
        }
    }

    @Test
    fun deduplicatesIdentitiesAndSupportsListMutations() {
        runTest {
            val paginator = PaginatorImpl(
                loader = PageLoader { _, _ ->
                    Page(items = persistentListOf(1, 1, 2), totalCount = 2, isLastPage = true)
                },
                identity = { it },
            )

            assertFalse(paginator.fetch().getOrThrow())
            paginator.prepend(2)
            paginator.insertAt(item = 3, index = 1)
            paginator.remove(1)

            assertEquals(
                PagingState.Success(
                    items = persistentListOf(2, 3),
                    totalCount = 2,
                    appendStatus = PagingState.AppendStatus.Completed,
                ),
                paginator.observe().first(),
            )
        }
    }

    @Test
    fun callsQueuedBehindALoadOfTheSamePageDoNotLoadItAgain() {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val requested = mutableListOf<Int>()
            val paginator = PaginatorImpl(
                loader = PageLoader { page, _ ->
                    requested += page
                    gate.await()
                    Page(items = persistentListOf(page))
                },
                identity = { it },
            )

            // What three items near the end of a list do in one frame.
            val calls = List(3) { async { paginator.fetch() } }
            runCurrent()
            gate.complete(Unit)
            calls.forEach { it.await() }

            assertEquals(listOf(0), requested)
        }
    }

    @Test
    fun aMutationMadeWhileAPageLoadsSurvivesThePage() {
        runTest {
            var gate = CompletableDeferred(Unit)
            val paginator = PaginatorImpl(
                loader = PageLoader { page, _ ->
                    gate.await()
                    Page(items = persistentListOf(page * 10 + 1, page * 10 + 2))
                },
                identity = { it },
            )
            paginator.fetch()

            gate = CompletableDeferred()
            val load = launch { paginator.fetch() }
            runCurrent()
            paginator.prepend(99)
            paginator.remove(1)
            gate.complete(Unit)
            load.join()

            val state = assertIs<PagingState.Success<Int>>(paginator.observe().first())
            assertEquals(listOf(99, 2, 11, 12), state.items)
        }
    }

    @Test
    fun aResetWhileAPageLoadsDiscardsItWhetherItSucceedsOrFails() {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val paginator = PaginatorImpl(
                loader = PageLoader { page, _ ->
                    gate.await()
                    Page(items = persistentListOf(page))
                },
                identity = { it },
            )

            val succeeding = launch { paginator.fetch() }
            runCurrent()
            paginator.reset()
            gate.complete(Unit)
            succeeding.join()
            assertEquals(PagingState.Idle, paginator.observe().first())

            val failing = CompletableDeferred<Unit>()
            val paginatorThatFails = PaginatorImpl(
                loader = PageLoader<Int> { _, _ ->
                    failing.await()
                    error("offline")
                },
                identity = { it },
            )
            val failingLoad = launch { paginatorThatFails.fetch() }
            runCurrent()
            paginatorThatFails.reset()
            failing.complete(Unit)
            failingLoad.join()
            assertEquals(PagingState.Idle, paginatorThatFails.observe().first())
        }
    }

    @Test
    fun aFirstPageFailureIsAnErrorAndALaterOneKeepsTheItems() {
        runTest {
            var shouldFailNext = true
            val paginator = PaginatorImpl(
                loader = PageLoader { page, _ ->
                    if (shouldFailNext) {
                        error("offline")
                    }
                    Page(items = persistentListOf(page))
                },
                identity = { it },
            )

            assertTrue(paginator.fetch().isFailure)
            assertIs<PagingState.Error>(paginator.observe().first())

            shouldFailNext = false
            paginator.fetch()
            shouldFailNext = true
            assertTrue(paginator.fetch().isFailure)
            val failed = assertIs<PagingState.Success<Int>>(paginator.observe().first())
            assertEquals(listOf(0), failed.items)
            assertIs<PagingState.AppendStatus.Failed>(failed.appendStatus)

            // The retry asks for the page that failed, not the one after it.
            shouldFailNext = false
            paginator.fetch()
            val retried = assertIs<PagingState.Success<Int>>(paginator.observe().first())
            assertEquals(listOf(0, 1), retried.items)
        }
    }

    @Test
    fun aCancelledLoadLeavesNoLoadingStateBehind() {
        runTest {
            val never = CompletableDeferred<Page<Int>>()
            val paginator = PaginatorImpl(
                loader = PageLoader { page, _ ->
                    if (page == 0) {
                        Page(items = persistentListOf(0))
                    } else {
                        never.await()
                    }
                },
                identity = { it },
            )
            paginator.fetch()

            val load = launch { paginator.fetch() }
            runCurrent()
            val loading = assertIs<PagingState.Success<Int>>(paginator.observe().first())
            assertEquals(PagingState.AppendStatus.Loading, loading.appendStatus)

            load.cancel()
            load.join()
            val settled = assertIs<PagingState.Success<Int>>(paginator.observe().first())
            assertEquals(PagingState.AppendStatus.Idle, settled.appendStatus)
        }
    }

    @Test
    fun mutationsBeforeTheFirstPageAreIgnored() {
        runTest {
            val paginator = PaginatorImpl(
                loader = PageLoader { _, _ -> Page(items = (1..3).toList().toImmutableList()) },
                identity = { it },
            )
            paginator.prepend(0)
            paginator.remove(1)
            assertEquals(PagingState.Idle, paginator.observe().first())
        }
    }
}
