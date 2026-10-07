package io.thernal.pagingkit.sample.shared.lists

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

/**
 * A feed the way an application has one: pull to refresh, a post the user just wrote appearing at
 * the top without a reload, a deleted one disappearing, and a network that sometimes says no.
 */
class FeedViewModel(
    paginatorFactory: PaginatorFactory,
) : ViewModel() {
    private val server = FakeServer { seedPosts(POST_COUNT) }

    private val paginator: Paginator<Post> = paginatorFactory.create(
        loader = PageLoader { page, size -> server.load(page = page, size = size) },
        identity = { post -> post.id },
    )

    val state: StateFlow<PagingState<Post>> = paginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    private val mutableIsRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = mutableIsRefreshing.asStateFlow()

    private val mutableIsFlaky = MutableStateFlow(false)
    val isFlaky: StateFlow<Boolean> = mutableIsFlaky.asStateFlow()

    private var nextLocalId = -1

    init {
        onFetch()
    }

    fun onFetch() {
        viewModelScope.launch { paginator.fetch() }
    }

    /** `reset` drops back to `Idle`; the fetch after it is what brings page 0 back. */
    fun onRefresh() {
        viewModelScope.launch {
            mutableIsRefreshing.value = true
            paginator.reset()
            paginator.fetch()
            mutableIsRefreshing.value = false
        }
    }

    /** What a successful "create post" call would do with the post it got back. */
    fun onWritePost() {
        val id = nextLocalId--
        paginator.prepend(Post(id = id, author = "You", text = "A post you just wrote (local id $id)."))
    }

    fun onDelete(post: Post) {
        paginator.remove(post.id)
    }

    fun onFlakyChange(isFlaky: Boolean) {
        mutableIsFlaky.value = isFlaky
        server.failureRate = if (isFlaky) {
            FLAKY_FAILURE_RATE
        } else {
            0.0
        }
    }
}

private const val POST_COUNT = 120
private const val FLAKY_FAILURE_RATE = 0.35
