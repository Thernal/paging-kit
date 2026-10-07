package io.thernal.pagingkit.sample.shared.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.PaginationList
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import io.thernal.pagingkit.paging.api.presentation.model.PaginationListParams
import io.thernal.pagingkit.paging.api.presentation.model.rememberPaginationListState
import io.thernal.pagingkit.sample.shared.ui.ExampleScaffold
import io.thernal.pagingkit.sample.shared.ui.FullMessage
import io.thernal.pagingkit.sample.shared.ui.ItemRow
import io.thernal.pagingkit.sample.shared.ui.RetryFooter
import io.thernal.pagingkit.sample.shared.ui.SectionHeader
import io.thernal.pagingkit.sample.shared.ui.ShimmerRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(paginatorFactory: PaginatorFactory) {
    val model: FeedViewModel = viewModel { FeedViewModel(paginatorFactory) }
    val state by model.state.collectAsState()
    val isRefreshing by model.isRefreshing.collectAsState()
    val isFlaky by model.isFlaky.collectAsState()
    val listState = rememberPaginationListState()
    val isAtTop by listState.isAtTop
    val scope = rememberCoroutineScope()

    ExampleScaffold(
        title = "Post feed",
        subtitle = "Pull to refresh, write a post, delete one. Turn on the flaky network to meet the retry footer.",
    ) {
        FeedControls(isFlaky = isFlaky, onFlakyChange = model::onFlakyChange, onWritePost = model::onWritePost)
        Box(modifier = Modifier.fillMaxSize()) {
            // The kit ships no refresh container; wrapping the list in the design system's own is
            // all pull-to-refresh takes.
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = model::onRefresh) {
                PaginationList(params = PaginationListParams(listState = listState)) {
                    ifLoaded(state) { posts ->
                        item(key = "feed_count") {
                            val total = (state as? PagingState.Success)?.totalCount ?: 0
                            SectionHeader(text = "${posts.size} of $total posts")
                        }
                    }
                    pagedItems(
                        params = PagedItemsParams(
                            state = state,
                            key = { post -> post.id },
                            onFetch = model::onFetch,
                            shimmer = { ShimmerRow() },
                            separator = { _, _ -> HorizontalDivider() },
                            emptyContent = { FullMessage(title = "No posts", body = "Nothing here yet.") },
                            errorContent = {
                                FullMessage(
                                    title = "Couldn't load the feed",
                                    body = "The first page failed, so there is nothing to keep on screen.",
                                    action = "Try again",
                                    onAction = model::onFetch,
                                )
                            },
                            appendErrorContent = { error, retry ->
                                RetryFooter(message = error.message ?: "Couldn't load more", onRetry = retry)
                            },
                        ),
                    ) { post ->
                        ItemRow(
                            title = post.author,
                            subtitle = post.text,
                            trailing = { TextButton(onClick = { model.onDelete(post) }) { Text(text = "Delete") } },
                        )
                    }
                }
            }
            if (!isAtTop) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.scrollToTop() } },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                ) {
                    Text(text = "↑", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun FeedControls(
    isFlaky: Boolean,
    onFlakyChange: (Boolean) -> Unit,
    onWritePost: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(onClick = onWritePost) { Text(text = "Write a post") }
        Text(text = "Flaky network", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = isFlaky, onCheckedChange = onFlakyChange)
    }
}
