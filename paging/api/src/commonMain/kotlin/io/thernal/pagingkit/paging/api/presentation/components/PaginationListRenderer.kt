package io.thernal.pagingkit.paging.api.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import io.thernal.pagingkit.paging.api.presentation.model.PaginationListParams

/**
 * Renders a `LazyColumn` built from a [PaginationListScope]. `impl` owns the list and the scope;
 * `api` exposes only the [PaginationList] composable below.
 */
interface PaginationListRenderer {
    @Composable
    fun Render(
        params: PaginationListParams,
        modifier: Modifier,
        content: PaginationListScope.() -> Unit,
    )
}

val LocalPaginationListRenderer =
    compositionLocalOf<PaginationListRenderer> { PreviewPaginationListRenderer }

/**
 * The renderer when none is installed. In a preview it draws the state it is handed — items,
 * shimmers, empty and error slots — so a screen previews without `impl`. Anywhere else it draws
 * nothing: a list that showed page 0 and never loaded page 1 would hide a missing installation
 * better than a blank one does.
 */
private object PreviewPaginationListRenderer : PaginationListRenderer {
    @Composable
    override fun Render(
        params: PaginationListParams,
        modifier: Modifier,
        content: PaginationListScope.() -> Unit,
    ) {
        if (LocalInspectionMode.current) {
            PreviewPaginationList(params = params, modifier = modifier, content = content)
        }
    }
}

/**
 * A lazy column holding any mix of paged windows and plain items.
 *
 * `content` is a `LazyListScope` builder, not a composable: the Compose compiler does not memoize
 * the slot lambdas built inside it, so `remember` a slot that captures changing state when its
 * recomposition cost matters.
 */
@Composable
fun PaginationList(
    params: PaginationListParams = PaginationListParams(),
    modifier: Modifier = Modifier,
    content: PaginationListScope.() -> Unit,
) {
    LocalPaginationListRenderer.current.Render(
        params = params,
        modifier = modifier,
        content = content,
    )
}
