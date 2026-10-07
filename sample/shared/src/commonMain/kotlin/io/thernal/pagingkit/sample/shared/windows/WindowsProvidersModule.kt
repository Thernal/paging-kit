package io.thernal.pagingkit.sample.windows

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.app.ExampleKind
import io.thernal.pagingkit.sample.app.SampleExample

private const val GROUP = "Windows"

@BindingContainer
@ContributesTo(AppScope::class)
interface WindowsBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideTwoWindowsExample(paginatorFactory: PaginatorFactory): SampleExample {
            return SampleExample(
                id = "windows.two",
                group = GROUP,
                kind = ExampleKind.SIMPLE,
                title = "Two windows",
                summary = "Two paginators in one list, plain items between them, ifLoaded for a header.",
                content = { TwoWindowsScreen(paginatorFactory) },
            )
        }

        @Provides
        @IntoSet
        fun provideContactsExample(paginatorFactory: PaginatorFactory): SampleExample {
            return SampleExample(
                id = "windows.contacts",
                group = GROUP,
                kind = ExampleKind.REAL_LIFE,
                title = "Contacts",
                summary = "pagedItemsGrouped with sticky letter headers over pages that split a letter.",
                content = { ContactsScreen(paginatorFactory) },
            )
        }
    }
}
