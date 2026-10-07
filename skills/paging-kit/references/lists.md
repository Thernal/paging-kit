# PaginationList, windows and groups

## Contents

1. One window
2. `PagedItemsParams` reference
3. Several windows in one list
4. Grouped windows
5. Scroll state and padding

## 1. One window

```kotlin
@Composable
fun FeedView(model: FeedViewModel) {
    val state by model.state.collectAsState()
    PaginationList {
        pagedItems(
            params = PagedItemsParams(
                state = state,
                key = { post -> post.id },
                onFetch = model::onFetch,
                shimmer = { PostShimmer() },
                appendErrorContent = { _, retry -> RetryFooter(onRetry = retry) },
            ),
        ) { post ->
            PostRow(post)
        }
    }
}
```

Imports: `io.thernal.pagingkit.paging.api.presentation.components.PaginationList`,
`io.thernal.pagingkit.paging.api.presentation.model.PagedItemsParams`.

- `PaginationList(params = PaginationListParams(), modifier = Modifier, content)` — a `LazyColumn`
  with `fillMaxSize()`. Give it bounded height (a `weight(1f)` in a `Column`, the rest of a `Scaffold`).
- The trailing lambda is the item (`@Composable LazyItemScope.(T) -> Unit`); everything optional is on
  the params.
- Items use `animateItem()` automatically.
- The next page is requested when an item within `fetchThreshold` (3) of the end is composed while
  `appendStatus` is `Idle`.

## 2. `PagedItemsParams` reference

| Field | Default | Notes |
|---|---|---|
| `state` | — | `PagingState<T>` |
| `key` | — | same id as the paginator's `identity`; `toString()` unique in the window |
| `onFetch` | — | `vm::onFetch` → `paginator.fetch()` |
| `contentType` | `{ null }` | |
| `fetchThreshold` | `3` | |
| `windowId` | `"paged"` | unique per window per list |
| `shimmerItemCount` | `15` | |
| `shimmer` | `null` | `(ShimmerSlot) -> Unit`: `Initial(index)` × count, `Append` × 1 |
| `separator` | `null` | `(before, after) -> Unit` between adjacent items |
| `emptyContent` | `null` | `ColumnScope`, fills viewport; `Idle` and loaded-empty |
| `errorContent` | `null` | `ColumnScope`, fills viewport; first page failed; falls back to `emptyContent` |
| `appendErrorContent` | `null` | `(Throwable, retry) -> Unit`; later page failed |

## 3. Several windows in one list

```kotlin
PaginationList {
    item(key = "pinned_header") { SectionHeader("Pinned") }
    pagedItems(
        params = PagedItemsParams(state = pinned, key = { it.id }, onFetch = vm::onFetchPinned, windowId = "pinned"),
    ) { PinnedRow(it) }

    ifLoaded(pinned) {
        item(key = "all_header") { SectionHeader("All") }
    }

    pagedItems(
        params = PagedItemsParams(state = all, key = { it.id }, onFetch = vm::onFetchAll, windowId = "all"),
    ) { ArticleRow(it) }
}
```

- `PaginationListScope` is a `LazyListScope`: `item`, `items`, `stickyHeader` mix with windows.
- **Distinct `windowId` per window**; a duplicate throws `IllegalArgumentException: Duplicate
  pagination windowId: …` on first composition.
- Item keys become `"<windowId>_item_<key>"`, so the same entity may appear in two windows. Plain
  items need keys that cannot collide with that pattern.
- `ifLoaded(state) { items -> … }` emits only once `state` is `Success` with items.
- One paginator, one `state`, one `onFetch` per window. A lower window only pages once scrolled to.

## 4. Grouped windows

```kotlin
pagedItemsGrouped(
    params = PagedItemsGroupedParams(
        state = state,
        key = { it.id },
        onFetch = vm::onFetch,
        groupBy = { contact -> contact.name.first().uppercaseChar() },
        groupKey = { letter -> letter },
        groupHeader = { letter -> LetterHeader(letter) },   // LazyItemScope.(G) -> Unit
        hasStickyHeaders = true,
        windowId = "contacts",
    ),
) { contact -> ContactRow(contact) }
```

- Groups are formed over all loaded items in order of first appearance; a group continuing on the next
  page stays under its header. **Sort on the backend by the grouping field.**
- `groupKey` must be stable and unique per group (it keys the header).
- No `separator` here; draw dividers inside the row.
- Date sections: `groupBy = { it.createdAt.date }`, `groupKey = { it.toString() }`.

## 5. Scroll state and padding

```kotlin
val listState = rememberPaginationListState()
val isAtTop by listState.isAtTop
val scope = rememberCoroutineScope()

PaginationList(params = PaginationListParams(listState = listState, contentPadding = PaddingValues(bottom = 80.dp))) { … }

if (!isAtTop) FloatingActionButton(onClick = { scope.launch { listState.scrollToTop() } }) { … }
```

- `PaginationListState(lazyListState)` is a `ScrollableState`; `lazyListState` is exposed for anything
  else (`firstVisibleItemIndex`, nested scroll).
- `rememberPaginationListState(rememberLazyListState(initialFirstVisibleItemIndex = n))` to start elsewhere.
- Pull to refresh: wrap `PaginationList` in the design system's container — see
  [states-and-errors.md](states-and-errors.md#pull-to-refresh).
