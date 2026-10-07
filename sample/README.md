# sample

A runnable demonstration of everything in `paging/`, on Android and iOS, from one shared
composition. Each capability has a **simple** example — the smallest thing that works — and, where
the problem has one, a **real-life** example — the shape it actually takes in an application. Every
example package has a README of its own that walks through the code, says what to notice, and how
to try it.

It is also the reference integration: [Set paging-kit up in your own app](#set-paging-kit-up-in-your-own-app)
below follows it step by step.

- The complete API guide: [`paging/api/README.md`](../paging/api/README.md)
- Why the kit is shaped the way it is: [`paging/README.md`](../paging/README.md)

## Running it

```sh
./gradlew :sample:app:installDebug          # Android
open sample/iosApp/iosApp.xcodeproj         # iOS, then run the iosApp scheme
```

The Xcode project builds the shared framework itself through a run-script phase
(`./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode`), so there is no separate Gradle step.
It sets `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` because the kit targets `iosArm64` and
`iosSimulatorArm64` only.

Every example loads from a `FakeServer` with a few hundred milliseconds of latency, so shimmers and
footers are visible, and several of them can be told to fail.

## What is where

| Module | What it is |
|---|---|
| `shared` | Every screen, ViewModel and binding. Android and iOS run this unchanged. |
| `app` | An Android application: an `Application` that owns the graph, one activity that hosts the composition. |
| `iosApp` | A SwiftUI shell whose only view is the shared composition. |

Inside `shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/`:

| Package | Holds |
|---|---|
| [`app/`](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/app) | the composition root, the graph, and the root ViewModel that owns the open example's ViewModel store — the integration |
| [`catalog/`](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/catalog) | the index screen, built from the examples the graph collected |
| [`ui/`](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/ui) | the scaffold, rows, shimmer and retry footer every example uses — the "design system" the kit leaves to the app |
| [`fake/`](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/fake) | `FakeServer`: pages an in-memory list, slowly, failing on request |
| `lists/`, `windows/`, `flowrow/`, `errors/` | one capability each — see below |

## The examples

| Group | Simple | Real life | Teaches |
|---|---|---|---|
| [Lists](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/lists/README.md) | Numbers | Post feed | the paginator in a ViewModel, `pagedItems`, shimmer; `prepend`, `remove`, pull-to-refresh with `reset`, retry footers, scroll to top |
| [Windows](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/windows/README.md) | Two windows | Contacts | several paged windows in one list, `windowId`, `ifLoaded`; `pagedItemsGrouped` with sticky headers across page boundaries |
| [Flow row](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/flowrow/README.md) | Tag cloud | — | `PaginationFlowRow`, its shimmer, and why it fetches by scroll distance |
| [Errors and resets](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/errors/README.md) | Failures and retry | Search as you type | `PagingState.Error` versus `AppendStatus.Failed`; one paginator reset per query, and stale pages discarded |

## Set paging-kit up in your own app

Four steps, each pointing at the file in this sample that does it. The generic version, including
wiring without a DI framework, is in [Installing](../paging/api/README.md#installing).

### 1. Depend on the three modules

[`shared/build.gradle.kts`](shared/build.gradle.kts): the contracts, the implementation that backs
them, and the Metro bindings that install both — all `implementation`, since no kit module
re-exports anything. The libraries whose types appear in the kit's signatures are declared beside
them ([Dependencies you declare](../paging/api/README.md#dependencies-you-declare)). The module that
declares the graph also applies the Metro plugin. A feature module needs `:paging:api` and the
libraries it uses from that list.

```kotlin
commonMain.dependencies {
    implementation(projects.paging.api)
    implementation(projects.paging.impl)
    implementation(projects.paging.wiring)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.lifecycle.viewmodel.compose)
}
```

### 2. Build one graph for the whole process

[`app/SampleGraph.kt`](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/shared/app/SampleGraph.kt):

```kotlin
@DependencyGraph(AppScope::class)
interface SampleGraph {
    val providedValues: Set<ProvidedValue<*>>   // the two renderers, from PagingProvidersModule
    val examples: Set<SampleExample>            // the sample's own index
}
```

`PagingProvidersModule` is contributed to `AppScope`, so nothing has to name it. The graph is created once
per process: [`SampleApplication.kt`](app/src/main/kotlin/io/thernal/pagingkit/sample/app/SampleApplication.kt)
on Android, a process-wide `lazy` in
[`MainViewController.kt`](shared/src/iosMain/kotlin/io/thernal/pagingkit/sample/shared/app/MainViewController.kt)
on iOS.

### 3. Install the renderers at the composition root

[`app/SampleApp.kt`](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/shared/app/SampleApp.kt):

```kotlin
@Composable
fun SampleApp(graph: SampleGraph) {
    CompositionLocalProvider(values = graph.providedValues.toTypedArray()) {
        MaterialTheme { /* … */ }
    }
}
```

Forget this and every `PaginationList` in the app is blank — no shimmer, no error, nothing — because
the default renderer draws nothing outside a preview. (Inside one it draws the state it is handed,
which is what lets a feature module preview its list without `impl`.)

### 4. Give each screen's ViewModel the factory

Every example ends in a `*ProvidersModule.kt` that receives `PaginatorFactory` from the graph and passes it
to the screen, which passes it to its ViewModel — see
[`lists/ListsProvidersModule.kt`](shared/src/commonMain/kotlin/io/thernal/pagingkit/sample/shared/lists/ListsProvidersModule.kt).
In an application the ViewModel takes it as a constructor parameter through whatever creates
ViewModels there. The ViewModel then does the rest:

```kotlin
class NumbersViewModel(paginatorFactory: PaginatorFactory) : ViewModel() {
    private val paginator = paginatorFactory.create(loader = …, identity = { it })
    val state = paginator.observe().stateIn(viewModelScope, SharingStarted.Eagerly, PagingState.Idle)
    init { onFetch() }
    fun onFetch() { viewModelScope.launch { paginator.fetch() } }
}
```

### Checklist

- [ ] One graph per process, handed to the composition root.
- [ ] The root installs `graph.providedValues`.
- [ ] ViewModels receive `PaginatorFactory`, create their own paginator, and fetch page 0 themselves.
- [ ] Every list supplies the slots its states need: a shimmer, an empty state, a first-page error,
      an append-error footer with retry.

## How the sample is put together

Not part of the kit, but worth knowing when reading it:

- **The catalog is not a navigation library.** `RootViewModel` holds the id of the open example;
  `SampleApp` shows the catalog or that example, and system back (`NavigationBackHandler` from
  `navigationevent-compose`) closes it. A real application would use its navigation — nav-kit, for
  instance.
- **Each open example gets its own `ViewModelStore`**, held by `RootViewModel` and cleared on close.
  An example's ViewModels therefore survive rotation, and reopening an example starts from page 0.
- **The `ui/` package plays the design system.** The kit draws no shimmer, no retry button, no empty
  state; the sample's are deliberately plain.

## What the sample does not show

| Capability | Where to read |
|---|---|
| wiring without Metro | [Without a DI framework](../paging/api/README.md#without-a-di-framework) |
| `PagingState.map` for UI models, `mapItems` | [PagingState](../paging/api/README.md#pagingstate) |
| a cursor-paginated backend | [Loading pages](../paging/api/README.md#loading-pages) |
| `separator` in a flow row, `header` | [PaginationFlowRow](../paging/api/README.md#paginationflowrow) |
| unit tests for a paginator or a ViewModel | [Testing](../paging/api/README.md#testing) |
