# Loaders and the paginator

## Contents

1. Writing a `PageLoader`
2. The ViewModel shape
3. `fetch` semantics
4. Editing without a reload
5. `reset`
6. Reading `PagingState`

## 1. Writing a `PageLoader`

```kotlin
class OrderPageLoader(private val api: OrderApi) : PageLoader<Order> {
    override suspend fun load(page: Int, size: Int): Page<Order> {
        val response = api.orders(page = page, size = size)
        return Page(
            items = response.content.map { it.toDomain() }.toImmutableList(),
            totalCount = response.totalElements,
            isLastPage = response.last,
        )
    }
}
```

Or inline: `PageLoader { page, size -> api.orders(page, size).toPage() }`.

| Backend shape | Map it as |
|---|---|
| page number + `last`/`totalPages` | `isLastPage = response.last` or `page + 1 >= totalPages` |
| offset/limit | `api.list(offset = page * size, limit = size)` |
| no "last" flag | `isLastPage = items.size < size` |
| no total | `totalCount = 0` (or a running count if the UI needs one) |
| cursor / keyset | keep `nextCursor` in the loader; reset it when `page == 0`; `isLastPage = nextCursor == null` |

Cursor loader:

```kotlin
class CursorLoader(private val api: Api) : PageLoader<Item> {
    private var nextCursor: String? = null

    override suspend fun load(page: Int, size: Int): Page<Item> {
        if (page == 0) nextCursor = null
        val response = api.items(cursor = nextCursor, limit = size)
        nextCursor = response.next
        return Page(items = response.items.toImmutableList(), isLastPage = response.next == null)
    }
}
```

Rules:

- **Throw to fail.** Do not catch and return an empty page — that reads as "loaded, empty".
- **Never catch `CancellationException`.**
- Switch dispatcher inside the loader if the client is blocking (`withContext(Dispatchers.IO)`); the
  paginator is called from the main thread.
- The loader may read the ViewModel's current filter/query — but the value the paginator was **last
  reset for**, not a live input (see [states-and-errors.md](states-and-errors.md#search-and-filters)).

## 2. The ViewModel shape

```kotlin
class FeedViewModel(
    paginatorFactory: PaginatorFactory,
    api: FeedApi,
) : ViewModel() {
    private val paginator: Paginator<Post> = paginatorFactory.create(
        loader = FeedPageLoader(api),
        identity = { post -> post.id },
    )

    val state: StateFlow<PagingState<Post>> = paginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    init {
        onFetch()
    }

    fun onFetch() {
        viewModelScope.launch { paginator.fetch() }
    }
}
```

- `identity` = the entity id. It de-duplicates pages (first occurrence wins) and locates items for
  edits. The list's `key` must be the same id.
- Page size: `paginator.fetch(size = 30)`; default `Paginator.DEFAULT_PAGE_SIZE` = 20. Use one size per
  paginator — changing it mid-collection makes offset maths wrong for page-number backends.
- To map to UI models, expose `paginator.observe().map { it.map(Post::toUi) }.stateIn(…)`.
- One paginator per collection. A screen with two paged sections holds two.

## 3. `fetch` semantics

`suspend fun fetch(size: Int = 20): Result<Boolean>` — `true` if more pages remain.

| State when called | Effect |
|---|---|
| `Idle` / `Error` | `Pending`, then loads page 0 (or the page that failed) |
| `Success` + `Idle` / `Failed` | `AppendStatus.Loading`, then the next page |
| `Success` + `Completed` | returns `false`, loader not called |
| same page already loading | waits, then returns without loading again |
| its coroutine is cancelled mid-load | state rolls back to what it was before |

Consequences:

- No `isLoading` guard in the ViewModel; call it as often as the list asks.
- The `Result` duplicates the state; use it only for side effects (logging, a snackbar).

## 4. Editing without a reload

```kotlin
paginator.prepend(item)              // top; moves it there if its identity is already loaded
paginator.insertAt(item, index = 3)  // clamped index; same move semantics
paginator.remove(item.id)            // by identity, not the item
```

- Only act in `Success`; before the first page they do nothing.
- `totalCount` rises only for a genuinely new identity, and falls by the number removed.
- An edit made while a page loads survives the page's arrival.
- A removed item can come back if a later page from the backend still contains it — delete on the
  backend first, then `remove`.

## 5. `reset`

`reset()` → `Idle`, page 0, new generation. A load in flight is discarded when it returns (success,
failure or cancellation). It does **not** fetch: call `fetch()` next. See
[states-and-errors.md](states-and-errors.md#pull-to-refresh) for refresh and search.

## 6. Reading `PagingState`

```kotlin
when (state) {
    PagingState.Idle -> …                  // nothing asked yet / reset
    PagingState.Pending -> …               // first page loading
    is PagingState.Error -> …              // first page failed: state.throwable
    is PagingState.Success -> when (state.appendStatus) {
        PagingState.AppendStatus.Idle -> …       // more pages exist
        PagingState.AppendStatus.Loading -> …    // next page loading
        PagingState.AppendStatus.Completed -> …  // all loaded
        is PagingState.AppendStatus.Failed -> …  // next page failed
    }
}
```

Helpers (`io.thernal.pagingkit.paging.api.domain.model`): `itemsOrEmpty()`, `canLoadMore`,
`isLoadingMore`, `hasMore`, `map { }`, `mapItems { }`. `Success` with no items is "loaded, empty".
