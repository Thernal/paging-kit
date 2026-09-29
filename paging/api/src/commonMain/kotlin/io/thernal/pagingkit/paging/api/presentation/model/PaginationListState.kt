package io.thernal.pagingkit.paging.api.presentation.model

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

/** The scroll state of a `PaginationList`, with the two questions a screen above one usually asks. */
@Stable
class PaginationListState(val lazyListState: LazyListState) : ScrollableState by lazyListState {
    /** Read in composition or a snapshot flow; it changes only when the answer does. */
    val isAtTop: State<Boolean> = derivedStateOf {
        lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
    }

    suspend fun scrollToTop() {
        lazyListState.animateScrollToItem(0)
    }
}

@Composable
fun rememberPaginationListState(state: LazyListState = rememberLazyListState()): PaginationListState {
    return remember(state) { PaginationListState(state) }
}
