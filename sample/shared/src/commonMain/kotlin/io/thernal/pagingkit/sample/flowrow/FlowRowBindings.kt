package io.thernal.pagingkit.sample.flowrow

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.app.ExampleKind
import io.thernal.pagingkit.sample.app.SampleExample

@BindingContainer
@ContributesTo(AppScope::class)
interface FlowRowBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideTagCloudExample(paginatorFactory: PaginatorFactory): SampleExample {
            return SampleExample(
                id = "flowrow.tags",
                group = "Flow row",
                kind = ExampleKind.SIMPLE,
                title = "Tag cloud",
                summary = "PaginationFlowRow: wrapping chips, a shimmer per chip, prefetch by distance.",
                content = { TagCloudScreen(paginatorFactory) },
            )
        }
    }
}
