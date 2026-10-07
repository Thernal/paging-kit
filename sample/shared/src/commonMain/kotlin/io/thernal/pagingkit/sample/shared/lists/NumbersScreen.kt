package io.thernal.pagingkit.sample.shared.lists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.PaginationList
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import io.thernal.pagingkit.paging.preview.PagingPreviewParameterProvider
import io.thernal.pagingkit.sample.shared.ui.ExampleScaffold
import io.thernal.pagingkit.sample.shared.ui.FullMessage
import io.thernal.pagingkit.sample.shared.ui.ItemRow
import io.thernal.pagingkit.sample.shared.ui.RetryFooter
import io.thernal.pagingkit.sample.shared.ui.ShimmerRow

@Composable
fun NumbersScreen(paginatorFactory: PaginatorFactory) {
    val model: NumbersViewModel = viewModel { NumbersViewModel(paginatorFactory) }
    val state by model.state.collectAsState()
    NumbersContent(state = state, onFetch = model::onFetch)
}

// Stateless, so a preview can hand it any state without a ViewModel.
@Composable
private fun NumbersContent(
    state: PagingState<Int>,
    onFetch: () -> Unit,
) {
    ExampleScaffold(
        title = "Numbers",
        subtitle = "200 numbers, 20 per page. Scroll: the next page is asked for three items before the end.",
    ) {
        PaginationList {
            pagedItems(
                params = PagedItemsParams(
                    state = state,
                    key = { number -> number },
                    onFetch = onFetch,
                    shimmer = { ShimmerRow() },
                    emptyContent = { FullMessage(title = "No numbers", body = "Nothing to count.") },
                    errorContent = {
                        FullMessage(
                            title = "Couldn't load",
                            body = "The first page failed.",
                            action = "Retry",
                            onAction = onFetch,
                        )
                    },
                    appendErrorContent = { _, retry -> RetryFooter(message = "Couldn't load more", onRetry = retry) },
                ),
            ) { number ->
                ItemRow(title = "Number $number", subtitle = "page ${(number - 1) / 20 + 1}")
            }
        }
    }
}

/** One preview per `PagingPreviewState` — loading, loaded, footer shimmer, retry, end, error, empty. */
private class NumbersStates : PagingPreviewParameterProvider<Int>(items = (1..8).toList())

@Preview(heightDp = 640)
@Composable
private fun NumbersContentPreview(@PreviewParameter(NumbersStates::class) state: PagingState<Int>) {
    NumbersContent(state = state, onFetch = {})
}
