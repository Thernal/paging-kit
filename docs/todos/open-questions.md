# Open questions

What the port deliberately left as it was, and why each is worth revisiting. Nothing here is a
defect in the current contracts; each is a capability the kit does not have yet, with the shape a
change would probably take and what it would cost.

## 1. Cursor and keyset pagination

**Now.** `PageLoader.load(page: Int, size: Int)` receives a page index. A cursor backend keeps its
cursor inside the loader and clears it when `page == 0` (see `paging/api/README.md` → "Loading
pages"). It works, but the loader becomes stateful, and the cursor is not reset by `reset()` — only
by the loader noticing page 0.

**Shape of a change.** A key type on the loader: `PageLoader<K, T>.load(key: K?, size): Page<K, T>`,
with `Page.nextKey` replacing `isLastPage`. The paginator would hold the next key instead of a page
number, and page-number backends would use `K = Int`.

**Cost.** Every loader and every `Paginator<T>` gains a type parameter, or a second paginator type
exists beside the first. Worth doing once a real cursor backend is in front of the kit.

## 2. Loading backwards and dropping far pages

**Now.** Append only. A chat, or a feed opened at a notification in its middle, cannot load older
*and* newer pages; and every loaded item stays in memory until a reset.

**Shape of a change.** `fetchPrevious()` and a `prependStatus` mirroring `appendStatus`; a
`maxItems` after which the far end is dropped and re-fetched on demand. That is most of what Jetpack
Paging's `PagingSource`/`PagingData` pair does, which is the argument for stopping short of it: the
kit's value is being small.

## 3. A default retry footer and refresh container

**Now.** The kit names no design system, so a list without `appendErrorContent` shows no footer when
a later page fails, and nothing retries. The source module had a `PagingRetryFooter()` stub waiting
for the app's design system, and a TODO to restore pull-to-refresh "once the design system ships a
refresh container".

**Options.** (a) Keep it a slot and keep documenting it — the current choice. (b) An
`unstyled` default drawn with `foundation` only (`BasicText` + `clickable`). (c) A
`PaginationDefaults` object an app fills once at the root, so every list gets the app's footer
without passing it. (c) fits the render-contract pattern best: another `CompositionLocal` beside the
renderer.

## 4. Scroll position across process death

**Now.** A paginator is in memory. After process death, `rememberLazyListState` restores a scroll
index into a list that starts again from page 0, so the list jumps to the top when the first page is
shorter than the saved index.

**Shape of a change.** Save the loaded page count (or the first visible key) in `SavedStateHandle`,
and let the owner fetch that many pages before handing the state to the list. That is an owner
concern, possibly a helper, not a paginator change.

## 5. A lazy flow row

**Now.** `PaginationFlowRow` composes every loaded item. Fine for tags and chips; wrong for thousands
of cards.

**Shape of a change.** `ContextualFlowRow` was the foundation answer and is deprecated; a
`LazyVerticalStaggeredGrid`-backed component is the likely replacement, with the list's
item-position trigger rather than the flow row's distance trigger.
