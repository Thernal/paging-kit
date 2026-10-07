package io.thernal.pagingkit.sample.shared.flowrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.thernal.pagingkit.paging.api.domain.loader.PageLoader
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.paginator.Paginator
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.shared.fake.FakeServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val words = listOf(
    "kotlin", "compose", "android", "ios", "paging", "coroutines", "flow", "lazy", "state", "metro",
    "gradle", "detekt", "material", "design", "layout", "shimmer", "retry", "cache", "network", "offline",
)

class TagCloudViewModel(
    paginatorFactory: PaginatorFactory,
) : ViewModel() {
    private val server = FakeServer {
        (0 until words.size * words.size).map { id ->
            Tag(id = id, label = "#${words.getOrElse(index = id % words.size) { "" }}${id / words.size}")
        }
    }

    private val paginator: Paginator<Tag> = paginatorFactory.create(
        loader = PageLoader { page, size -> server.load(page = page, size = size) },
        identity = { tag -> tag.id },
    )

    val state: StateFlow<PagingState<Tag>> = paginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    private val mutableRequests = MutableStateFlow(0)

    /** How many pages have been asked for — the thing to watch while scrolling. */
    val requests: StateFlow<Int> = mutableRequests.asStateFlow()

    init {
        onFetch()
    }

    fun onFetch() {
        viewModelScope.launch {
            paginator.fetch(size = TAG_PAGE_SIZE)
            mutableRequests.value = server.requestCount
        }
    }
}

private const val TAG_PAGE_SIZE = 30
