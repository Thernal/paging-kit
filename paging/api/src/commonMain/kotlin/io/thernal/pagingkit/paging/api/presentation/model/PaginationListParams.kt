package io.thernal.pagingkit.paging.api.presentation.model

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable

/**
 * @property listState hoisted scroll state, for scrolling to the top or reading [PaginationListState.isAtTop].
 *   `null` lets the list remember its own.
 */
@Immutable
data class PaginationListParams(
    val listState: PaginationListState? = null,
    val contentPadding: PaddingValues = PaddingValues(),
)
