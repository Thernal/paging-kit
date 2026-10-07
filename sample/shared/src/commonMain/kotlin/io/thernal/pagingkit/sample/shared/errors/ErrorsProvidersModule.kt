package io.thernal.pagingkit.sample.errors

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.app.ExampleKind
import io.thernal.pagingkit.sample.app.SampleExample

private const val GROUP = "Errors and resets"

@BindingContainer
@ContributesTo(AppScope::class)
interface ErrorsBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideFailuresExample(paginatorFactory: PaginatorFactory): SampleExample {
            return SampleExample(
                id = "errors.failures",
                group = GROUP,
                kind = ExampleKind.SIMPLE,
                title = "Failures and retry",
                summary = "PagingState.Error versus AppendStatus.Failed, errorContent and appendErrorContent.",
                content = { FailuresScreen(paginatorFactory) },
            )
        }

        @Provides
        @IntoSet
        fun provideSearchExample(paginatorFactory: PaginatorFactory): SampleExample {
            return SampleExample(
                id = "errors.search",
                group = GROUP,
                kind = ExampleKind.REAL_LIFE,
                title = "Search as you type",
                summary = "One paginator, a loader that reads the query, reset on every change.",
                content = { SearchScreen(paginatorFactory) },
            )
        }
    }
}
