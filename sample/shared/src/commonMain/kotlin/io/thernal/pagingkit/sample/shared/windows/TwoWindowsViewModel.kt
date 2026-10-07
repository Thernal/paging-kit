package io.thernal.pagingkit.sample.shared.windows

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
import kotlin.time.Duration.Companion.milliseconds

private val pinnedIds = listOf(3, 7, 12, 30, 41, 58)

/**
 * Two paginators, two states, two fetch commands — one per window. The pinned articles are also in
 * the full list, on purpose: the same id appears in both windows of one list.
 */
class TwoWindowsViewModel(
    paginatorFactory: PaginatorFactory,
) : ViewModel() {
    private val pinnedServer = FakeServer(latency = 500.milliseconds) { pinnedIds.map(::article) }
    private val allServer = FakeServer(latency = 1200.milliseconds) { (1..ARTICLE_COUNT).map(::article) }

    private val pinnedPaginator: Paginator<Article> = paginatorFactory.create(
        loader = PageLoader { page, size -> pinnedServer.load(page = page, size = size) },
        identity = { article -> article.id },
    )
    private val allPaginator: Paginator<Article> = paginatorFactory.create(
        loader = PageLoader { page, size -> allServer.load(page = page, size = size) },
        identity = { article -> article.id },
    )

    val pinned: StateFlow<PagingState<Article>> = pinnedPaginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )
    val all: StateFlow<PagingState<Article>> = allPaginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    init {
        onFetchPinned()
        onFetchAll()
    }

    // A small page size, so the pinned window pages too — and completes, which is what lets the
    // window below it start asking for its own pages.
    fun onFetchPinned() {
        viewModelScope.launch { pinnedPaginator.fetch(size = PINNED_PAGE_SIZE) }
    }

    fun onFetchAll() {
        viewModelScope.launch { allPaginator.fetch() }
    }
}

private fun article(id: Int): Article {
    return Article(id = id, title = "Article $id")
}

private const val PINNED_PAGE_SIZE = 4
private const val ARTICLE_COUNT = 90
