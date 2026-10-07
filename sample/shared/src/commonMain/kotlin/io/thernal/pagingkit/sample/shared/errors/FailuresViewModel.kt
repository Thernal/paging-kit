package io.thernal.pagingkit.sample.shared.errors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.thernal.pagingkit.paging.api.domain.loader.PageLoader
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.paginator.Paginator
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.shared.fake.FakeServer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Starts with its first request set to fail, so the screen opens on the first-page error. After
 * that, "Fail the next page" arms one failure at a time.
 */
class FailuresViewModel(
    paginatorFactory: PaginatorFactory,
) : ViewModel() {
    private val server = FakeServer { (1..ROW_COUNT).toList() }.apply { shouldFailNext = true }

    private val paginator: Paginator<Int> = paginatorFactory.create(
        loader = PageLoader { page, size -> server.load(page = page, size = size) },
        identity = { row -> row },
    )

    val state: StateFlow<PagingState<Int>> = paginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    init {
        onFetch()
    }

    fun onFetch() {
        viewModelScope.launch { paginator.fetch() }
    }

    fun onFailNextPage() {
        server.shouldFailNext = true
    }

    fun onStartOver() {
        server.shouldFailNext = true
        paginator.reset()
        onFetch()
    }
}

private const val ROW_COUNT = 60
