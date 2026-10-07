# Flow row — wrapping items, page by page

Catalog group **Flow row**: [Tag cloud](#simple-tag-cloud) (simple).

Chips, tags, small cards — items that wrap onto as many lines as they need rather than sitting one
per row. `PaginationFlowRow` is a vertically scrolling `FlowRow` that loads more as it nears its end
and crossfades between shimmer, content, error and empty.

| File | What is in it |
|---|---|
| [`TagCloudViewModel.kt`](TagCloudViewModel.kt) | a paginator over 400 tags, 30 per page, and a request counter |
| [`TagCloudScreen.kt`](TagCloudScreen.kt) | `PaginationFlowRow` with a chip per tag and a shimmer per chip |
| [`FlowRowProvidersModule.kt`](FlowRowProvidersModule.kt) | catalog entry |

## Simple: Tag cloud

```kotlin
PaginationFlowRow(
    params = PaginationFlowRowParams(
        state = state,
        key = { tag -> tag.id },
        onFetch = model::onFetch,
        shimmerItemCount = 24,
        shimmerContent = { index ->
            val width = if (index == null) 120.dp else (60 + index % 4 * 18).dp
            ShimmerBlock(width = width, height = 32.dp, modifier = Modifier.padding(4.dp))
        },
    ),
    modifier = Modifier.padding(horizontal = 16.dp),
) { tag ->
    SuggestionChip(onClick = {}, label = { Text(text = tag.label) }, modifier = Modifier.padding(horizontal = 4.dp))
}
```

What to notice:

- **It fetches by scroll distance, not by item.** A `FlowRow` is not lazy — it composes every chip the
  moment a page arrives. A trigger on "an item near the end was composed" would fire for every page
  immediately and load all 400 tags unprompted. The flow row asks for the next page when the
  remaining scroll is within `prefetchDistance` (240.dp by default). The note at the top counts
  requests so you can watch it happen.
- **Short content fills itself.** If a page does not fill the viewport there is nothing to scroll, the
  remaining distance is already under the threshold, and the next page is fetched — until the viewport
  is full or the last page arrives.
- **`shimmerContent` gets an index or `null`.** An index for each of the `shimmerItemCount` placeholders
  while page 0 loads — varied widths look like chips; `null` for the single footer while a later page
  loads.
- **Spacing is item padding.** `separator` exists, but it is drawn inline between chips; padding on the
  chip spaces both directions.
- **It scrolls itself.** Nesting it in another vertical scroll gives it infinite height and breaks both
  the layout and the prefetch.

**Try it:** open *Tag cloud*. The counter says 1 or 2 — enough to fill the screen. It rises only as you
scroll towards the bottom, one page at a time.

## Doing this in your app

1. The same ViewModel as any list: a private paginator, `state`, `onFetch`, a fetch in `init`.
2. `PaginationFlowRow(params = PaginationFlowRowParams(state, key, onFetch, …)) { item -> Chip(item) }`.
3. Give it a bounded height — the rest of the screen, not a scrolling parent.
4. Tune `prefetchDistance` if pages are small or items are tall.

## Pitfalls

- **Inside a `verticalScroll` or a `LazyColumn` item** — unbounded height; the whole collection loads.
- **Thousands of items** — every loaded item stays composed. Use a lazy grid, or a `PaginationList`
  whose rows hold several items, for very long collections.
- **Expecting `fetchThreshold`** — the flow row has `prefetchDistance` instead, for the reason above.

## Read more

- [PaginationFlowRow](../../../../../../../../../../paging/api/README.md#paginationflowrow) in the API guide
- [Why it fetches by distance](../../../../../../../../../../paging/README.md#paginationflowrow--prefetch-by-distance)
- [All examples](../../../../../../../../../README.md#the-examples)
