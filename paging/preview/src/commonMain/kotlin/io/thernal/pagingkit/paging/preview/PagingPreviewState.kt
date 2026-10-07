package io.thernal.pagingkit.paging.preview

/** One `PagingState` a list can be in, as a preview shows it. */
enum class PagingPreviewState {
    /** `Pending`: the first page is loading — the initial shimmer. */
    LOADING,

    /** `Success`, more pages to come — the items. */
    LOADED,

    /** `Success` + `AppendStatus.Loading` — the items and the footer shimmer. */
    LOADING_MORE,

    /** `Success` + `AppendStatus.Failed` — the items and `appendErrorContent`. */
    LOAD_MORE_FAILED,

    /** `Success` + `AppendStatus.Completed` — the items, no footer. */
    END_REACHED,

    /** `Error`: the first page failed — `errorContent`. */
    ERROR,

    /** `Success` with no items — `emptyContent`. */
    EMPTY,
}
