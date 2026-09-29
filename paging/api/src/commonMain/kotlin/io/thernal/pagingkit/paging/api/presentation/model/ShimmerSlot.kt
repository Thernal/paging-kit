package io.thernal.pagingkit.paging.api.presentation.model

import androidx.compose.runtime.Immutable

/** Which placeholder a shimmer slot is drawing, so one lambda can size both. */
@Immutable
sealed interface ShimmerSlot {
    /** One of the placeholder rows shown while the first page loads. */
    data class Initial(val index: Int) : ShimmerSlot

    /** The single footer shown while a later page loads. */
    data object Append : ShimmerSlot
}
