package io.thernal.pagingkit.sample.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.PaginationList
import io.thernal.pagingkit.paging.api.presentation.model.PagedItemsGroupedParams
import io.thernal.pagingkit.sample.ui.ExampleScaffold
import io.thernal.pagingkit.sample.ui.ItemRow
import io.thernal.pagingkit.sample.ui.SectionHeader
import io.thernal.pagingkit.sample.ui.ShimmerRow

@Composable
fun ContactsScreen(paginatorFactory: PaginatorFactory) {
    val model: ContactsViewModel = viewModel { ContactsViewModel(paginatorFactory) }
    val state by model.state.collectAsState()

    ExampleScaffold(
        title = "Contacts",
        subtitle = "Grouped by first letter with sticky headers. A letter split across two pages keeps one header.",
    ) {
        PaginationList {
            pagedItemsGrouped(
                params = PagedItemsGroupedParams(
                    state = state,
                    key = { contact -> contact.id },
                    onFetch = model::onFetch,
                    groupBy = { contact -> contact.name.first().uppercaseChar() },
                    groupKey = { letter -> letter },
                    groupHeader = { letter -> SectionHeader(text = letter.toString()) },
                    hasStickyHeaders = true,
                    windowId = "contacts",
                    shimmer = { ShimmerRow() },
                ),
            ) { contact ->
                ItemRow(title = contact.name, subtitle = contact.phone)
            }
        }
    }
}
