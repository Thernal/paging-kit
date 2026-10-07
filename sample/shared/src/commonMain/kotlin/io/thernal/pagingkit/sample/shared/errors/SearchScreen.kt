package io.thernal.pagingkit.sample.shared.errors

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.PaginationList
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import io.thernal.pagingkit.sample.shared.ui.ExampleScaffold
import io.thernal.pagingkit.sample.shared.ui.FullMessage
import io.thernal.pagingkit.sample.shared.ui.ItemRow
import io.thernal.pagingkit.sample.shared.ui.RetryFooter
import io.thernal.pagingkit.sample.shared.ui.ShimmerRow

@Composable
fun SearchScreen(paginatorFactory: PaginatorFactory) {
    val model: SearchViewModel = viewModel { SearchViewModel(paginatorFactory) }
    val query by model.query.collectAsState()
    val state by model.state.collectAsState()

    ExampleScaffold(
        title = "Search as you type",
        subtitle = "Each query resets the paginator. Type fast: a slower answer to an older query never shows up.",
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = model::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            label = { Text(text = "City") },
            singleLine = true,
        )
        PaginationList {
            pagedItems(
                params = PagedItemsParams(
                    state = state,
                    key = { place -> place.id },
                    onFetch = model::onFetch,
                    shimmer = { ShimmerRow() },
                    emptyContent = { FullMessage(title = "No matches", body = "Nothing contains \"$query\".") },
                    appendErrorContent = { _, retry -> RetryFooter(message = "Couldn't load more", onRetry = retry) },
                ),
            ) { place ->
                ItemRow(title = place.name, subtitle = place.id)
            }
        }
    }
}
