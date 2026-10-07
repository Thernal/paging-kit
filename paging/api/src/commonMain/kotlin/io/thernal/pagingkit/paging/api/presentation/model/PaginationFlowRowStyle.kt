package io.thernal.pagingkit.paging.api.presentation.model

import androidx.compose.runtime.Immutable

/**
 * How a [PaginationFlowRow][io.thernal.pagingkit.paging.api.presentation.components.PaginationFlowRow]
 * looks, beyond the slots its params carry. Defaults come from
 * [PagingTheme.styles][io.thernal.pagingkit.paging.api.presentation.theme.PagingTheme].
 */
@Immutable
data class PaginationFlowRowStyle(
    /** How long the row fades between loading, content, empty and error. */
    val crossfadeMillis: Int = DEFAULT_CROSSFADE_MILLIS,
)

private const val DEFAULT_CROSSFADE_MILLIS = 500
