# paging/api

The public surface of paging-kit — everything an application or a feature module compiles against:
the loader a backend implements, the state of a paginated collection, the paginator that moves it,
and the two composables that render one.

This module holds contracts only. `paging/impl` implements them and `paging/wiring` installs them
into an application graph, so a feature module depends on `api` and nothing else.

- **Runnable examples** of every section: [`sample/`](../../sample/README.md) — one simple and one
  real-life example per capability, on Android and iOS.
- **Why** each contract has the shape it has: [`paging/README.md`](../README.md).
- **Agents** get the same material as a skill: [`skills/paging-kit`](../../skills/paging-kit/SKILL.md).

Targets: `android`, `iosArm64`, `iosSimulatorArm64`. Built on Compose Multiplatform foundation and
kotlinx.coroutines; versions in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).

## Contents

- [Which tool do I need](#which-tool-do-i-need)
- [The model](#the-model)
- [Installing](#installing)
- [Loading pages](#loading-pages)
- [Paginator](#paginator)
- [PagingState](#pagingstate)
- [PaginationList](#paginationlist)
- [Grouped windows](#grouped-windows)
- [PaginationFlowRow](#paginationflowrow)
- [Placeholders, empty, errors and retry](#placeholders-empty-errors-and-retry)
- [Refresh, search and other resets](#refresh-search-and-other-resets)
- [Scroll state](#scroll-state)
- [Composition locals](#composition-locals)
- [What survives what](#what-survives-what)
- [Testing](#testing)
- [Rules checklist](#rules-checklist)
- [Known limitations](#known-limitations)
- [API index](#api-index)

## Which tool do I need

| I want to… | Use | Section | Sample |
|---|---|---|---|
| load a backend's pages | a `PageLoader<T>` returning `Page<T>` | [Loading pages](#loading-pages) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| hold a paginated collection in a ViewModel | `PaginatorFactory.create(loader, identity)` | [Paginator](#paginator) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| show it as a list that loads as it scrolls | `PaginationList { pagedItems(PagedItemsParams(…)) { … } }` | [PaginationList](#paginationlist) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| two paged collections in one scroll | two `pagedItems` with different `windowId`s | [Windows](#several-windows-in-one-list) | [windows](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/windows/README.md) |
| headers between items — by letter, by date | `pagedItemsGrouped` | [Grouped windows](#grouped-windows) | [windows](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/windows/README.md) |
| chips or cards that wrap | `PaginationFlowRow` | [PaginationFlowRow](#paginationflowrow) | [flowrow](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/flowrow/README.md) |
| a shimmer while loading | the `shimmer` slot, sized by `ShimmerSlot` | [Placeholders](#placeholders-empty-errors-and-retry) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| a retry button when a page fails | `errorContent` (first page), `appendErrorContent` (later pages) | [Placeholders](#placeholders-empty-errors-and-retry) | [errors](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/errors/README.md) |
| show a new item without reloading | `paginator.prepend(item)` / `insertAt` | [Paginator](#editing-without-a-reload) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| drop a deleted item | `paginator.remove(id)` | [Paginator](#editing-without-a-reload) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| pull to refresh | `reset()` then `fetch()`, inside the design system's refresh container | [Resets](#refresh-search-and-other-resets) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| search as you type | a loader that reads the query; `reset()` + `fetch()` per query | [Resets](#refresh-search-and-other-resets) | [errors](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/errors/README.md) |
| scroll to top, or know whether the list is at the top | `rememberPaginationListState()` | [Scroll state](#scroll-state) | [lists](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) |
| map domain items to UI models | `PagingState.map { }` | [PagingState](#pagingstate) | — |

## The model

```
 backend ◀── PageLoader.load(page, size) ── Paginator (private to its ViewModel)
                                               │ observe(): Flow<PagingState<T>>
                                               ▼
                                    ViewModel.state: StateFlow<PagingState<T>>
                                               │
                                               ▼
       PaginationList { pagedItems(PagedItemsParams(state, key, onFetch = vm::onFetch)) { item -> } }
                                               │ an item near the end is composed
                                               ▼
                                   vm.onFetch() ── paginator.fetch() ──▶ next page
```

Five facts carry the rest of this document:

1. **The loader returns a `Page`; it does not know about state.** Throwing is how it fails.
2. **A paginator belongs to one owner.** It is created from the injected `PaginatorFactory`, kept
   private, and never shared or injected itself.
3. **Nothing loads on its own.** The owner calls `fetch()` for page 0 — normally in `init` — and
   again after every `reset()`. The list calls `onFetch` only for later pages.
4. **`fetch` is safe to over-call.** Repeated calls for the same page load it once; calls after the
   last page do nothing. Wire `onFetch` straight to it.
5. **The composables are render contracts.** They draw what `PaginationListRenderer` /
   `PaginationFlowRowRenderer` draw, installed once at the root. Without them they draw nothing —
   except in a preview, where they draw the state they are handed.

## Installing

### Modules

| Module | Who depends on it | What it holds |
|---|---|---|
| `:paging:api` | every feature module, and the app | the contracts in this document; re-exports nothing — see [Dependencies you declare](#dependencies-you-declare) |
| `:paging:impl` | the module that builds the graph | `PaginatorImpl`, the list and flow-row hierarchies and their renderers |
| `:paging:wiring` | the module that declares the [Metro](https://github.com/ZacSweers/metro) graph | `PagingWiring`, the bindings below |
| `:paging:preview` | feature modules that preview paged screens — optional | `PagingPreviewParameterProvider`, see [Previews](#previews) |

The modules are not published to a Maven repository; build against them from source.
[`sample/shared/build.gradle.kts`](../../sample/shared/build.gradle.kts) is the reference consumer:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.paging.api)
            implementation(projects.paging.impl)
            implementation(projects.paging.wiring)          // leave out when wiring by hand
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.lifecycle.viewmodel.compose) // paginators live in ViewModels
        }
    }
}
```

### Dependencies you declare

`api(...)` is not used in this repository: no kit module re-exports anything, so a module that uses a
type from one of these libraries in its own code declares the library itself, as `implementation`.

| Library (catalog entry) | Declare it where the module uses |
|---|---|
| `kotlinx-coroutines-core` | `Flow<PagingState<T>>` from a paginator |
| `kotlinx-collections-immutable` | `ImmutableList<T>` pages and items |
| `compose-ui-tooling-preview` | `PagingPreviewParameterProvider` from `:paging:preview` (`@Preview`, `PreviewParameterProvider`) |

A module that names none of these types needs none of them. A missing one shows up at compile time as
`Cannot access class 'kotlinx.collections.immutable.ImmutableList'`.

### With Metro

`PagingWiring` is a `@BindingContainer` contributed to `AppScope`, so a graph over `AppScope` picks
it up once the module is on the classpath. It binds, each `@SingleIn(AppScope::class)`:

| Binding | For |
|---|---|
| `PaginatorFactory` | ViewModels and repositories, injected |
| `PaginationListRenderer` | contributed as `LocalPaginationListRenderer provides …` into `Set<ProvidedValue<*>>` |
| `PaginationFlowRowRenderer` | contributed as `LocalPaginationFlowRowRenderer provides …` into `Set<ProvidedValue<*>>` |

`Paginator` is deliberately not bound.

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val providedValues: Set<ProvidedValue<*>>
}
```

If nothing else in the application contributes a `ProvidedValue`, the set still resolves: the two
renderers are in it.

### Without a DI framework

`api` and `impl` name no container. Build the three objects once per process and install the two
locals yourself:

```kotlin
object Paging {
    val paginatorFactory: PaginatorFactory = PaginatorFactoryImpl()
    val providedValues: Array<ProvidedValue<*>> = arrayOf(
        LocalPaginationListRenderer provides PaginationListRendererImpl(),
        LocalPaginationFlowRowRenderer provides PaginationFlowRowRendererImpl(),
    )
}
```

### The composition root

```kotlin
@Composable
fun AppRoot(graph: AppGraph) {
    CompositionLocalProvider(values = graph.providedValues.toTypedArray()) {
        AppTheme { AppContent() }
    }
}
```

Create the graph once per process — an Android `Application`, a process-wide `lazy` on iOS — and
hand it to the composition. The renderers are stateless, so this matters less for paging than for
the rest of an application graph, but a graph remembered by the composition is rebuilt on every
rotation.

### Previews

Nothing to install. In a preview (`LocalInspectionMode`), the default renderers draw whatever state
they are handed — once, with no fetching, keys or animation — so a feature module previews its list
without depending on `impl`:

```kotlin
@Preview
@Composable
private fun FeedLoadedPreview() {
    FeedContent(
        state = PagingState.Success(
            items = persistentListOf(Post(1, "Aysel", "Hello"), Post(2, "Murad", "Hi")),
            appendStatus = PagingState.AppendStatus.Loading,   // shows the footer shimmer too
        ),
        onFetch = {},
    )
}

@Preview
@Composable
private fun FeedLoadingPreview() {
    FeedContent(state = PagingState.Pending, onFetch = {})   // shimmerItemCount shimmers
}
```

Each state previews its slot: `Pending` the shimmers, `Error` `errorContent`, `Idle` or an empty
`Success` `emptyContent`, `AppendStatus.Failed` `appendErrorContent` (its retry does nothing). Preview
the screen's stateless content composable, the one that takes `state` rather than a ViewModel.

**Every state at once** — `:paging:preview` has a `PreviewParameterProvider` that yields one
`PagingState` per `PagingPreviewState` (`LOADING`, `LOADED`, `LOADING_MORE`, `LOAD_MORE_FAILED`,
`END_REACHED`, `ERROR`, `EMPTY`), each preview named after its state. A preview parameter class needs
a no-argument constructor, so subclass it once per item type with sample items:

```kotlin
private class FeedStates : PagingPreviewParameterProvider<Post>(items = listOf(post1, post2))

@Preview(heightDp = 640)
@Composable
private fun FeedPreview(@PreviewParameter(FeedStates::class) state: PagingState<Post>) {
    FeedContent(state = state, onFetch = {})
}
```

`states = listOf(PagingPreviewState.LOADING, PagingPreviewState.ERROR)` narrows the set; `error =`
sets the throwable the error states carry (default `PreviewPagingException("Preview error")`). The
module depends on `ui-tooling-preview`, which is why it is separate from `api`. The sample's
[`NumbersScreen.kt`](../../sample/shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/NumbersScreen.kt)
uses it.

Outside a preview the default renderers draw nothing, so a missing installation shows up as a blank
list rather than as a list that never loads a second page.

## Loading pages

```kotlin
class PostPageLoader(private val api: PostApi) : PageLoader<Post> {
    override suspend fun load(page: Int, size: Int): Page<Post> {
        val response = api.posts(page = page, size = size)
        return Page(
            items = response.content.map { it.toDomain() }.toImmutableList(),
            totalCount = response.totalElements,
            isLastPage = response.last,
        )
    }
}
```

- `page` is zero-based and counts pages loaded since the paginator was created or reset. A failed
  page is retried with the same number.
- `size` is whatever the caller passed to `fetch(size)`; `Paginator.DEFAULT_PAGE_SIZE` (20) if none.
- **Throw to fail.** Every exception except `CancellationException` becomes an error state and a
  `Result.failure`; the loader never catches its own transport errors.
- `isLastPage = true` stops the paginator. A backend that does not say so can answer
  `items.size < size`.
- `totalCount` is passed through to `PagingState.Success.totalCount`; `0` when the backend has none.
- A cursor- or keyset-paginated backend keeps its cursor in the loader: store the next cursor from
  each response, clear it when `page == 0`. See [Known limitations](#known-limitations).

## Paginator

```kotlin
class PostFeedViewModel(
    paginatorFactory: PaginatorFactory,
    postApi: PostApi,
) : ViewModel() {
    private val paginator: Paginator<Post> = paginatorFactory.create(
        loader = PostPageLoader(postApi),
        identity = { post -> post.id },
    )

    val state: StateFlow<PagingState<Post>> = paginator.observe()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = PagingState.Idle)

    init {
        onFetch()
    }

    fun onFetch() {
        viewModelScope.launch { paginator.fetch() }
    }
}
```

- **`identity`** says what makes two items the same item. Pages are de-duplicated by it (first
  occurrence wins), and `insertAt`/`prepend`/`remove` find items by it. Use the entity id, never the
  whole object.
- **`observe()`** is hot: a collector gets the current state at once. `stateIn` with `Eagerly` gives
  a `StateFlow` a composable can `collectAsState()` without a first frame of `Idle` after rotation.
- **Call it from one thread** — the ViewModel's main dispatcher. `fetch` suspends on the loader; the
  loader switches to IO itself if it needs to.

### `fetch`

`suspend fun fetch(size: Int = 20): Result<Boolean>` — loads the next page, or retries the failed one,
and answers whether more pages remain.

| State when called | What happens |
|---|---|
| `Idle`, `Error` | `Pending`, then page 0 (or the failed page) |
| `Success`, `AppendStatus.Idle` or `Failed` | `AppendStatus.Loading`, then the next page |
| `Success`, `AppendStatus.Completed` | returns `false`; the loader is not called |
| another `fetch` for the same page is loading | waits for it, then returns without loading again |

The failure is reported twice — in the state, for the UI, and as `Result.failure`, for a caller that
wants to log it or show a snackbar. Ignoring the `Result` is normal.

### Editing without a reload

```kotlin
fun onPostCreated(post: Post) = paginator.prepend(post)
fun onPostPinned(post: Post) = paginator.insertAt(post, index = 0)
fun onPostDeleted(id: String) = paginator.remove(id)
```

- Each edits the loaded list in place; nothing is requested. Outside `Success` they do nothing.
- `prepend`/`insertAt` **move** an item whose identity is already present, rather than duplicating it,
  and adjust `totalCount` only for a genuinely new item. `index` is clamped.
- `remove` takes the identity (`post.id`), not the item.
- An edit made while a page is loading is kept when the page arrives.

### `reset`

Back to `Idle`, page 0, and a new generation: a load in flight is discarded when it returns, whether
it succeeds or fails. **`reset` does not fetch** — call `fetch()` when you want page 0 again. See
[Refresh, search and other resets](#refresh-search-and-other-resets).

## PagingState

```kotlin
sealed interface PagingState<out T> {
    data object Idle
    data object Pending
    data class Success<T>(val items: ImmutableList<T>, val totalCount: Int = 0, val appendStatus: AppendStatus = Idle)
    data class Error(val throwable: Throwable)

    sealed interface AppendStatus { Idle; Loading; Completed; Failed(throwable) }
}
```

| State | Means | The list shows |
|---|---|---|
| `Idle` | nothing asked yet, or reset since | `emptyContent` |
| `Pending` | the first page is loading | `shimmer` × `shimmerItemCount` |
| `Error` | the first page failed | `errorContent`, else `emptyContent` |
| `Success`, items, `Idle` | more pages exist | items |
| `Success`, items, `Loading` | the next page is loading | items + `shimmer(ShimmerSlot.Append)` |
| `Success`, items, `Failed` | the next page failed | items + `appendErrorContent(error, retry)` |
| `Success`, items, `Completed` | everything is loaded | items |
| `Success`, no items | loaded, and empty | `emptyContent` |

Extensions in `domain.model`:

| Extension | Answers |
|---|---|
| `itemsOrEmpty()` | the items, or an empty list outside `Success` |
| `canLoadMore` | `Success` with `AppendStatus.Idle` |
| `isLoadingMore` | `Success` with `AppendStatus.Loading` |
| `hasMore` | `Success` and not `Completed` |
| `map { }` | the same state over transformed items — domain → UI models |
| `mapItems { }` | the same state over a rewritten list — a filter, a local edit on a copy |

`map`/`mapItems` change a copy the caller holds; the paginator's own state is unaffected. To change
what the paginator holds, use its edit commands.

## PaginationList

```kotlin
@Composable
fun PostFeed(model: PostFeedViewModel) {
    val state by model.state.collectAsState()
    PaginationList {
        pagedItems(
            params = PagedItemsParams(
                state = state,
                key = { post -> post.id },
                onFetch = model::onFetch,
                shimmer = { PostShimmer() },
            ),
        ) { post ->
            PostRow(post)
        }
    }
}
```

`PaginationList(params = PaginationListParams(), modifier = Modifier, content)` is a `LazyColumn`
that fills the space it is given. `PaginationListParams` holds `listState` and `contentPadding`.

`PagedItemsParams<T>`:

| Field | Default | |
|---|---|---|
| `state` | — | the window's `PagingState` |
| `key` | — | stable identity per item; its `toString()` must be unique in the window |
| `onFetch` | — | called for later pages; wire to `Paginator.fetch` |
| `contentType` | `{ null }` | as `LazyColumn`'s |
| `fetchThreshold` | `3` | how many items before the end the next page is asked for |
| `windowId` | `"paged"` | unique per window in one list |
| `shimmerItemCount` | `15` | placeholders while the first page loads |
| `shimmer` | `null` | `@Composable (ShimmerSlot) -> Unit` |
| `separator` | `null` | `@Composable (before: T, after: T) -> Unit`, between adjacent items |
| `emptyContent` | `null` | `ColumnScope` slot filling the viewport |
| `errorContent` | `null` | `ColumnScope` slot filling the viewport; falls back to `emptyContent` |
| `appendErrorContent` | `null` | `@Composable (Throwable, retry: () -> Unit) -> Unit` |

Items are animated with `animateItem()` — inserts, moves and removals from the paginator's edit
commands animate without extra work.

### Several windows in one list

```kotlin
PaginationList {
    item(key = "pinned_header") { SectionHeader("Pinned") }
    pagedItems(params = PagedItemsParams(state = pinned, key = { it.id }, onFetch = vm::fetchPinned, windowId = "pinned")) { PinnedRow(it) }
    ifLoaded(pinned) {
        item(key = "all_header") { SectionHeader("All") }
    }
    pagedItems(params = PagedItemsParams(state = all, key = { it.id }, onFetch = vm::fetchAll, windowId = "all")) { ArticleRow(it) }
}
```

- `PaginationListScope` extends `LazyListScope`: `item`, `items`, `stickyHeader` all work between
  windows. Give plain items explicit keys that cannot collide with `"<windowId>_…"`.
- **`windowId` must be unique** in one list — the second window with the same id throws
  `IllegalArgumentException: Duplicate pagination windowId: …`.
- Keys are namespaced per window, so the same entity may appear in two windows.
- `ifLoaded(state) { items -> … }` adds content only once `state` is `Success` with items.
- A lower window's fetch trigger is only composed once the user scrolls to it, so a long upper
  window keeps a lower one at page 0 — usually what you want. Each window has its own paginator.

## Grouped windows

```kotlin
pagedItemsGrouped(
    params = PagedItemsGroupedParams(
        state = state,
        key = { contact -> contact.id },
        onFetch = model::onFetch,
        groupBy = { contact -> contact.name.first().uppercaseChar() },
        groupKey = { letter -> letter },
        groupHeader = { letter -> LetterHeader(letter) },
        hasStickyHeaders = true,
    ),
) { contact ->
    ContactRow(contact)
}
```

- `groupBy` maps an item to its group; `groupKey` maps a group to a stable key; `groupHeader` draws it.
- Groups appear in order of first appearance over **all** loaded items, and a group that continues on
  a later page stays under its existing header. Sort on the backend by the grouping field.
- `hasStickyHeaders` pins the current header while its items scroll.
- There is no `separator` for grouped windows; draw dividers inside `itemContent`.

## PaginationFlowRow

```kotlin
PaginationFlowRow(
    params = PaginationFlowRowParams(
        state = state,
        key = { tag -> tag.id },
        onFetch = model::onFetch,
        header = { SectionHeader("Tags") },
        shimmerContent = { index -> ChipShimmer(index) },
        emptyContent = { NoTags() },
    ),
) { tag ->
    TagChip(tag)
}
```

A vertically scrolling `FlowRow` that crossfades between shimmer, content, error and empty.

- **It fetches by scroll distance**, not by item position: a `FlowRow` composes every child, so the
  next page is asked for when the remaining scroll is within `prefetchDistance` (240.dp). Content
  shorter than the viewport keeps fetching until it fills the viewport or the last page arrives.
- **It scrolls itself** — do not put it inside another vertical scroll.
- `header` scrolls with the items, above them, in every state.
- `shimmerContent(index)` is drawn `shimmerItemCount` times while the first page loads, and
  `shimmerContent(null)` once as a footer while a later page loads.
- `separator` is drawn between adjacent items inside the flow; use item padding for spacing.
- Not lazy: every loaded item stays composed. For thousands of items, use a `LazyVerticalGrid` or a
  `PaginationList` of rows instead.

## Placeholders, empty, errors and retry

The kit draws nothing of its own. Every placeholder is a slot, and a `null` slot draws nothing.

```kotlin
PagedItemsParams(
    state = state,
    key = { it.id },
    onFetch = model::onFetch,
    shimmer = { slot ->
        when (slot) {
            is ShimmerSlot.Initial -> PostShimmer()            // × shimmerItemCount, first page
            ShimmerSlot.Append -> SmallSpinner()               // × 1, footer, later pages
        }
    },
    emptyContent = { EmptyFeed() },
    errorContent = { FeedError(onRetry = model::onFetch) },   // first page failed
    appendErrorContent = { error, retry -> RetryFooter(error, onRetry = retry) }, // later page failed
)
```

- `emptyContent`/`errorContent` receive a `ColumnScope` filling the list's viewport — center in it.
- `errorContent` has no retry parameter: call the ViewModel's fetch. `appendErrorContent` is handed
  `retry`, which is `onFetch`.
- **Without `appendErrorContent`, a failed later page shows no footer and nothing retries.** The
  fetch trigger only fires while `appendStatus` is `Idle`. Always supply it when the network can
  fail.

## Refresh, search and other resets

A reset returns to `Idle`; the fetch after it brings page 0 back.

**Pull to refresh** — the kit ships no refresh container. Wrap the list in the design system's:

```kotlin
fun onRefresh() {
    viewModelScope.launch {
        isRefreshing.value = true
        paginator.reset()
        paginator.fetch()
        isRefreshing.value = false
    }
}

PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = model::onRefresh) {
    PaginationList { … }
}
```

Between `reset` and the first page the state is `Idle` then `Pending`, so the list shows its shimmer
under the refresh indicator. To keep the old items visible while refreshing, keep the last `Success`
in the ViewModel and show it until the new one arrives.

**Search as you type** — one paginator for the screen, a loader that reads the query it was last
reset for:

```kotlin
private val query = MutableStateFlow("")      // the text field
private var activeQuery = ""                  // what the pages on screen were loaded for
private val paginator = paginatorFactory.create(
    loader = PageLoader { page, size -> api.search(activeQuery, page, size) },
    identity = { it.id },
)

init {
    viewModelScope.launch {
        query.debounce(300.milliseconds).collect { next ->
            activeQuery = next
            paginator.reset()
            paginator.fetch()
        }
    }
}
```

A slow page for an older query is discarded when it returns, because the reset moved the generation
on. The loader reads `activeQuery` rather than `query.value`: the field runs ahead of the debounce,
and a later page the list asks for in between must still belong to the results on screen. Do not
create a paginator per query: the old one's loads keep running and its state is no longer observed.

## Scroll state

```kotlin
val listState = rememberPaginationListState()
val isAtTop by listState.isAtTop
val scope = rememberCoroutineScope()

PaginationList(params = PaginationListParams(listState = listState)) { … }

if (!isAtTop) {
    FloatingActionButton(onClick = { scope.launch { listState.scrollToTop() } }) { … }
}
```

`PaginationListState` wraps a `LazyListState` (`lazyListState`) and is a `ScrollableState`. Pass
`rememberPaginationListState(rememberLazyListState(initialFirstVisibleItemIndex = …))` to start
elsewhere. Without a `listState`, the list remembers its own — enough unless something above the
list reads or drives the scroll.

## Composition locals

| Local | Provided by | Without a provider |
|---|---|---|
| `LocalPaginationListRenderer` | the root, from the graph | `PaginationList` draws nothing; in a preview, the handed state, statically |
| `LocalPaginationFlowRowRenderer` | the root, from the graph | `PaginationFlowRow` draws nothing; in a preview, the handed state, statically |

A list that is blank in a running app — no shimmer, no items, no empty state — has no renderer.

## What survives what

| | Recomposition | Activity recreated (Android) | Process death |
|---|---|---|---|
| Paginator in a ViewModel | kept | kept | lost: page 0 again |
| `PaginationListState` (remembered) | kept | kept (`rememberLazyListState` is saveable) | kept, but the items it pointed at are gone |

A paginator's state is in memory only. After process death the screen starts from page 0; a list
restored to a deep scroll position over an empty collection jumps back to the top.

## Testing

Everything except the composables is plain Kotlin and runs in `commonTest`.

```kotlin
@Test
fun loadsUntilTheLastPage() = runTest {
    val paginator = PaginatorImpl(
        loader = PageLoader { page, _ ->
            Page(items = persistentListOf(page), isLastPage = page == 1)
        },
        identity = { it },
    )

    assertTrue(paginator.fetch().getOrThrow())
    assertFalse(paginator.fetch().getOrThrow())
    assertEquals(listOf(0, 1), paginator.observe().first().itemsOrEmpty())
}
```

| Subject | Build it with |
|---|---|
| a ViewModel | `PaginatorFactoryImpl()` as its factory, and a fake `PageLoader` |
| the paginator's rules | `PaginatorImpl(loader, identity)` |
| an in-flight load | a loader that awaits a `CompletableDeferred`; `launch { fetch() }`, `runCurrent()`, act, then complete it |
| a failure | a loader that throws; assert `Result.isFailure` and the state |

`paging/impl/src/commonTest` holds the kit's own tests in exactly this shape.

## Rules checklist

- [ ] The root installs the graph's provided values once; the graph is created once per process.
- [ ] `PaginatorFactory` is injected; every paginator is created by its owner and kept private.
- [ ] `identity` is the entity id; `key` in the params is the same id.
- [ ] The owner fetches page 0 itself — in `init`, and after every `reset`.
- [ ] `onFetch` is wired straight to `Paginator.fetch`, without guards of its own.
- [ ] Every window in one list has a distinct `windowId`.
- [ ] `appendErrorContent` is supplied wherever loads can fail.
- [ ] A new query or a refresh is `reset()` + `fetch()` on the same paginator.
- [ ] `PaginationFlowRow` is not nested in a vertical scroll.
- [ ] Nothing that must survive process death lives only in a paginator.

## Known limitations

- **Page numbers only.** `PageLoader` receives a page index. Cursor and keyset backends keep their
  cursor inside the loader — workable, but not first-class. See
  [`docs/todos/open-questions.md`](../../docs/todos/open-questions.md).
- **Append only.** There is no loading backwards (`prepend` a page) and no dropping of pages that
  have scrolled far away; memory grows with what the user has loaded.
- **In memory only.** No disk cache, no restoration after process death.
- **No refresh container and no default placeholders** — both are the caller's design system.
- **`PaginationFlowRow` is not lazy.**
- **Not published** to a Maven repository.

## API index

`api` — package prefix `io.thernal.pagingkit.paging.api`.

| Package | Declarations |
|---|---|
| `domain.loader` | `PageLoader` |
| `domain.model` | `Page`, `PagingState`, `PagingState.AppendStatus`, `itemsOrEmpty`, `canLoadMore`, `isLoadingMore`, `hasMore`, `map`, `mapItems` |
| `domain.paginator` | `Paginator`, `PaginatorFactory` |
| `presentation.components` | `PaginationList`, `PaginationListRenderer`, `LocalPaginationListRenderer`, `PaginationListScope`, `PaginationFlowRow`, `PaginationFlowRowRenderer`, `LocalPaginationFlowRowRenderer` |
| `presentation.model` | `PaginationListParams`, `PaginationListState`, `rememberPaginationListState`, `PagedItemsParams`, `PagedItemsGroupedParams`, `PaginationFlowRowParams`, `ShimmerSlot` |

`impl` declarations an application reaches for — package prefix `io.thernal.pagingkit.paging.impl`:

| Package | Declarations |
|---|---|
| `domain.paginator` | `PaginatorImpl`, `PaginatorFactoryImpl` |
| `presentation.components` | `PaginationListRendererImpl`, `PaginationFlowRowRendererImpl` |

`wiring` — `io.thernal.pagingkit.paging.wiring.PagingWiring`.

`preview` — `io.thernal.pagingkit.paging.preview`: `PagingPreviewParameterProvider`,
`PagingPreviewState`, `PreviewPagingException`.
