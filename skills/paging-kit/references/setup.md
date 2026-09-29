# Installing paging-kit into an application

## Contents

1. Dependencies
2. The application graph — with Metro, or by hand
3. The composition root
4. Giving ViewModels the factory
5. Previews
6. Verify

## 1. Dependencies

The modules are not published to a Maven repository; the project builds against them from source
(`paging/api`, `paging/impl`, `paging/wiring` in https://github.com/Thernal/paging-kit, which also
carries the Gradle conventions and version catalog they expect). Targets: `android`, `iosArm64`,
`iosSimulatorArm64`.

The source is copied into the application. With skill-manager that is one command, which renames the copy
and records where it came from, so later paging-kit changes can be merged into it:

```sh
skillctl.sh kit install paging-kit --package com.example.app --module :core:paging --alias example
```

It prints what the copied modules expect — the `requires` list in `kit.yml` at the repository root: the
convention plugins behind `libs.plugins.<alias>.compose` and `.injection`, the catalog entries, and
`TYPESAFE_PROJECT_ACCESSORS`. Nothing in the application's build is written for you; add those, then
include each copied directory as a module.

| Module | Add to | As |
|---|---|---|
| `:paging:api` | every feature module with paged data or UI; the app | `api` in the app's shared module, `implementation` elsewhere |
| `:paging:impl` | the module that builds the graph (or wires by hand) | `implementation` |
| `:paging:wiring` | the module declaring the Metro graph — skip when wiring by hand | `implementation` |
| `:paging:preview` | feature modules with previews of paged screens — optional | `implementation` |

`:paging:api` re-exports `kotlinx-coroutines-core` and `kotlinx-collections-immutable` and applies
Compose. Paginators are owned by ViewModels, so the app normally also has
`androidx.lifecycle:lifecycle-viewmodel-compose`.

## 2. The application graph

### With Metro

`PagingWiring` is `@BindingContainer @ContributesTo(AppScope::class)`: a graph over `AppScope`
includes it automatically. It binds `PaginatorFactory`, `PaginationListRenderer` and
`PaginationFlowRowRenderer` as singletons and contributes two `ProvidedValue<*>` into
`Set<ProvidedValue<*>>`.

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val providedValues: Set<ProvidedValue<*>>
}

fun createAppGraph(): AppGraph {
    return createGraph<AppGraph>()
}
```

Do not bind `Paginator`. If the app already collects `Set<ProvidedValue<*>>` (nav-kit's
`NavigationWiring` contributes to the same set), nothing changes: the paging renderers join it.

### By hand (any other container, or none)

```kotlin
object Paging {
    val paginatorFactory: PaginatorFactory = PaginatorFactoryImpl()
    val providedValues: Array<ProvidedValue<*>> = arrayOf(
        LocalPaginationListRenderer provides PaginationListRendererImpl(),
        LocalPaginationFlowRowRenderer provides PaginationFlowRowRendererImpl(),
    )
}
```

Imports: `io.thernal.pagingkit.paging.impl.domain.paginator.PaginatorFactoryImpl`,
`io.thernal.pagingkit.paging.impl.presentation.components.{PaginationListRendererImpl, PaginationFlowRowRendererImpl}`,
`io.thernal.pagingkit.paging.api.presentation.components.{LocalPaginationListRenderer, LocalPaginationFlowRowRenderer}`.

For Koin/Hilt/kotlin-inject: bind `PaginatorFactoryImpl()` as a singleton `PaginatorFactory`, and
provide the two locals at the root as above.

## 3. The composition root

```kotlin
@Composable
fun AppRoot(graph: AppGraph) {
    CompositionLocalProvider(values = graph.providedValues.toTypedArray()) {
        AppTheme { AppContent() }
    }
}
```

- Create the graph **once per process** — the Android `Application`, a top-level `lazy` for the iOS
  `ComposeUIViewController` — and pass it in. Never create it inside a composable.
- Install the locals above every screen that shows a paged list, including dialogs and sheets hosted
  in separate windows if they have their own composition.

## 4. Giving ViewModels the factory

Whatever creates ViewModels passes `PaginatorFactory` in:

```kotlin
class FeedViewModel(
    paginatorFactory: PaginatorFactory,
    private val api: FeedApi,
) : ViewModel() {
    private val paginator = paginatorFactory.create(loader = FeedPageLoader(api), identity = { it.id })
    // …
}
```

With Metro, an `@Inject` constructor; with a `viewModel { }` initializer, read the factory from the
graph at the call site. See [paginator.md](paginator.md) for the rest of the ViewModel.

## 5. Previews

Nothing to install. In `LocalInspectionMode` the default renderers draw the `PagingState` they are
handed — items, shimmers, footer, empty and error slots — once, without fetching. Preview the
screen's stateless content composable with a hand-made state:

```kotlin
@Preview
@Composable
private fun FeedPreview() {
    FeedContent(state = PagingState.Success(items = persistentListOf(samplePost)), onFetch = {})
}
```

`PagingState.Pending` previews the shimmers, `Error` the error slot, `Success(appendStatus = Failed(…))`
the retry footer.

Every state in one go: depend on `:paging:preview` and subclass its provider per item type (a
`@PreviewParameter` class needs a no-arg constructor, so the generic class cannot be named directly):

```kotlin
private class FeedStates : PagingPreviewParameterProvider<Post>(items = listOf(post1, post2))

@Preview(heightDp = 640)
@Composable
private fun FeedPreview(@PreviewParameter(FeedStates::class) state: PagingState<Post>) {
    FeedContent(state = state, onFetch = {})
}
```

It yields `LOADING`, `LOADED`, `LOADING_MORE`, `LOAD_MORE_FAILED`, `END_REACHED`, `ERROR`, `EMPTY`
(narrow with `states = listOf(…)`), each preview named after its state. Give previews a `heightDp`:
empty and error slots fill the list's viewport. Outside a preview the defaults draw nothing — keep installing the renderers at the
root.

## 6. Verify

- [ ] A list shows its shimmer while page 0 loads. (Blank instead: the locals are not installed.)
- [ ] Scrolling to the end loads the next page, once.
- [ ] `grep -rn "PaginatorFactoryImpl()" .` finds exactly one construction (the graph or the manual
      object), not one per screen.
