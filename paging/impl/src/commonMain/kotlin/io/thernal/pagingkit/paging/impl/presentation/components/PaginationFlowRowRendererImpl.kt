package io.thernal.pagingkit.paging.impl.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.thernal.pagingkit.paging.api.presentation.components.PaginationFlowRowRenderer
import io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowParams

class PaginationFlowRowRendererImpl : PaginationFlowRowRenderer {
    @Composable
    override fun <T> Render(
        params: PaginationFlowRowParams<T>,
        modifier: Modifier,
        content: @Composable (T) -> Unit,
    ) {
        PaginationFlowRowImpl(
            params = params,
            modifier = modifier,
            content = content,
        )
    }
}
