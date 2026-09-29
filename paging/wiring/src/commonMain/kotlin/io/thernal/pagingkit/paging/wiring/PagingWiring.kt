package io.thernal.pagingkit.paging.wiring

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.paging.api.presentation.components.LocalPaginationFlowRowRenderer
import io.thernal.pagingkit.paging.api.presentation.components.LocalPaginationListRenderer
import io.thernal.pagingkit.paging.api.presentation.components.PaginationFlowRowRenderer
import io.thernal.pagingkit.paging.api.presentation.components.PaginationListRenderer
import io.thernal.pagingkit.paging.impl.domain.paginator.PaginatorFactoryImpl
import io.thernal.pagingkit.paging.impl.presentation.components.PaginationFlowRowRendererImpl
import io.thernal.pagingkit.paging.impl.presentation.components.PaginationListRendererImpl

/**
 * Binds the paging capability into an application graph — a worked example rather than a fixed part
 * of the kit, since `api` and `impl` name no injection framework. `Paginator` is deliberately **not**
 * bound: it is stateful and private to its owner, which builds one from [PaginatorFactory].
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface PagingWiring {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun providePaginatorFactory(): PaginatorFactory {
            return PaginatorFactoryImpl()
        }

        @Provides
        @SingleIn(AppScope::class)
        fun providePaginationListRenderer(): PaginationListRenderer {
            return PaginationListRendererImpl()
        }

        @Provides
        @SingleIn(AppScope::class)
        fun providePaginationFlowRowRenderer(): PaginationFlowRowRenderer {
            return PaginationFlowRowRendererImpl()
        }

        /**
         * Contributed into the graph's `Set<ProvidedValue<*>>`, so a composition root installs every
         * feature's composition locals in one `CompositionLocalProvider` without naming any of them.
         */
        @Provides
        @IntoSet
        fun providePaginationListRendererValue(renderer: PaginationListRenderer): ProvidedValue<*> {
            return LocalPaginationListRenderer provides renderer
        }

        @Provides
        @IntoSet
        fun providePaginationFlowRowRendererValue(renderer: PaginationFlowRowRenderer): ProvidedValue<*> {
            return LocalPaginationFlowRowRenderer provides renderer
        }
    }
}
