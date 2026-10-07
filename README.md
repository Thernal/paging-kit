# paging-kit

A Compose Multiplatform pagination layer — a page loader, one state type for a paginated
collection, a paginator that loads, retries, edits and resets it, and two render contracts: a
`LazyColumn` that holds any number of paged windows and a wrapping `FlowRow` — split across
`api` / `impl` / `wiring` so nothing depends on a concrete implementation or on a
dependency-injection framework.

Ported from the `core/pagination` module of an Android-only app; what changed on the way across —
including the defects fixed — is recorded in [`paging/README.md`](paging/README.md) → "What changed
in the port". It is the sibling of [nav-kit](../nav-kit) and shares its layout, conventions and
target set, so the two can sit in one application.

## Documentation

| I want to… | Read |
|---|---|
| use the kit — every public contract, task by task, with the rules that matter | [`paging/api/README.md`](paging/api/README.md) |
| see each capability running, and set the kit up in my own app | [`sample/README.md`](sample/README.md), then the README in each example package |
| understand why a contract has the shape it has | [`paging/README.md`](paging/README.md) |
| know what is still open in the design | [`docs/todos/open-questions.md`](docs/todos/open-questions.md) |
| give an AI agent the same knowledge | [`skills/`](skills/README.md) — see below |

## Installing

An application takes the kit **by copy**, not as a dependency: the code is copied into the app, renamed to the app's own package, and belongs to the app from then on. Nothing is published to a Maven repository.

### With skill-manager

If you have access to the author's knowledge repository (`github.com/Thernal/knowledge`), its **skill-manager** skill does all of it — copy, rename, the skill, and later updates:

```sh
skillctl.sh kit install paging-kit --package com.example.app --module :core:paging --alias app
```

It copies the `code` parts of [`kit.yml`](kit.yml) renamed, installs the `paging-kit` skill and records the copy in `kits.lock`. `kit status` then shows what changed upstream and what the app edited; `kit update` merges the kit's changes three ways, keeping the app's edits. The install prints what the app must provide (`requires`).

### Without it

The same by hand, from a clone of this repository.

1. **Copy** the paths listed under `code` in [`kit.yml`](kit.yml) into the app, under the module path the app gives them: `paging/…` → `core/paging/…`. Note the commit you copied (`git rev-parse HEAD`) — updates start from it.
2. **Rename** in everything copied:

   | In the kit | Becomes | Where |
   |---|---|---|
   | `io.thernal.pagingkit` | the app's package, e.g. `com.example.app` | sources, build files; and the directories `io/thernal/pagingkit` |
   | `:paging:` and `":paging"`, `projects.paging.` | the module path, e.g. `:core:paging:`, `projects.core.paging.` | build files |
   | `libs.plugins.pagingkit.` | the app's catalog alias, e.g. `libs.plugins.app.` | build files |

   ```sh
   # in the app, after copying — perl, so it runs the same on macOS and Linux
   grep -rlI -e io.thernal.pagingkit -e io/thernal/pagingkit -e :paging -e plugins.pagingkit. core/paging \
     | xargs perl -pi -e 's/\Qio.thernal.pagingkit\E/com.example.app/g; s{\Qio/thernal/pagingkit\E}{com/example/app}g; s/\Q:paging:\E/:core:paging:/g; s/"\Q:paging\E"/":core:paging"/g; s/projects\.\Qpaging\E\./projects.core.paging./g; s/libs\.plugins\.\Qpagingkit\E\./libs.plugins.app./g'
   find core/paging -depth -type d -path '*/io/thernal/pagingkit' | while read -r d; do
     mkdir -p "${d%/io/thernal/pagingkit}/com/example" && mv "$d" "${d%/io/thernal/pagingkit}/com/example/app"
   done
   find core/paging -depth -type d -empty -delete
   ```

3. **Provide** what the copy expects — the `requires` list in [`kit.yml`](kit.yml): convention plugins (build-kit's, or the ones in this repository's `build-logic/convention`), catalog entries, settings — and, where listed, platform setup.
4. **The skill** (optional): copy [`skills/paging-kit`](skills/paging-kit) into the app's skills directory (`.claude/skills/` for Claude Code), with the same renames, so an agent working in the app knows the kit.
5. **Updates** are yours to carry: `git diff <the commit you copied> <a newer one> -- <the code paths>` in the kit shows what changed; apply what you want, renamed the same way.

## For AI agents

**Helping a project that uses paging-kit?** The usage skill lives in [`skills/paging-kit/`](skills/paging-kit/SKILL.md).
Start at `skills/paging-kit/SKILL.md`: it states the model, the checks to run in the project first, and
routes each task to one file under `skills/paging-kit/references/` — `setup.md`, `paginator.md`,
`lists.md`, `flow-row.md`, `states-and-errors.md`, `testing.md`, `troubleshooting.md`. Read it in place,
or install it so your agent loads it on its own:

```sh
git clone --depth 1 https://github.com/Thernal/paging-kit.git /tmp/paging-kit
mkdir -p .claude/skills && cp -R /tmp/paging-kit/skills/paging-kit .claude/skills/   # Claude Code; other runtimes: their skills directory
```

[`skills/README.md`](skills/README.md) lists every skill and the install options. An application takes the code
by copying it; [`kit.yml`](kit.yml) declares what is copied, what is only read, what the copy expects of the
application's build, and the names an application renames — skill-manager's `kit install` / `kit update`
read it to copy, rename and later merge. The skill restates
[`paging/api/README.md`](paging/api/README.md) for an agent; when in doubt, that README and the code
under `paging/api` are the source of truth.

**Working on paging-kit itself?** The skill above is not for you. Read this README, `paging/README.md`
for the design, and [`.agents/workspace.md`](.agents/workspace.md) for this repository's delivery
conventions and where each kind of artifact belongs. `.claude/skills/` holds vendored workflow skills
(task planning, git delivery) — they are not about paging.

## Layout

| Path | What |
|---|---|
| `paging/api` | `PageLoader`, `Page`, `PagingState`, `Paginator`, `PaginatorFactory`; the `PaginationList` and `PaginationFlowRow` render contracts and their params. Depends on no implementation. |
| `paging/impl` | `PaginatorImpl`, the `LazyColumn` and `FlowRow` hierarchies behind the render contracts, and their renderers. |
| `paging/wiring` | The worked example of installing the above into an application graph, with [Metro](https://github.com/ZacSweers/metro). `api` and `impl` name no container, so an app on a different one replaces just this module. |
| `paging/preview` | `PagingPreviewParameterProvider` — every `PagingState` of a list as `@Preview` parameters. Optional; depends on `ui-tooling-preview`. |
| `sample/designsystem` | A small design system and its mapping onto `PagingStyles` — how an app styles the kit. Never copied. |
| `sample/` | A runnable Android and iOS app with a simple and a real-life example of every capability — see [`sample/README.md`](sample/README.md). |
| `skills/` | Agent skills for projects that use the kit — see [For AI agents](#for-ai-agents). |
| `docs/` | Design notes and open work. |
| `build-logic/convention` | Five convention plugins — `kmp.library`, `compose`, `injection`, `android.application` for the sample app, and a `quality` one with no plugin id that the first and the fourth apply. A module names capabilities, never versions. |
| `build-logic/detekt-rules` | The project's own Detekt rules — see "Static analysis" below. |

Modules are discovered from the tree: any directory under `paging/` or `sample/` with a
`build.gradle.kts` is a Gradle project, so `settings.gradle.kts` has no list to keep in sync.

## Targets

`android`, `iosArm64`, `iosSimulatorArm64` — the same set as nav-kit. Compose Multiplatform publishes
`desktop`, `js`, `wasmJs` and `iosX64` variants as well, and nothing in `paging/` is platform-specific,
so adding a target is a line in `KmpLibraryConventionPlugin` — not verified here, since nothing in
this repository builds for them yet.

`commonTest` runs on the Android host-test (JVM) compilation and on the iOS simulator, so every test
in this repository is executed twice on different backends.

## Building

```
./gradlew build          # compile every target, run the tests on both, and run Detekt
./gradlew :paging:impl:allTests
./gradlew detektMainAndroid --auto-correct   # apply the ktlint-formatting fixes
```

**The Gradle daemon runs on JDK 21**, declared in `gradle/gradle-daemon-jvm.properties`: Metro's
Gradle plugin is compiled for JVM 21, so an older daemon cannot even configure the build. The file
carries download URLs, so Gradle provisions that JDK into its own cache on a machine that has none
— nothing has to be installed by hand. Modules themselves target the `jvm` version in
`gradle/libs.versions.toml` through `jvmToolchain`, which is a separate setting.

`local.properties` must point at an Android SDK (`sdk.dir=…`); it is git-ignored. Running the iOS
tests and linking the iOS framework need Xcode selected (`xcode-select -s /Applications/Xcode.app`);
Command Line Tools alone compile the iOS sources but cannot link them.

## Using it

```kotlin
class FeedViewModel(paginatorFactory: PaginatorFactory, api: FeedApi) : ViewModel() {
    private val paginator = paginatorFactory.create(
        loader = PageLoader { page, size -> api.posts(page, size).toPage() },
        identity = { post -> post.id },
    )
    val state = paginator.observe().stateIn(viewModelScope, SharingStarted.Eagerly, PagingState.Idle)

    init { onFetch() }
    fun onFetch() { viewModelScope.launch { paginator.fetch() } }
}

@Composable
fun FeedView(model: FeedViewModel) {
    val state by model.state.collectAsState()
    PaginationList {
        pagedItems(
            params = PagedItemsParams(
                state = state,
                key = { post -> post.id },
                onFetch = model::onFetch,
                shimmer = { PostShimmer() },
                appendErrorContent = { _, retry -> RetryFooter(onRetry = retry) },
            ),
        ) { post ->
            PostRow(post)
        }
    }
}
```

`PaginationList` resolves its renderer from `LocalPaginationListRenderer`, which the app installs
once at its composition root — with the Metro wiring in place, as part of the graph's collected
`Set<ProvidedValue<*>>`. Without the renderer installed the list draws nothing rather than crashing
— except in a `@Preview`, where the default renderer draws the `PagingState` it is handed, so a
feature module previews a real-looking list without depending on `impl`.

The full setup — graph, root, ViewModels, wiring without a DI framework — is in
[`paging/api/README.md`](paging/api/README.md#installing), and running in
[`sample/`](sample/README.md#set-paging-kit-up-in-your-own-app).

## Static analysis

Detekt runs as part of `check`, configured once in `config/detekt/detekt.yml`, and **findings fail
the build** — the repository starts with none, and a warning nobody has to clear is a rule that
decays. A genuinely wrong finding is silenced in the config or with `@Suppress`, where the decision
is visible in review.

Alongside the standard rules and the ktlint wrapper, `build-logic/detekt-rules` ships six rules of
its own, in the `project` rule set:

| Rule | Enforces |
|---|---|
| `LayerPackageRequired` | every file of an `api`/`impl` module lives in its `data`, `domain` or `presentation` package |
| `LayerPackageBoundary` | inside one module, `domain` imports neither of the other two, and `data`/`presentation` never import each other |
| `UnsafeCollectionIndexAccess` | `list[i]` / `list.get(i)` give way to `getOrNull(i)` |
| `ExpressionBodyNotAllowed` | function bodies are `{ … }`, not `= …` |
| `MultilineConstructorRequired` | a primary constructor with 2+ parameters puts each on its own line |
| `PreviewMustBePrivate` | a `@Preview` function never widens a module's public API |

The two layer rules are what make the `api`/`impl`/`wiring` split checkable rather than a
convention: they read the package name, so they apply to any module whose root package ends in
`api` or `impl`, and skip `wiring` — a binding container is none of the three layers.

Their tests live in the included build, so they are not picked up by a module's `test` task;
`./gradlew detektRulesTest` runs them, and the root `test` task depends on it.
