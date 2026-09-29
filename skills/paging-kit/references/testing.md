# Testing

Everything but the composables is plain Kotlin and runs in `commonTest` with `kotlinx-coroutines-test`.

## A paginator's behavior

```kotlin
@Test
fun loadsUntilTheLastPage() = runTest {
    val paginator = PaginatorImpl(
        loader = PageLoader { page, _ -> Page(items = persistentListOf(page), isLastPage = page == 1) },
        identity = { it },
    )

    assertTrue(paginator.fetch().getOrThrow())
    assertFalse(paginator.fetch().getOrThrow())
    assertEquals(listOf(0, 1), paginator.observe().first().itemsOrEmpty())
}
```

## Something happening while a page loads

Gate the loader on a `CompletableDeferred`, start `fetch` in a child coroutine, `runCurrent()` so it
suspends in the loader, act, then release it:

```kotlin
@Test
fun aResetDiscardsTheLoadItInterrupted() = runTest {
    val gate = CompletableDeferred<Unit>()
    val paginator = PaginatorImpl(
        loader = PageLoader { page, _ -> gate.await(); Page(items = persistentListOf(page)) },
        identity = { it },
    )

    val load = launch { paginator.fetch() }
    runCurrent()
    paginator.reset()
    gate.complete(Unit)
    load.join()

    assertEquals(PagingState.Idle, paginator.observe().first())
}
```

## A ViewModel

Give it `PaginatorFactoryImpl()` and a fake API; set `Dispatchers.setMain(StandardTestDispatcher(testScheduler))`
so `viewModelScope` runs on the test scheduler; `advanceUntilIdle()` after each action; assert on
`viewModel.state.value`.

```kotlin
@Test
fun deletingRemovesTheRowWithoutARequest() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val api = FakeFeedApi(posts = seedPosts(40))
    val viewModel = FeedViewModel(paginatorFactory = PaginatorFactoryImpl(), api = api)
    advanceUntilIdle()

    viewModel.onDelete(postId = 1)

    assertEquals(1, api.requestCount)
    assertFalse(viewModel.state.value.itemsOrEmpty().any { it.id == 1 })
    Dispatchers.resetMain()
}
```

## What to cover

| Behavior | Assert |
|---|---|
| loader mapping | `isLastPage`/`totalCount` from the backend's envelope; cursor reset at `page == 0` |
| first-page failure | `PagingState.Error`; `fetch().isFailure` |
| later-page failure and retry | `AppendStatus.Failed`, items kept; the retry requests the same page |
| refresh | `reset()` + `fetch()` → page 0 requested, old items gone |
| search | stale page discarded after a query change; loader called with the applied query |
| edits | `prepend`/`remove` change items without a loader call |

`paging/impl/src/commonTest/.../PaginatorImplTest.kt` in the kit shows each of these patterns.
