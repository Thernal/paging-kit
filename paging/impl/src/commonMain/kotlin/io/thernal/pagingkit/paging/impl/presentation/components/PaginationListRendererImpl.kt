package io.thernal.pagingkit.paging.impl.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.thernal.pagingkit.paging.api.presentation.components.PaginationListRenderer
import io.thernal.pagingkit.paging.api.presentation.components.PaginationListScope
import io.thernal.pagingkit.paging.api.presentation.model.PaginationListParams

class PaginationListRendererImpl : PaginationListRenderer {
    @Composable
    override fun Render(
        params: PaginationListParams,
        modifier: Modifier,
        content: PaginationListScope.() -> Unit,
    ) {
        PaginationListImpl(
            params = params,
            modifier = modifier,
            content = content,
        )
    }
}
