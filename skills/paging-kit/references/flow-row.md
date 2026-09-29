# PaginationFlowRow

```kotlin
PaginationFlowRow(
    params = PaginationFlowRowParams(
        state = state,
        key = { tag -> tag.id },
        onFetch = model::onFetch,
        header = { SectionHeader("Tags") },
        shimmerContent = { index -> ChipShimmer(isFooter = index == null) },
        emptyContent = { NoTags() },
        appendErrorContent = { _, retry -> RetryFooter(onRetry = retry) },
    ),
    modifier = Modifier.weight(1f),
) { tag ->
    TagChip(tag, modifier = Modifier.padding(4.dp))
}
```

Imports: `io.thernal.pagingkit.paging.api.presentation.components.PaginationFlowRow`,
`io.thernal.pagingkit.paging.api.presentation.model.PaginationFlowRowParams`.

## How it differs from `PaginationList`

| | `PaginationList` | `PaginationFlowRow` |
|---|---|---|
| Layout | `LazyColumn`, lazy | `FlowRow` in a `verticalScroll`, **not lazy** |
| Next-page trigger | an item within `fetchThreshold` composed | remaining scroll within `prefetchDistance` (240.dp) |
| Short content | stops at one page if it fits | keeps fetching until the viewport is full or the last page |
| State changes | items swap in place | crossfade between shimmer / content / error / empty |
| Shimmer slot | `shimmer: (ShimmerSlot) -> Unit` | `shimmerContent: (Int?) -> Unit` — index, or `null` for the footer |
| Separator | `(before, after)` | `()`, inline between items |
| Several windows | yes | no — one collection |

## Params

| Field | Default | |
|---|---|---|
| `state`, `key` | — | as for lists |
| `onFetch` | `{}` | wire to `paginator.fetch()` |
| `prefetchDistance` | `240.dp` | raise for small pages or tall items |
| `shimmerItemCount` | `15` | |
| `contentType` | `{ null }` | |
| `header` | `null` | scrolls with the items, shown in every state |
| `shimmerContent` | `null` | `(Int?) -> Unit` |
| `separator` | `null` | |
| `errorContent`, `emptyContent` | `null` | `ColumnScope`; error falls back to empty |
| `appendErrorContent` | `null` | `(Throwable, retry) -> Unit` |

## Rules

- **Bounded height, no scrolling parent.** Inside `verticalScroll` or a `LazyColumn` item it measures
  infinitely tall, the remaining distance is always "near", and every page loads.
- **Not for thousands of items.** Every loaded item stays composed. For long collections use a
  `LazyVerticalGrid` driven by the same paginator, or a `PaginationList` whose rows hold several items.
- **Spacing via item padding**; `separator` only goes between items on the same line flow.
- There is no `fetchThreshold`; that is deliberate (see `paging/README.md` → "prefetch by distance").
