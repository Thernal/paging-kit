package io.thernal.pagingkit.paging.api.presentation.theme

import androidx.compose.runtime.Immutable
import io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowStyle

/**
 * Every look paging-kit decides, mapped once from the app's design system and installed with
 * [PagingTheme]. Everything else a list shows — shimmer, separators, empty and error states — is the
 * app's own composables, passed as slots.
 */
@Immutable
data class PagingStyles(
    val flowRow: PaginationFlowRowStyle = PaginationFlowRowStyle(),
)
