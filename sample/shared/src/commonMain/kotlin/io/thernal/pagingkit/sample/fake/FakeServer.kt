package io.thernal.pagingkit.sample.fake

import io.thernal.pagingkit.paging.api.domain.model.Page
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** What a failed request throws; a real loader would let its transport exception through. */
class FakeNetworkException(message: String) : Exception(message)

/**
 * A backend that pages through an in-memory list, slowly, and fails when told to. Every example
 * loads from one, so what the list does is the kit's doing rather than a network's.
 *
 * @param failureRate the chance, `0.0`–`1.0`, that any request fails.
 */
class FakeServer<T>(
    private val latency: Duration = DEFAULT_LATENCY,
    var failureRate: Double = 0.0,
    private val source: () -> List<T>,
) {
    /** Fails the next request, then clears itself. */
    var shouldFailNext: Boolean = false

    var requestCount: Int = 0
        private set

    suspend fun load(
        page: Int,
        size: Int,
    ): Page<T> {
        requestCount += 1
        delay(latency)
        if (shouldFailNext || Random.nextDouble() < failureRate) {
            shouldFailNext = false
            throw FakeNetworkException("Page ${page + 1} failed to load")
        }
        val all = source()
        val from = (page * size).coerceAtMost(all.size)
        val to = (from + size).coerceAtMost(all.size)
        return Page(
            items = all.subList(from, to).toImmutableList(),
            totalCount = all.size,
            isLastPage = to >= all.size,
        )
    }

    private companion object {
        val DEFAULT_LATENCY = 800.milliseconds
    }
}
