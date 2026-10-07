# Windows — several paged collections in one list

Catalog group **Windows**: [Two windows](#simple-two-windows) (simple) and
[Contacts](#real-life-contacts) (real life).

One scroll often holds more than one paged collection, or one collection under headers. A *window*
is one `pagedItems` (or `pagedItemsGrouped`) call inside a `PaginationList`: its own state, its own
placeholders, its own footer and its own fetch trigger, next to plain `item`s and other windows.

| File | What is in it |
|---|---|
| [`TwoWindowsViewModel.kt`](TwoWindowsViewModel.kt) | two paginators, two states, two fetch commands |
| [`TwoWindowsScreen.kt`](TwoWindowsScreen.kt) | two windows, a header between them that waits with `ifLoaded` |
| [`ContactsViewModel.kt`](ContactsViewModel.kt) | contacts sorted by name, fifteen per page |
| [`ContactsScreen.kt`](ContactsScreen.kt) | `pagedItemsGrouped` with sticky letter headers |
| [`WindowsProvidersModule.kt`](WindowsProvidersModule.kt) | catalog entries |

## Simple: Two windows

```kotlin
PaginationList {
    item(key = "pinned_header") { SectionHeader(text = "Pinned") }
    pagedItems(
        params = PagedItemsParams(
            state = pinned,
            key = { article -> article.id },
            onFetch = model::onFetchPinned,
            windowId = "pinned",
            shimmerItemCount = 2,
            shimmer = { ShimmerRow() },
        ),
    ) { article -> ItemRow(title = "📌 ${article.title}", subtitle = "pinned") }

    ifLoaded(pinned) {
        item(key = "all_header") { SectionHeader(text = "All articles") }
    }

    pagedItems(
        params = PagedItemsParams(
            state = all,
            key = { article -> article.id },
            onFetch = model::onFetchAll,
            windowId = "all",
            shimmer = { ShimmerRow() },
        ),
    ) { article -> ItemRow(title = article.title, subtitle = "id ${article.id}") }
}
```

What to notice:

- **One paginator per window.** The ViewModel holds two and exposes two states; each window gets its
  own `onFetch`.
- **`windowId` tells windows apart.** It keys each window's placeholders and footer, and it namespaces
  item keys — article 3 is in both windows here, and the lazy list does not see a duplicate key. Two
  windows with the same id fail fast with `Duplicate pagination windowId`.
- **`ifLoaded` holds the second header back** until the first window has items, so it does not float
  between two shimmers.
- **The lower window pages only when reached.** Its fetch trigger is an item near *its* end; while the
  pinned window is long enough to fill the screen, the full list stays at page 0.
- **Page size is per call.** The pinned window fetches four at a time (`fetch(size = 4)`), so it pages
  and completes quickly.

**Try it:** open *Two windows*. The pinned shimmer resolves first, then the header appears, then the
full list. Scroll: the pinned window completes after two pages, the full list keeps going.

## Real life: Contacts

A contact list sorted by name and fetched fifteen at a time — so one letter's contacts are routinely
split across two pages.

```kotlin
pagedItemsGrouped(
    params = PagedItemsGroupedParams(
        state = state,
        key = { contact -> contact.id },
        onFetch = model::onFetch,
        groupBy = { contact -> contact.name.first().uppercaseChar() },
        groupKey = { letter -> letter },
        groupHeader = { letter -> SectionHeader(text = letter.toString()) },
        hasStickyHeaders = true,
        windowId = "contacts",
        shimmer = { ShimmerRow() },
    ),
) { contact ->
    ItemRow(title = contact.name, subtitle = contact.phone)
}
```

What to notice:

- **One header per letter, however the pages fall.** Items are grouped over everything loaded so far,
  in order of first appearance; when the next page continues "K", its contacts join the "K" already
  on screen instead of starting a second one.
- **Sort on the backend by the grouping field.** Grouping follows first appearance, so an unsorted
  feed would gather a group's later items under a header far above them.
- **`hasStickyHeaders`** pins the current letter while its contacts scroll.
- **`groupKey`** must be stable and unique per group — it becomes the header's lazy-list key.

**Try it:** open *Contacts* and scroll slowly past a page boundary (every fifteen contacts). The letter
header stays pinned and never repeats.

## Doing this in your app

1. A paginator per collection in the ViewModel, each with its own `state` and `onFetch`.
2. One `pagedItems` per collection in a single `PaginationList`, each with a distinct `windowId`.
3. Headers between windows: `item(key = …)`, or `ifLoaded(state) { item(…) }` to wait for data.
4. Headers inside a window: `pagedItemsGrouped` over a backend sorted by the grouping field.

## Pitfalls

- **Two windows left on the default `windowId`** — `"paged"` twice throws.
- **A plain `item` whose key looks like `"<windowId>_item_…"`** — it can collide with a window's keys.
- **Grouping an unsorted feed** — groups merge across the whole list, far from where items arrived.
- **Expecting a `separator` in a grouped window** — there is none; draw dividers in the row.

## Read more

- [Several windows in one list](../../../../../../../../../../paging/api/README.md#several-windows-in-one-list) in the API guide
- [Grouped windows](../../../../../../../../../../paging/api/README.md#grouped-windows)
- [All examples](../../../../../../../../../README.md#the-examples)
