package io.thernal.pagingkit.sample.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.PaginationList
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams
import io.thernal.pagingkit.sample.ui.ExampleNote
import io.thernal.pagingkit.sample.ui.ExampleScaffold
import io.thernal.pagingkit.sample.ui.ItemRow
import io.thernal.pagingkit.sample.ui.SectionHeader
import io.thernal.pagingkit.sample.ui.ShimmerRow

private const val PINNED_SHIMMER_COUNT = 2

@Composable
fun TwoWindowsScreen(paginatorFactory: PaginatorFactory) {
    val model: TwoWindowsViewModel = viewModel { TwoWindowsViewModel(paginatorFactory) }
    val pinned by model.pinned.collectAsState()
    val all by model.all.collectAsState()

    ExampleScaffold(
        title = "Two windows",
        subtitle = "Pinned and All are two paginators in one LazyColumn, each with its own placeholders and pages.",
    ) {
        ExampleNote(text = "Articles 3, 7, 12… are in both windows. Keys are namespaced by windowId, so that is fine.")
        PaginationList {
            item(key = "pinned_header") { SectionHeader(text = "Pinned") }
            pagedItems(
                params = PagedItemsParams(
                    state = pinned,
                    key = { article -> article.id },
                    onFetch = model::onFetchPinned,
                    windowId = "pinned",
                    shimmerItemCount = PINNED_SHIMMER_COUNT,
                    shimmer = { ShimmerRow() },
                ),
            ) { article ->
                ItemRow(title = "📌 ${article.title}", subtitle = "pinned")
            }
            // Only once the first window has something, so the second header does not sit
            // between two shimmers.
            ifLoaded(pinned) {
                item(key = "all_header") { SectionHeader(text = "All articles") }
            }
            pagedItems(
                params = PagedItemsParams(
                    state = all,
                    key = { article -> article.id },
                    onFetch = model::onFetchAll,
                    windowId = "all",
                    shimmer = { ShimmerRow() },
                ),
            ) { article ->
                ItemRow(title = article.title, subtitle = "id ${article.id}")
            }
        }
    }
}
