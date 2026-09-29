package io.thernal.pagingkit.paging.impl.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowParams
import kotlinx.coroutines.flow.first

private enum class FlowRowContentKey { SHIMMER, CONTENT, ERROR, EMPTY }

private const val CROSSFADE_DURATION_MILLIS = 500

@Composable
internal fun <T> PaginationFlowRowImpl(
    params: PaginationFlowRowParams<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val pagingState = params.state
    // The content branch keeps drawing the last loaded list while it fades out into another state,
    // when `params.state` has already moved on.
    var lastSuccess by remember { mutableStateOf<PagingState.Success<T>?>(null) }
    SideEffect {
        if (pagingState is PagingState.Success) {
            lastSuccess = pagingState
        }
    }
    Crossfade(
        targetState = pagingState.contentKey(),
        modifier = modifier,
        animationSpec = tween(CROSSFADE_DURATION_MILLIS),
        label = "pagination-flow-row",
    ) { target ->
        when (target) {
            FlowRowContentKey.SHIMMER -> FlowRowShimmer(params)

            // Read the current state first: `lastSuccess` is only written after this composition,
            // so on the frame the first page arrives it is still null.
            FlowRowContentKey.CONTENT -> {
                val success = params.state as? PagingState.Success ?: lastSuccess
                if (success != null) {
                    FlowRowContent(state = success, params = params, content = content)
                }
            }

            FlowRowContentKey.ERROR -> StaticPagingContent(
                header = params.header,
                content = params.errorContent ?: params.emptyContent,
            )

            FlowRowContentKey.EMPTY -> StaticPagingContent(header = params.header, content = params.emptyContent)
        }
    }
}

@Composable
private fun <T> FlowRowShimmer(params: PaginationFlowRowParams<T>) {
    val count = params.shimmerItemCount
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        params.header?.invoke()
        FlowRow(Modifier.fillMaxWidth()) {
            repeat(count) { index ->
                key("paging_shimmer_$index") { params.shimmerContent?.invoke(index) }
                if (index < count - 1) {
                    params.separator?.invoke()
                }
            }
        }
    }
}

@Composable
private fun <T> FlowRowContent(
    state: PagingState.Success<T>,
    params: PaginationFlowRowParams<T>,
    content: @Composable (T) -> Unit,
) {
    val scrollState = rememberScrollState()
    PrefetchOnScroll(state = state, scrollState = scrollState, params = params)
    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        params.header?.invoke()
        FlowRow(Modifier.fillMaxWidth()) {
            state.items.forEachIndexed { index, item ->
                key(params.key(item), params.contentType(item)) { content(item) }
                if (index < state.items.lastIndex) {
                    params.separator?.invoke()
                }
            }
        }
        when (val append = state.appendStatus) {
            PagingState.AppendStatus.Loading -> params.shimmerContent?.invoke(null)
            is PagingState.AppendStatus.Failed -> params.appendErrorContent?.invoke(append.throwable, params.onFetch)
            PagingState.AppendStatus.Idle, PagingState.AppendStatus.Completed -> Unit
        }
    }
}

/**
 * Fetches once the scroll comes within [PaginationFlowRowParams.prefetchDistance] of the bottom.
 * Restarted per loaded page, and waits a frame first so it measures the layout that page produced
 * rather than the one before it.
 */
@Composable
private fun <T> PrefetchOnScroll(
    state: PagingState.Success<T>,
    scrollState: ScrollState,
    params: PaginationFlowRowParams<T>,
) {
    if (state.appendStatus != PagingState.AppendStatus.Idle) {
        return
    }
    val distancePx = with(LocalDensity.current) { params.prefetchDistance.roundToPx() }
    val onFetch by rememberUpdatedState(params.onFetch)
    LaunchedEffect(key1 = state.items.size, key2 = distancePx) {
        withFrameNanos { }
        snapshotFlow { scrollState.maxValue - scrollState.value <= distancePx }.first { isNearEnd -> isNearEnd }
        onFetch()
    }
}

@Composable
private fun StaticPagingContent(
    header: (@Composable () -> Unit)?,
    content: (@Composable ColumnScope.() -> Unit)?,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        header?.invoke()
        content?.invoke(this)
    }
}

private fun PagingState<*>.contentKey(): FlowRowContentKey {
    return when (this) {
        PagingState.Pending -> FlowRowContentKey.SHIMMER

        is PagingState.Success -> when {
            items.isNotEmpty() -> FlowRowContentKey.CONTENT
            appendStatus == PagingState.AppendStatus.Loading -> FlowRowContentKey.SHIMMER
            else -> FlowRowContentKey.EMPTY
        }

        is PagingState.Error -> FlowRowContentKey.ERROR

        PagingState.Idle -> FlowRowContentKey.EMPTY
    }
}
