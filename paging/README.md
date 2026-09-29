# paging

Public contracts for paginated data (`domain`) and paginated Compose lists (`presentation`): a
page loader, the state of one paginated collection, the paginator that moves it, and two render
contracts — a multi-window `LazyColumn` and a wrapping `FlowRow`. `impl` owns every concrete
behavior; `api` never depends on it.

**Using the kit?** [`api/README.md`](api/README.md) is the task-oriented guide to every public
contract, and [`../sample/README.md`](../sample/README.md) runs each one. This file records the
design — why each contract has the shape it has.

## Module layout

- `api` — `PageLoader`, `Page`, `PagingState`, `Paginator`, `PaginatorFactory`; the
  `PaginationList` and `PaginationFlowRow` render contracts, their params, `PaginationListScope`,
  `PaginationListState` and `ShimmerSlot`.
- `impl` — `PaginatorImpl` and its factory; the `LazyColumn` and `FlowRow` hierarchies behind the
  two render contracts (internal), and their renderer classes.
- `wiring` — binds `PaginatorFactory` and both renderers into an application graph, and
  contributes each renderer's composition local as a `ProvidedValue`.
- `preview` — `PagingPreviewParameterProvider`: every `PagingState` of a list as preview parameters.
  Separate from `api` because it depends on `ui-tooling-preview`, which `api` should not force on
  every consumer.

An `impl` declaration that would collide with an `api` name carries the `Impl` suffix, composables
included: `PaginationListImpl` is the internal render of `api`'s `PaginationList`, the same way
`PaginatorImpl` implements `Paginator`.

## Domain — pure Kotlin

Nothing under `domain` imports Compose. A repository or a use case can hold a `Paginator` and
expose a `PagingState` without its module applying the Compose plugin — `api` as a whole does
apply it, for the presentation half, but the domain half does not lean on it.

### `Page` — the loader's answer

`PageLoader.load(page, size)` returns a `Page<T>`: the items, the backend's total, and whether this
was the last page. Those three fields are all the paginator reads. A backend's own envelope —
`number`, `totalPages`, cursors, links — is mapped onto a `Page` at the edge, in the loader, so the
kit never has to know one backend's pagination format.

### `PagingState` — two ways to fail

```
Idle ──fetch──▶ Pending ──ok──▶ Success(items, total, appendStatus = Idle | Completed)
                   │                         │fetch
                   └──fail──▶ Error          ▼
                                  Success(appendStatus = Loading) ──ok──▶ Success(items + page)
                                                    └──fail──▶ Success(appendStatus = Failed)
```

The first page and every later page fail differently on purpose. Before anything has loaded there
is nothing to keep, so the whole collection is `Error` and a screen shows an error in place of the
list. After that, the items already loaded stay on screen and only the footer changes:
`AppendStatus.Failed`. A single `Error` state for both would force every screen to choose between
wiping a list the user was reading and ignoring a failure.

`Success(items = [])` with `AppendStatus.Completed` is "loaded, and empty" — distinct from `Idle`,
which is "nothing asked yet". The list renders `emptyContent` for both, because to a user both look
the same; code that needs the difference reads the state.

### `Paginator` — stateful, private, safe to over-call

A paginator holds a page number, a generation counter and its own `StateFlow`, and is generic per
use. That is why it is never injected: two screens sharing one would page each other's lists, and
a singleton `Paginator<T>` cannot exist for every `T` anyway. The graph binds `PaginatorFactory`,
which is stateless, and each owner — normally a ViewModel — creates its own paginator and keeps it
private.

**`fetch` is idempotent per page.** A list asks for the next page from every item within
`fetchThreshold` of its end, and three such items are composed in the same frame. `fetch` reads the
page number and generation before queueing on its mutex; a call that finds either changed once it
gets the lock returns without loading. So three calls load one page, not three — the list does not
have to coordinate, and neither does the ViewModel.

**Mutations survive a load in flight.** `prepend`, `insertAt` and `remove` edit the loaded list
without a request — for a post the user just created, or one they deleted. The loaded page is merged
into the state as it is *when the page arrives*, not as it was when the request left, so an edit made
while a page was loading is kept.

**A reset discards what it interrupted.** `reset()` bumps the generation and returns to `Idle`. A
load that started in the previous generation is dropped when it returns — success, failure or
cancellation alike — so a slow answer to an old query cannot land under a new one. `reset` does not
fetch: the owner calls `fetch` when it wants page 0 again, because only the owner knows whether that
is now (pull-to-refresh) or after something else (a debounced query).

**A cancelled load leaves no spinner behind.** If the coroutine running `fetch` is cancelled while
the loader is suspended, the state rolls back from `Pending`/`Loading` to what it was before, so the
next `fetch` is not mistaken for one already running.

**Identity de-duplicates.** Offset pagination over a list that changes between requests returns an
item twice when something is inserted ahead of it. Items are de-duplicated by the `identity` passed
to the factory, first occurrence wins.

## Presentation — render contracts

Both components follow the same three-part pattern, and nothing about it is paging-specific:

- `api` owns the contract (`PaginationListRenderer`), its `CompositionLocal`, and a composable facade
  (`PaginationList`) so a caller calls a function instead of reading the local.
- `impl` owns the composable hierarchy and keeps it `internal`.
- `wiring` installs the implementation as a `ProvidedValue`, so an application never names `impl`
  in UI code.

The local's default renderer depends on where it runs. **In a preview** (`LocalInspectionMode`) it
draws the state it is handed — items with their separators, the initial shimmer, the append footer,
the empty and error slots — once, with no fetching, keys or animation. A feature module's `@Preview`
therefore shows a real-looking list from a hand-made `PagingState`, without depending on `impl`.
**Anywhere else** it draws nothing. A default that also rendered in the app would show page 0 and
silently never load page 1 when the installation is missing; a blank list is the louder failure, and
the first thing to check when a list is blank in a running app.

The preview drawing lives in `api` (`PreviewPaginationList`, `PreviewPaginationFlowRow`, both
internal) and repeats `impl`'s state-to-slot mapping without its behavior. The two are small and
change together: a new slot or state is added to both.

### Params, then modifier, then the one required slot

Each facade takes `params: XParams`, `modifier: Modifier = Modifier`, and — only when the component
has exactly one composable slot every caller must supply — a trailing lambda for it. Everything else
goes on `XParams`: the state, the callbacks, and every *optional* slot (shimmer, empty, error, append
error, separator). A caller opens `params.` in autocomplete for what it can tune and gets ordinary
trailing-lambda syntax for the thing it always writes — the item — the same shape as `LazyColumn` or
`Card`. A component grows by adding fields to its params, never parameters to its facade.

### `PaginationList` — windows in one `LazyColumn`

A screen often shows more than one paged collection in one scroll: pinned and all, following and
discover, a header and a feed. Each is a *window*: `pagedItems(params) { item -> … }` adds one to the
list, with its own placeholders, its own footer and its own fetch trigger. `PaginationListScope`
extends `LazyListScope`, so plain `item`/`items` sit between windows, and `ifLoaded(state)` adds
something only once a window has items — a section header that should not float above a shimmer.

A window's items are keyed `"<windowId>_item_<key>"`, so the same entity in two windows does not
collide in the lazy list, and a key stays a `String` — the type an Android `Bundle` can save.
`windowId` must be unique per list; a duplicate fails fast with `Duplicate pagination windowId`.

`pagedItemsGrouped` is a window whose items sit under headers. Rows are grouped in order of first
appearance across *all* loaded pages, so a group that continues on the next page stays under the
header already shown — the common case for any list sorted by the grouping field.

### `PaginationFlowRow` — prefetch by distance

A `FlowRow` is not lazy: it composes every child. "An item near the end was composed" — the lazy
list's trigger — is therefore true for every page the moment it arrives, and a flow row triggered
that way loads the whole collection unprompted. The flow row triggers on scroll instead: when the
remaining scroll is within `prefetchDistance`, it fetches. Content shorter than the viewport keeps
fetching until it fills it or the last page arrives.

It crossfades between shimmer, content, error and empty, and scrolls itself, so it is not nested in
another vertical scroll.

### What the kit does not draw

The kit names no design system. There is no default shimmer, empty state, error state or retry
footer: each is a slot, and a `null` slot draws nothing. The one consequence worth stating is that a
list without `appendErrorContent` shows no footer when a later page fails — and since the fetch
trigger only fires while `appendStatus` is `Idle`, nothing retries until the owner fetches again.
Pull-to-refresh is the same: the kit ships no refresh container, and wrapping a `PaginationList` in
the design system's own (Material 3's `PullToRefreshBox` in the sample) is all it takes.

### Slot lambdas are not memoized

A `PaginationList`'s content is a `LazyListScope` builder, not a composable, and the flow row invokes
its slots from inside a `FlowRow` body. The Compose compiler's automatic lambda memoization does not
reach either place. If a slot captures state that changes often and its recomposition cost matters,
`remember` it at the call site — the component cannot do that on the caller's behalf.

## What changed in the port

The source module was `core/pagination` in an Android-only application. Most differences are that
fact — multiplatform, no application around it — and the rest are defects found while porting,
each covered by a test in `impl/src/commonTest` where it is testable without a UI.

| | Android original | Here | Why |
|---|---|---|---|
| Loader result | `PagedEntity<T>` from the app's `core/domain`, seven fields marked "update based on backend" | `Page<T>(items, totalCount, isLastPage)` in `api` | the paginator reads three fields; a kit cannot depend on one app's backend envelope. |
| Renderer marker | contracts extended the app's `ComponentRenderer` | plain interfaces | the marker carried no members; depending on it would have pulled in the app's `core/presentation`. |
| State helpers | `canLoadMore`, `hasMore`, `map`, … in `impl`, so no feature could call them | the queries and `map`/`mapItems` in `api`; the transition helpers dropped | the transitions duplicated `PaginatorImpl`'s own logic and were called nowhere. |
| Concurrent `fetch` | queued on the mutex, and each queued call loaded the *next* page | a call that finds the page already loaded returns | the list calls `onFetch` from every item within the threshold, so one scroll loaded up to three pages. |
| Edits during a load | the page was merged into the items read before the request | merged into the items at arrival | a `prepend` or `remove` made while a page loaded was overwritten. |
| `reset` during a failing load | the failure was written after the reset | discarded, like a success already was | a stale error replaced the `Idle` the reset produced. |
| Cancelled load | state left in `Pending`/`Loading` | rolled back | a spinner stayed on screen for a request nobody was running. |
| Flow row fetch trigger | `fetchThreshold` items from the end, on composition | `prefetchDistance` from the bottom of its scroll | a `FlowRow` composes every item, so every page triggered the next and the whole collection loaded at once. |
| Flow row first frame | read `lastSuccess`, written in a `SideEffect` after composition | reads the current state first | the frame on which the first page arrived drew an empty flow row. |
| Item keys | the caller's key, raw | `"<windowId>_item_<key>"` | two windows showing the same entity crashed the lazy list with a duplicate key. |
| Default renderer | drew nothing, so previews showed an empty area | draws the handed state in `LocalInspectionMode`, nothing elsewhere | a preview of a screen should show the screen; the source's own architecture notes asked for exactly this rule. |
| Missing placeholder slot | `shimmerItemCount` empty items still emitted | no items emitted | empty items still took part in layout and animation for nothing. |
| Append failure without a slot | `PagingRetryFooter()`, a no-op awaiting a design-system retry | documented: no slot, no footer | the kit names no design system, so the retry is a slot the caller fills. |
| `stickyHeaders` | `Boolean` named as a noun | `hasStickyHeaders` | the project's Detekt `BooleanPropertyNaming`. |
| Targets | Android | `android`, `iosArm64`, `iosSimulatorArm64` | the same set as nav-kit, so the two kits can sit in one application. |
| Detekt findings | annotated `TODO: Detekt …` and left | findings fail the build | the port starts at zero findings. |

The layer packages this module is organised into — `data`, `domain`, `presentation` — are checked
by the `LayerPackageRequired` and `LayerPackageBoundary` Detekt rules rather than left to review;
see the root README's "Static analysis".
