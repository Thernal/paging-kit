package io.thernal.pagingkit.sample.lists

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.app.ExampleKind
import io.thernal.pagingkit.sample.app.SampleExample

private const val GROUP = "Lists"

/**
 * The factory is injected here and handed to the screen, which hands it to its ViewModel — the
 * same path a feature's `@Inject` ViewModel constructor takes in an application.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface ListsBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideNumbersExample(paginatorFactory: PaginatorFactory): SampleExample {
            return SampleExample(
                id = "lists.numbers",
                group = GROUP,
                kind = ExampleKind.SIMPLE,
                title = "Numbers",
                summary = "One paged window: a paginator in a ViewModel, pagedItems in a PaginationList.",
                content = { NumbersScreen(paginatorFactory) },
            )
        }

        @Provides
        @IntoSet
        fun provideFeedExample(paginatorFactory: PaginatorFactory): SampleExample {
            return SampleExample(
                id = "lists.feed",
                group = GROUP,
                kind = ExampleKind.REAL_LIFE,
                title = "Post feed",
                summary = "Pull to refresh, local prepend and remove, retry footers, scroll to top.",
                content = { FeedScreen(paginatorFactory) },
            )
        }
    }
}
