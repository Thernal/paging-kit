package io.thernal.pagingkit.paging.impl.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.thernal.pagingkit.paging.api.presentation.components.PaginationListScope
import io.thernal.pagingkit.paging.api.presentation.model.PaginationListParams
import io.thernal.pagingkit.paging.api.presentation.model.rememberPaginationListState

@Composable
internal fun PaginationListImpl(
    params: PaginationListParams,
    modifier: Modifier = Modifier,
    content: PaginationListScope.() -> Unit,
) {
    val listState = params.listState ?: rememberPaginationListState()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState.lazyListState,
        contentPadding = params.contentPadding,
    ) {
        PaginationListScopeImpl(this).apply(content)
    }
}
