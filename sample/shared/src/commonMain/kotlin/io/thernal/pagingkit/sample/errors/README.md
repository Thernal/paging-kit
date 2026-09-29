# Errors and resets — when a page fails, and when the question changes

Catalog group **Errors and resets**: [Failures and retry](#simple-failures-and-retry) (simple) and
[Search as you type](#real-life-search-as-you-type) (real life).

A page can fail, and the collection being paged can stop being the one the user wants. The first is
two states — which one depends on whether anything had loaded yet. The second is one command,
`reset`, and the guarantee that whatever it interrupted cannot come back.

| File | What is in it |
|---|---|
| [`FailuresViewModel.kt`](FailuresViewModel.kt) | a server that fails on request, a paginator, "start over" |
| [`FailuresScreen.kt`](FailuresScreen.kt) | `errorContent` and `appendErrorContent`, and the state printed live |
| [`SearchViewModel.kt`](SearchViewModel.kt) | a loader that reads the query; a debounced reset per change |
| [`SearchScreen.kt`](SearchScreen.kt) | a text field above a paged list of results |
| [`ErrorsBindings.kt`](ErrorsBindings.kt) | catalog entries |

## Simple: Failures and retry

The screen opens with its first request set to fail.

```kotlin
PagedItemsParams(
    state = state,
    key = { row -> row },
    onFetch = model::onFetch,
    shimmer = { ShimmerRow() },
    errorContent = {
        FullMessage(title = "PagingState.Error", body = "…", action = "Retry", onAction = model::onFetch)
    },
    appendErrorContent = { error, retry ->
        RetryFooter(message = "AppendStatus.Failed — ${error.message}", onRetry = retry)
    },
)
```

What to notice:

- **The first page fails as `PagingState.Error`.** Nothing has loaded, so there is nothing to keep:
  `errorContent` fills the list's viewport. It has no retry parameter — it calls the ViewModel's fetch.
- **A later page fails as `AppendStatus.Failed`.** The loaded rows stay; `appendErrorContent` is the
  footer, and it is handed `retry`.
- **A retry asks for the same page again.** The paginator's page number only advances on success.
- **Nothing retries by itself.** The list asks for more only while the append status is `Idle`; after
  a failure it waits for the footer's button. Leave `appendErrorContent` out and the list simply
  stops.
- **Both failures are also returned** as `Result.failure` from `fetch`, for a caller that wants to log
  or show a snackbar; this screen ignores it.

**Try it:** open *Failures and retry*. The error fills the screen; tap *Retry*. Tap *Fail the next page*
and scroll down: the footer shows the failure under the rows you already have; tap *Retry* in it.
*Start over* resets and fails the first page again.

## Real life: Search as you type

One paginator for the life of the screen. Its loader reads the query the paginator was last reset
for, so a new query is a reset and a fetch, not a new paginator:

```kotlin
private var activeQuery = ""

private val server = FakeServer(latency = 900.milliseconds) {
    val current = activeQuery.trim()
    cities.filter { city -> city.contains(other = current, ignoreCase = true) }.flatMap { … }
}

private val paginator = paginatorFactory.create(
    loader = PageLoader { page, size -> server.load(page = page, size = size) },
    identity = { place -> place.id },
)

private fun observeQuery() {
    viewModelScope.launch {
        mutableQuery.drop(1).debounce(searchDebounce).collect { query ->
            activeQuery = query
            paginator.reset()
            onFetch()
        }
    }
}
```

What to notice:

- **A reset discards what it interrupted.** Each reset starts a new generation. A page for "Ba" still
  in flight when the user has typed "Bak" is dropped when it returns — success or failure — so a slow
  answer never lands under a newer query. There is no request id to track in the ViewModel.
- **The loader reads `activeQuery`, not the text field.** The field runs ahead of the debounce; a page
  the list asks for in between must still belong to the results on screen.
- **`reset` does not fetch.** The ViewModel decides when page 0 is wanted — here, after the debounce.
- **Paging continues per query.** Type "a": many results; scroll and further pages load for "a". Change
  the query and paging starts over from page 0 for the new one.
- **`emptyContent` covers "no matches".** A query with no results is `Success` with no items, which the
  list renders as its empty slot.
- **`drop(1)`** skips the initial empty query, which `init` already fetched.

**Try it:** open *Search as you type*, type "a" and scroll a few pages, then quickly type "ş" — the list
shows a shimmer and then only matching cities, never a flash of results for "a".

## Doing this in your app

1. Always supply `appendErrorContent` with a retry, and `errorContent` for the first page.
2. For a filter, a sort or a search: keep the criteria in the ViewModel, let the loader read them, and
   `reset()` + `fetch()` when they change — on the same paginator.
3. Debounce text input before resetting; do not reset per keystroke.

## Pitfalls

- **No `appendErrorContent`** — a failed page is a dead end.
- **A new paginator per query** — the old one keeps loading, unobserved; its results cost requests
  for nothing.
- **Criteria captured at creation** — a loader that closes over the first query's value keeps
  searching for it after every reset.
- **Criteria read live from the input** — a later page requested before the debounce fires loads for
  the new text and is appended to the old results. Read the criteria the paginator was last reset
  for.
- **Reset without fetch** — the list sits in `Idle` and shows `emptyContent`.

## Read more

- [PagingState](../../../../../../../../../../paging/api/README.md#pagingstate) in the API guide
- [Placeholders, empty, errors and retry](../../../../../../../../../../paging/api/README.md#placeholders-empty-errors-and-retry)
- [Refresh, search and other resets](../../../../../../../../../../paging/api/README.md#refresh-search-and-other-resets)
- [All examples](../../../../../../../../../README.md#the-examples)
