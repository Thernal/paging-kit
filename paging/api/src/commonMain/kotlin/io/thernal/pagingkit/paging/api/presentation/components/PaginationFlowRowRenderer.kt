package io.thernal.pagingkit.paging.api.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowParams

/**
 * Renders a paginated, vertically scrolling `FlowRow`. `impl` owns the hierarchy; `api` exposes
 * only the [PaginationFlowRow] composable below.
 */
interface PaginationFlowRowRenderer {
    @Composable
    fun <T> Render(
        params: PaginationFlowRowParams<T>,
        modifier: Modifier,
        content: @Composable (T) -> Unit,
    )
}

val LocalPaginationFlowRowRenderer =
    compositionLocalOf<PaginationFlowRowRenderer> { PreviewPaginationFlowRowRenderer }

/**
 * The renderer when none is installed: the state drawn once in a preview, nothing anywhere else —
 * see `PaginationListRenderer`'s default for why.
 */
private object PreviewPaginationFlowRowRenderer : PaginationFlowRowRenderer {
    @Composable
    override fun <T> Render(
        params: PaginationFlowRowParams<T>,
        modifier: Modifier,
        content: @Composable (T) -> Unit,
    ) {
        if (LocalInspectionMode.current) {
            PreviewPaginationFlowRow(params = params, modifier = modifier, content = content)
        }
    }
}

/**
 * Wrapping chips or cards, loaded page by page. It scrolls itself, so it is not placed inside
 * another vertically scrolling container.
 */
@Composable
fun <T> PaginationFlowRow(
    params: PaginationFlowRowParams<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    LocalPaginationFlowRowRenderer.current.Render(
        params = params,
        modifier = modifier,
        content = content,
    )
}
