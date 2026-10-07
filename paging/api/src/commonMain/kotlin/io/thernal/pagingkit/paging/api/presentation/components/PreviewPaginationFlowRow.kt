package io.thernal.pagingkit.paging.api.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowParams

/**
 * What a `PaginationFlowRow` looks like in a preview: the state it was handed, drawn once, with no
 * crossfade and no fetching.
 */
@Composable
internal fun <T> PreviewPaginationFlowRow(
    params: PaginationFlowRowParams<T>,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        params.header?.invoke()
        when (val state = params.state) {
            PagingState.Idle -> params.emptyContent?.invoke(this)
            PagingState.Pending -> PreviewShimmerFlow(params)
            is PagingState.Error -> (params.errorContent ?: params.emptyContent)?.invoke(this)
            is PagingState.Success -> PreviewFlowSuccess(state = state, params = params, content = content)
        }
    }
}

@Composable
private fun <T> ColumnScope.PreviewFlowSuccess(
    state: PagingState.Success<T>,
    params: PaginationFlowRowParams<T>,
    content: @Composable (T) -> Unit,
) {
    when {
        state.items.isNotEmpty() -> {
            PreviewFlowItems(items = state.items, separator = params.separator, content = content)
            PreviewFlowFooter(status = state.appendStatus, params = params)
        }

        state.appendStatus == PagingState.AppendStatus.Loading -> PreviewShimmerFlow(params)

        else -> params.emptyContent?.invoke(this)
    }
}

@Composable
private fun <T> PreviewFlowItems(
    items: List<T>,
    separator: (@Composable () -> Unit)?,
    content: @Composable (T) -> Unit,
) {
    FlowRow(Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            content(item)
            if (index < items.lastIndex) {
                separator?.invoke()
            }
        }
    }
}

@Composable
private fun <T> PreviewShimmerFlow(params: PaginationFlowRowParams<T>) {
    val shimmer = params.shimmerContent ?: return
    FlowRow(Modifier.fillMaxWidth()) {
        repeat(params.shimmerItemCount) { index -> shimmer(index) }
    }
}

@Composable
private fun <T> PreviewFlowFooter(
    status: PagingState.AppendStatus,
    params: PaginationFlowRowParams<T>,
) {
    when (status) {
        PagingState.AppendStatus.Loading -> params.shimmerContent?.invoke(null)
        is PagingState.AppendStatus.Failed -> params.appendErrorContent?.invoke(status.throwable) {}
        PagingState.AppendStatus.Idle, PagingState.AppendStatus.Completed -> Unit
    }
}
