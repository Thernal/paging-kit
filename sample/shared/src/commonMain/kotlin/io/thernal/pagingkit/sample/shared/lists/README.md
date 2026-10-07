# Lists — one paged window

Catalog group **Lists**: [Numbers](#simple-numbers) (simple) and [Post feed](#real-life-post-feed)
(real life).

A collection too long to load at once, shown in a list that asks for more as the user scrolls. The
paginator owns the pages and the state; the list only renders the state and says when it wants more.

| File | What is in it |
|---|---|
| [`NumbersViewModel.kt`](NumbersViewModel.kt) | a factory in, a private paginator, its state out, one fetch command |
| [`NumbersScreen.kt`](NumbersScreen.kt) | `PaginationList` + `pagedItems` + a shimmer, and a `@Preview` of every state via `PagingPreviewParameterProvider` |
| [`Post.kt`](Post.kt) | the feed's item and its seed data |
| [`FeedViewModel.kt`](FeedViewModel.kt) | refresh, local prepend and remove, a flaky network |
| [`FeedScreen.kt`](FeedScreen.kt) | pull to refresh, empty/error/retry slots, separators, scroll to top |
| [`ListsBindings.kt`](ListsBindings.kt) | the factory from the graph, handed to each screen |

## Simple: Numbers

The ViewModel is the whole contract:

```kotlin
class NumbersViewModel(paginatorFactory: PaginatorFactory) : ViewModel() {
    private val server = FakeServer { (1..200).toList() }

    private val paginator: Paginator<Int> = paginatorFactory.create(
        loader = PageLoader { page, size -> server.load(page = page, size = size) },
        identity = { number -> number },
    )

    val state: StateFlow<PagingState<Int>> = paginator.observe().stateIn(
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

The screen hands the state and the command to one window:

```kotlin
PaginationList {
    pagedItems(
        params = PagedItemsParams(
            state = state,
            key = { number -> number },
            onFetch = model::onFetch,
            shimmer = { ShimmerRow() },
        ),
    ) { number ->
        ItemRow(title = "Number $number", subtitle = "page ${(number - 1) / 20 + 1}")
    }
}
```

What to notice:

- **The paginator is created, not injected.** The graph binds `PaginatorFactory`; the ViewModel builds
  its own paginator and keeps it private. Two screens never share one.
- **`init` fetches page 0.** Nothing loads on its own — an `Idle` list is an empty list. The list's
  `onFetch` only asks for *later* pages.
- **`onFetch` is not guarded.** Three items near the end are composed in the same frame and all three
  call it; the paginator loads the page once. There is no `isLoading` flag to keep in the ViewModel.
- **The shimmer is one lambda.** While page 0 loads it is drawn 15 times (`shimmerItemCount`); while a
  later page loads, once, as a footer.

**Try it:** open *Numbers* and scroll steadily. A shimmer row appears at the end three items before
you reach it, and the next twenty numbers replace it.

## Real life: Post feed

The same window, with everything a production feed adds.

**Pull to refresh.** The kit has no refresh container; the design system's wraps the list:

```kotlin
PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = model::onRefresh) {
    PaginationList(params = PaginationListParams(listState = listState)) { … }
}
```

```kotlin
fun onRefresh() {
    viewModelScope.launch {
        mutableIsRefreshing.value = true
        paginator.reset()
        paginator.fetch()
        mutableIsRefreshing.value = false
    }
}
```

**Edits without a reload.** A post the user wrote goes to the top; a deleted one disappears. Neither
touches the network:

```kotlin
fun onWritePost() {
    val id = nextLocalId--
    paginator.prepend(Post(id = id, author = "You", text = "A post you just wrote (local id $id)."))
}

fun onDelete(post: Post) {
    paginator.remove(post.id)
}
```

**Every state has a slot:**

```kotlin
PagedItemsParams(
    state = state,
    key = { post -> post.id },
    onFetch = model::onFetch,
    shimmer = { ShimmerRow() },
    separator = { _, _ -> HorizontalDivider() },
    emptyContent = { FullMessage(title = "No posts", body = "Nothing here yet.") },
    errorContent = { FullMessage(title = "Couldn't load the feed", …, action = "Try again", onAction = model::onFetch) },
    appendErrorContent = { error, retry -> RetryFooter(message = error.message ?: "…", onRetry = retry) },
)
```

**A header that waits for data**, and a count from the backend's total:

```kotlin
ifLoaded(state) { posts ->
    item(key = "feed_count") { SectionHeader(text = "${posts.size} of $total posts") }
}
```

What to notice:

- **`reset` does not fetch.** The refresh calls both, because only the ViewModel knows that it wants
  page 0 *now*.
- **Edits made while a page loads survive it.** Write a post while the footer shimmer is showing: the
  page arrives and your post is still at the top.
- **`prepend` moves rather than duplicates** an item whose id is already loaded; `remove` takes the id.
- **The two failures look different.** Turn on *Flaky network* and refresh until the first page fails:
  the whole list is the error. Scroll until a later page fails: the posts stay and only the footer
  offers a retry.
- **Without `appendErrorContent` a failed page would be a dead end** — the list asks for more only
  while the append status is `Idle`.
- **Items animate.** `pagedItems` applies `animateItem()`, so prepend and delete animate for free.
- **Scroll to top** reads `listState.isAtTop` — a derived state that changes only when the answer does.

**Try it:** open *Post feed*, write two posts, delete one, pull to refresh (your posts are gone — they
were local), switch on *Flaky network* and scroll until the retry footer appears; tap *Retry*.

## Doing this in your app

1. Implement `PageLoader<T>` over your API, mapping its envelope onto `Page(items, totalCount, isLastPage)`.
2. Inject `PaginatorFactory` into the ViewModel; `create(loader, identity = { it.id })`; keep it private.
3. Expose `paginator.observe().stateIn(viewModelScope, Eagerly, Idle)`; fetch in `init`.
4. In the screen: `PaginationList { pagedItems(PagedItemsParams(state, key, onFetch = vm::onFetch, …)) { row } }`.
5. Fill the slots your states need — at least `shimmer` and `appendErrorContent`.
6. Local creates and deletes: `prepend`/`insertAt`/`remove`. Refresh: `reset()` then `fetch()`.

## Pitfalls

- **Forgetting the first fetch** — the list stays empty (it renders `emptyContent`, or nothing).
- **An `isLoading` guard in front of `fetch`** — unnecessary, and a guard that is never cleared after a
  failure stops the list for good.
- **`key` different from `identity`** — an edit moves the item in the paginator but the list animates a
  different one.
- **Refreshing with a new paginator** — the old one's load keeps running and its state is orphaned;
  reset the one you have.

## Read more

- [Paginator](../../../../../../../../../../paging/api/README.md#paginator) in the API guide
- [PaginationList](../../../../../../../../../../paging/api/README.md#paginationlist)
- [Placeholders, empty, errors and retry](../../../../../../../../../../paging/api/README.md#placeholders-empty-errors-and-retry)
- [All examples](../../../../../../../../../README.md#the-examples)
