package io.thernal.pagingkit.sample.shared.flowrow

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.PaginationFlowRow
import io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowParams
import io.thernal.pagingkit.sample.shared.ui.ExampleNote
import io.thernal.pagingkit.sample.shared.ui.ExampleScaffold
import io.thernal.pagingkit.sample.shared.ui.ShimmerBlock

private const val SHIMMER_CHIP_COUNT = 24

@Composable
fun TagCloudScreen(paginatorFactory: PaginatorFactory) {
    val model: TagCloudViewModel = viewModel { TagCloudViewModel(paginatorFactory) }
    val state by model.state.collectAsState()
    val requests by model.requests.collectAsState()

    ExampleScaffold(
        title = "Tag cloud",
        subtitle = "Wrapping chips, 30 per page. A FlowRow composes every chip, so the next page is asked " +
            "for by scroll distance.",
    ) {
        ExampleNote(text = "Pages requested: $requests. It only grows as you scroll towards the bottom.")
        PaginationFlowRow(
            params = PaginationFlowRowParams(
                state = state,
                key = { tag -> tag.id },
                onFetch = model::onFetch,
                shimmerItemCount = SHIMMER_CHIP_COUNT,
                shimmerContent = { index ->
                    // `null` is the footer shown while a later page loads.
                    val width = if (index == null) {
                        120.dp
                    } else {
                        (60 + index % 4 * 18).dp
                    }
                    ShimmerBlock(width = width, height = 32.dp, modifier = Modifier.padding(4.dp))
                },
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) { tag ->
            SuggestionChip(
                onClick = {},
                label = { Text(text = tag.label) },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
