package io.thernal.pagingkit.sample.shared.lists

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
 * The whole contract in one class: a factory in, a private paginator, its state out, and one
 * command the list calls when it wants more.
 */
class NumbersViewModel(
    paginatorFactory: PaginatorFactory,
) : ViewModel() {
    private val server = FakeServer { (1..NUMBER_COUNT).toList() }

    private val paginator: Paginator<Int> = paginatorFactory.create(
        loader = PageLoader { page, size -> server.load(page = page, size = size) },
        identity = { number -> number },
    )

    val state: StateFlow<PagingState<Int>> = paginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    // Nothing loads on its own: an idle list is an empty list, so the owner asks for page 0.
    init {
        onFetch()
    }

    fun onFetch() {
        viewModelScope.launch { paginator.fetch() }
    }
}

private const val NUMBER_COUNT = 200
