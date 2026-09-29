# Placeholders, errors, refresh and search

## Contents

1. What each state renders
2. Slots
3. Retry
4. Pull to refresh
5. Search and filters

## 1. What each state renders

| State | `PaginationList` window shows |
|---|---|
| `Idle` | `emptyContent` |
| `Pending` | `shimmer(ShimmerSlot.Initial(i))` × `shimmerItemCount` |
| `Error` | `errorContent` ?: `emptyContent` |
| `Success`, items, `Idle`/`Completed` | items |
| `Success`, items, `Loading` | items + `shimmer(ShimmerSlot.Append)` |
| `Success`, items, `Failed` | items + `appendErrorContent(throwable, retry)` |
| `Success`, no items | `emptyContent` (or the initial shimmer while `Loading`) |

A `null` slot draws nothing. The kit ships no default shimmer, empty, error or retry UI.

## 2. Slots

```kotlin
PagedItemsParams(
    state = state,
    key = { it.id },
    onFetch = vm::onFetch,
    shimmer = { slot ->
        when (slot) {
            is ShimmerSlot.Initial -> RowShimmer()      // slot.index available for variety
            ShimmerSlot.Append -> FooterSpinner()
        }
    },
    separator = { _, _ -> HorizontalDivider() },
    emptyContent = { EmptyState(title = "Nothing yet") },                   // ColumnScope, fills viewport
    errorContent = { ErrorState(onRetry = vm::onFetch) },                   // first page failed
    appendErrorContent = { error, retry -> RetryFooter(error.message, retry) }, // later page failed
)
```

- `emptyContent`/`errorContent` get a `ColumnScope` sized to the list's viewport
  (`fillParentMaxSize`); center with `Modifier.weight(1f)` + `Arrangement.Center`.
- To tell "idle" from "loaded, empty" in `emptyContent`, read `state` from the enclosing scope.
- Slot lambdas are built inside a `LazyListScope` builder and are not memoized by the compiler;
  `remember` a slot that captures fast-changing state if its cost matters.

## 3. Retry

- **First page:** `errorContent` has no retry parameter — call the ViewModel's fetch (`vm::onFetch`).
- **Later page:** `appendErrorContent` receives `retry` (the window's `onFetch`).
- A retry loads the **same** page again; the page number only advances on success.
- **Nothing retries automatically.** The list's trigger fires only while `appendStatus` is `Idle`.
  Without `appendErrorContent`, a failed later page ends the list silently.
- Automatic retry (backoff) belongs in the loader or the ViewModel, not the UI:
  `fetch().onFailure { delay(backoff); fetch() }`.

## 4. Pull to refresh

The kit has no refresh container. Wrap the list in the design system's:

```kotlin
// ViewModel
private val mutableIsRefreshing = MutableStateFlow(false)
val isRefreshing = mutableIsRefreshing.asStateFlow()

fun onRefresh() {
    viewModelScope.launch {
        mutableIsRefreshing.value = true
        paginator.reset()
        paginator.fetch()
        mutableIsRefreshing.value = false
    }
}

// Screen (Material 3)
PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = vm::onRefresh) {
    PaginationList { … }
}
```

- `reset()` then `fetch()`, on the same paginator. `reset` alone leaves the list `Idle` (empty).
- During the refresh the window shows its initial shimmer. To keep old items visible, cache the last
  `Success` in the ViewModel and expose it until the new `Success` arrives.
- Locally prepended items disappear on refresh unless the backend now returns them.

## 5. Search and filters

One paginator; the loader reads the criteria the paginator was **last reset for**; every change is a
reset and a fetch.

```kotlin
private val query = MutableStateFlow("")      // bound to the text field
private var activeQuery = ""                  // what the pages on screen belong to

private val paginator = paginatorFactory.create(
    loader = PageLoader { page, size -> api.search(activeQuery, page, size) },
    identity = { it.id },
)

init {
    onFetch()
    viewModelScope.launch {
        query.drop(1).debounce(300.milliseconds).collect { next ->
            activeQuery = next
            paginator.reset()
            paginator.fetch()
        }
    }
}

fun onQueryChange(value: String) {
    query.value = value
}
```

- A page for an older query still in flight is discarded when it returns — `reset` moved the
  generation on. No request ids.
- Why `activeQuery` and not `query.value`: the field runs ahead of the debounce; a later page the list
  asks for in between would load for the new text and append under the old results.
- Filters and sort orders: the same shape — hold the applied value, set it before `reset()`.
- Never a paginator per query: the old one keeps loading, unobserved.
