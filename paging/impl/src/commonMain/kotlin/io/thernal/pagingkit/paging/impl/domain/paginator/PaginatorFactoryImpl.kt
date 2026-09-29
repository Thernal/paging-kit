package io.thernal.pagingkit.paging.impl.domain.paginator

import io.thernal.pagingkit.paging.api.domain.loader.PageLoader
import io.thernal.pagingkit.paging.api.domain.paginator.Paginator
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory

class PaginatorFactoryImpl : PaginatorFactory {
    override fun <T> create(
        loader: PageLoader<T>,
        identity: (T) -> Any,
    ): Paginator<T> {
        return PaginatorImpl(loader = loader, identity = identity)
    }
}
