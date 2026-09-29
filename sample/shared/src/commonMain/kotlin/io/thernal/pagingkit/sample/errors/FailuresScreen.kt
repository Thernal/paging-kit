package io.thernal.pagingkit.sample.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.PaginationList
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import io.thernal.pagingkit.sample.ui.ExampleNote
import io.thernal.pagingkit.sample.ui.ExampleScaffold
import io.thernal.pagingkit.sample.ui.FullMessage
import io.thernal.pagingkit.sample.ui.ItemRow
import io.thernal.pagingkit.sample.ui.RetryFooter
import io.thernal.pagingkit.sample.ui.ShimmerRow

@Composable
fun FailuresScreen(paginatorFactory: PaginatorFactory) {
    val model: FailuresViewModel = viewModel { FailuresViewModel(paginatorFactory) }
    val state by model.state.collectAsState()

    ExampleScaffold(
        title = "Failures and retry",
        subtitle = "The first page fails differently from a later one: nothing to keep versus a list to keep.",
    ) {
        ExampleNote(text = "State: ${state.describe()}")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(onClick = model::onFailNextPage) { Text(text = "Fail the next page") }
            OutlinedButton(onClick = model::onStartOver) { Text(text = "Start over") }
        }
        PaginationList {
            pagedItems(
                params = PagedItemsParams(
                    state = state,
                    key = { row -> row },
                    onFetch = model::onFetch,
                    shimmer = { ShimmerRow() },
                    errorContent = {
                        FullMessage(
                            title = "PagingState.Error",
                            body = "The first page failed. There are no items to keep, so the whole window is this.",
                            action = "Retry",
                            onAction = model::onFetch,
                        )
                    },
                    appendErrorContent = { error, retry ->
                        RetryFooter(message = "AppendStatus.Failed — ${error.message.orEmpty()}", onRetry = retry)
                    },
                ),
            ) { row ->
                ItemRow(title = "Row $row", subtitle = "loaded")
            }
        }
    }
}

private fun PagingState<*>.describe(): String {
    return when (this) {
        PagingState.Idle -> "Idle"
        PagingState.Pending -> "Pending"
        is PagingState.Error -> "Error(${throwable.message.orEmpty()})"
        is PagingState.Success -> "Success(${items.size} items, ${appendStatus::class.simpleName.orEmpty()})"
    }
}
