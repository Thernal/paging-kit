# Troubleshooting and review

## Contents

1. By error message
2. By symptom
3. Review checklist

## 1. By error message

| Message | Cause | Fix |
|---|---|---|
| `IllegalArgumentException: Duplicate pagination windowId: <id>` | two windows in one `PaginationList` share a `windowId` — usually both left on the default `"paged"` | give each window its own `windowId` |
| `IllegalArgumentException: Key "<…>" was already used` (LazyColumn) | a plain `item`'s key equals a window key (`"<windowId>_item_<key>"`, `"<windowId>_shimmer_<i>"`, `"<windowId>_group_<key>"`), or two items in one window have keys with the same `toString()` | unique keys for plain items; a `key` whose string form is unique |
| `Type of the key … is not supported. On Android you can only use types which can be stored inside the Bundle` | a plain `item(key = …)` with a non-Bundle key (window keys are already strings) | use a `String`/`Int` key |
| `Vertically scrollable component was measured with an infinity maximum height constraints` | `PaginationList` or `PaginationFlowRow` inside a vertical scroll or a `LazyColumn` item | give it bounded height; use windows / `item`s instead of nesting |

## 2. By symptom

| Symptom | Likely cause | Fix |
|---|---|---|
| the list is completely blank — no shimmer, no empty state | renderer locals not installed (previews are the exception: there the default draws the state) | `CompositionLocalProvider(values = graph.providedValues.toTypedArray())` at the root |
| shows `emptyContent` forever | nobody fetched page 0; or `reset()` without `fetch()` | `fetch()` in the owner's `init` and after every `reset` |
| blank while loading, then items | no `shimmer` slot | supply `shimmer` |
| stops loading after an error; no footer | no `appendErrorContent`; the trigger only fires while `Idle` | supply `appendErrorContent` using its `retry` |
| never loads a second page | an `isLoading` guard never cleared; `onFetch` not wired; `isLastPage` true on page 0 (e.g. `items.size < size` with a server that caps size lower) | call `fetch()` unguarded; check the loader's `isLastPage` |
| loads every page at once | `PaginationFlowRow` (or `PaginationList`) with unbounded height, inside a scroll | bounded height, no scrolling parent |
| two or three pages load per scroll | the ViewModel creates a new coroutine *and* a new paginator, or several paginators share one loader | one private paginator per collection; `fetch` already de-duplicates |
| items appear twice | `identity` not the entity id (e.g. `{ it }` over data classes whose fields change) | `identity = { it.id }` |
| a deleted item comes back | a later page from the backend still contains it | delete on the backend first, then `remove(id)` |
| old query's results appear under a new query | the loader reads the live input rather than the applied criteria; or a new paginator per query | hold `activeQuery`, set it before `reset()`; one paginator |
| refresh shows a shimmer instead of the old items | expected: `reset` returns to `Idle`/`Pending` | cache the last `Success` in the ViewModel while refreshing |
| spinner stuck after leaving and returning | a `fetch` launched in a composable scope that was cancelled (fixed in the kit: state rolls back) — or a custom flag in the ViewModel | launch fetches in `viewModelScope`; no custom flags |
| edits animate the wrong row | `key` differs from `identity` | same id for both |
| lower window never loads | expected until scrolled to; or its `onFetch` points at the upper window's paginator | wire each window's own `onFetch` |
| group header repeated | backend not sorted by the grouping field; or `groupKey` unstable | sort by the grouping field; stable `groupKey` |
| state lost on rotation | paginator held in a composable (`remember`) instead of a ViewModel | move it into the ViewModel |

## 3. Review checklist

- [ ] `PaginatorFactory` is injected; `Paginator` is never injected, shared or stored outside its owner.
- [ ] `identity` and `key` use the same entity id.
- [ ] Page 0 is fetched by the owner (in `init`, and after every `reset`).
- [ ] `onFetch` goes straight to `paginator.fetch()`, launched in `viewModelScope`, with no guard.
- [ ] The loader throws on failure and never catches `CancellationException`.
- [ ] The loader maps `isLastPage` correctly for the backend (flag, `totalPages`, or `items.size < size`).
- [ ] Every window in one list has a distinct `windowId`; plain item keys cannot collide with window keys.
- [ ] `shimmer`, `emptyContent`, `errorContent` and `appendErrorContent` are supplied as the design needs;
      `appendErrorContent` is always supplied when the network can fail.
- [ ] Refresh and criteria changes use `reset()` + `fetch()` on the same paginator, and the loader reads
      the applied criteria.
- [ ] `PaginationList`/`PaginationFlowRow` have bounded height and no scrolling parent.
- [ ] The composition root installs the renderer locals once.
