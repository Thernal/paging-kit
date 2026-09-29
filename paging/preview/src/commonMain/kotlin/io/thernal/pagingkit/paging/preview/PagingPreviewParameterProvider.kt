package io.thernal.pagingkit.paging.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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

/** The throwable preview states carry; its message is what an error slot showing it will print. */
class PreviewPagingException(
    message: String = "Preview error",
) : Exception(message)

/**
 * Every [PagingPreviewState] of one list, for `@PreviewParameter`. Subclass it once per item type with
 * sample items — a preview parameter class needs a no-argument constructor, so the generic class
 * itself cannot be named in the annotation:
 *
 * ```
 * private class FeedStates : PagingPreviewParameterProvider<Post>(items = listOf(post1, post2))
 *
 * @Preview(heightDp = 600)
 * @Composable
 * private fun FeedPreview(@PreviewParameter(FeedStates::class) state: PagingState<Post>) {
 *     FeedContent(state = state, onFetch = {})
 * }
 * ```
 *
 * No renderer has to be installed: in a preview the kit's default renderers draw the state they are
 * handed. Each preview is named after its [PagingPreviewState].
 *
 * @param states which states to preview, in this order.
 */
open class PagingPreviewParameterProvider<T>(
    items: List<T>,
    private val states: List<PagingPreviewState> = PagingPreviewState.entries,
    error: Throwable = PreviewPagingException(),
) : PreviewParameterProvider<PagingState<T>> {
    private val loaded = items.toImmutableList()

    private val stateByKind: Map<PagingPreviewState, PagingState<T>> = mapOf(
        PagingPreviewState.LOADING to PagingState.Pending,
        PagingPreviewState.LOADED to PagingState.Success(items = loaded, totalCount = loaded.size),
        PagingPreviewState.LOADING_MORE to PagingState.Success(
            items = loaded,
            totalCount = loaded.size,
            appendStatus = PagingState.AppendStatus.Loading,
        ),
        PagingPreviewState.LOAD_MORE_FAILED to PagingState.Success(
            items = loaded,
            totalCount = loaded.size,
            appendStatus = PagingState.AppendStatus.Failed(error),
        ),
        PagingPreviewState.END_REACHED to PagingState.Success(
            items = loaded,
            totalCount = loaded.size,
            appendStatus = PagingState.AppendStatus.Completed,
        ),
        PagingPreviewState.ERROR to PagingState.Error(error),
        PagingPreviewState.EMPTY to PagingState.Success(
            items = persistentListOf(),
            appendStatus = PagingState.AppendStatus.Completed,
        ),
    )

    override val values: Sequence<PagingState<T>>
        get() {
            return states.asSequence().mapNotNull { state -> stateByKind[state] }
        }

    override fun getDisplayName(index: Int): String? {
        return states.getOrNull(index)?.name
    }
}
