---
name: paging-kit
description: Builds, wires, reviews and debugs paginated lists in Compose Multiplatform apps that use paging-kit, the kit in packages io.thernal.pagingkit.paging.* (PagingPreviewParameterProvider, PageLoader, Page, PagingState, AppendStatus, Paginator, PaginatorFactory, PaginationList, pagedItems, PagedItemsParams, pagedItemsGrouped, PagedItemsGroupedParams, PaginationFlowRow, PaginationFlowRowParams, ShimmerSlot, PaginationListState, PagingWiring). Use it for any paging or infinite-scroll work in such a project, even when paging-kit is not named - installing the kit, loading a backend's pages, a feed or list that loads more on scroll, several paged sections in one list, grouped or sticky-header lists, wrapping chips that page, shimmer placeholders, empty and error states, retry on a failed page, pull to refresh, search as you type, adding or deleting an item without a reload, scroll to top, tests - and for lists that stay blank, load every page at once, never load more, or crash on a duplicate key. Not for Jetpack Paging 3 (PagingSource, Pager, LazyPagingItems) or hand-rolled pagination without paging-kit.
---

# paging-kit

paging-kit is a pagination layer for Compose Multiplatform (Android, iosArm64, iosSimulatorArm64): a
loader a backend implements, one state type for a paginated collection, a paginator that moves it,
and two render contracts — a multi-window `LazyColumn` (`PaginationList`) and a wrapping `FlowRow`
(`PaginationFlowRow`). Source and the complete API guide: https://github.com/Thernal/paging-kit —
`paging/api/README.md`; `sample/` runs every capability.

Three modules. `paging/api` (package `io.thernal.pagingkit.paging.api`) holds the contracts and is all
a feature module needs. `paging/impl` implements them. `paging/wiring` binds them into a
[Metro](https://github.com/ZacSweers/metro) graph.

## 1. Orient before writing

Most paging-kit bugs come from a second mechanism beside the kit's (an `isLoading` flag, a
hand-written scroll listener, a paginator per query) or from a missing installation. Find the
existing pieces first:

```sh
grep -rn --include=*.kt -e "PagingWiring" -e "PaginationListRendererImpl" -e "LocalPaginationListRenderer provides" .  # installed?
grep -rn --include=*.kt -e "PaginatorFactory" -e "paginatorFactory.create" .      # who owns paginators
grep -rn --include=*.kt -e ": PageLoader<" -e "PageLoader {" .                    # loaders and their backends
grep -rn --include=*.kt -e "PaginationList" -e "pagedItems" -e "PaginationFlowRow" .  # every paged UI
grep -rn --include=*.kt -e "windowId =" .                                         # window ids already used per list
```

If nothing is installed, read [references/setup.md](references/setup.md) before anything else.

**Taken as a kit?** A `kits.lock` at the project root naming `paging-kit` means the code was copied with
skill-manager, renamed to the project's package and module path — and this skill with it, so the names
here are already the project's. `skillctl.sh kit status paging-kit` says whether paging-kit has moved
since and what changed; offer `kit update paging-kit` rather than editing towards a newer version by hand.

## 2. The model

These hold everywhere; each reference builds on them.

1. **A loader returns a `Page`, and throws to fail.** `PageLoader<T>.load(page, size): Page<T>` with
   `Page(items, totalCount, isLastPage)`. `page` is zero-based; a failed page is retried with the same
   number. The backend's envelope is mapped onto `Page` inside the loader.
2. **A paginator belongs to one owner.** Inject `PaginatorFactory`; `create(loader, identity)` in the
   ViewModel; keep it private. Never inject, scope or share a `Paginator`.
3. **Nothing loads on its own.** The owner calls `fetch()` for page 0 — in `init` — and again after
   every `reset()`. The list's `onFetch` only asks for later pages.
4. **`fetch` is safe to over-call.** Calls for a page already loading load it once; calls after the
   last page return `false` without loading. Wire `onFetch = vm::onFetch` straight to it, no guard.
5. **Two failures.** First page fails → `PagingState.Error` (nothing to keep). A later page fails →
   `Success` with `AppendStatus.Failed` (items stay). Nothing retries by itself.
6. **Edits don't reload.** `prepend`, `insertAt`, `remove(identity)` change the loaded list in place and
   survive a page arriving mid-edit.
7. **`reset` discards, it does not fetch.** It returns to `Idle` and drops any load in flight when that
   load returns. Refresh and new criteria are `reset()` + `fetch()` on the same paginator.
8. **The composables are render contracts.** `PaginationList`/`PaginationFlowRow` draw through
   `LocalPaginationListRenderer`/`LocalPaginationFlowRowRenderer`, installed once at the root. Without
   them they draw nothing, except in a preview, where they draw the handed state statically — so
   previews need no `impl`. The kit draws no shimmer, empty, error or retry UI — those are slots.

## 3. Route the task

| The task | Mechanism | Read |
|---|---|---|
| install the kit, the composition root, wiring without Metro | graph + provided values | [setup.md](references/setup.md) |
| preview a paged screen in every state | `PagingPreviewParameterProvider` (`:paging:preview`) | [setup.md](references/setup.md) |
| implement a loader over an API (page numbers, cursors, "no total") | `PageLoader`, `Page` | [paginator.md](references/paginator.md) |
| a ViewModel holding a paged collection; add/delete without reload | `PaginatorFactory`, `Paginator` | [paginator.md](references/paginator.md) |
| a list that loads more on scroll | `PaginationList` + `pagedItems` | [lists.md](references/lists.md) |
| several paged sections in one list; headers between them | windows, `windowId`, `ifLoaded` | [lists.md](references/lists.md) |
| a list grouped by letter/date, sticky headers | `pagedItemsGrouped` | [lists.md](references/lists.md) |
| wrapping chips/tags/cards that page | `PaginationFlowRow` | [flow-row.md](references/flow-row.md) |
| shimmer, empty state, error state, retry footer | slots on the params | [states-and-errors.md](references/states-and-errors.md) |
| pull to refresh, search, filters, sort changes | `reset()` + `fetch()` | [states-and-errors.md](references/states-and-errors.md) |
| unit tests for a paginator or a ViewModel | `PaginatorImpl`, fake loaders | [testing.md](references/testing.md) |
| a blank list, a list that loads everything or nothing, an exception; reviewing paging code | — | [troubleshooting.md](references/troubleshooting.md) |

Read only the references the task needs.

## 4. Implement, install, verify

A paging change is usually correct in its own file and broken by something elsewhere. Before calling
the work done, walk this:

- [ ] The root installs the graph's `providedValues` (or the two renderer locals by hand).
- [ ] The ViewModel receives `PaginatorFactory`, creates its paginator privately, and fetches page 0.
- [ ] `identity` (factory) and `key` (params) are the same entity id.
- [ ] Every window in one `PaginationList` has a distinct `windowId`.
- [ ] `appendErrorContent` is supplied wherever loads can fail; `errorContent` calls the ViewModel's fetch.
- [ ] Refresh / new criteria reset the existing paginator and then fetch; the loader reads the criteria
      the paginator was reset for.
- [ ] `PaginationFlowRow` has a bounded height and no scrolling parent.
- [ ] The project builds and its tests pass. For paginator logic, add a `commonTest` (see
      [testing.md](references/testing.md)).
- [ ] The review checklist in [troubleshooting.md](references/troubleshooting.md) passes.

## 5. Traps that look reasonable

| Tempting | Why it breaks | Instead |
|---|---|---|
| injecting `Paginator<T>` or making it a singleton | two owners page each other's lists; there is no singleton per `T` | inject `PaginatorFactory`, create per owner |
| `if (!isLoading) paginator.fetch()` | redundant; a flag not cleared on failure stops the list forever | call `fetch()` unguarded |
| a scroll listener that fetches near the end | duplicates the list's own trigger | `onFetch` on the params |
| forgetting `fetch()` in `init` / after `reset()` | an `Idle` list is empty; the list only fetches later pages | fetch page 0 in the owner |
| a new paginator per query or per refresh | the old one keeps loading, unobserved; state is orphaned | `reset()` + `fetch()` on one paginator |
| the loader reading the text field's live value | a page requested before the debounce loads for new text and appends to old results | read the criteria the paginator was last reset for |
| omitting `appendErrorContent` | a failed later page shows nothing and never retries | supply a footer with the `retry` it is handed |
| two windows on the default `windowId` | `Duplicate pagination windowId: paged` | a distinct id per window |
| `key = { it }` over whole objects while `identity = { it.id }` | edits move one item, the list animates another; unstable keys | key and identity on the same id |
| `PaginationFlowRow` inside `verticalScroll`/`LazyColumn` | unbounded height; every page loads at once | give it the remaining height |
| mutating the list returned by `map`/`mapItems` to "update" it | changes a copy; the paginator is unaffected | `prepend`/`insertAt`/`remove` |
| catching exceptions in the loader and returning an empty `Page` | turns a failure into "loaded, empty, maybe last" | let it throw |
