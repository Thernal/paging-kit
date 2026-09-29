package io.thernal.pagingkit.paging.api.domain.paginator

import io.thernal.pagingkit.paging.api.domain.loader.PageLoader

/**
 * Builds a fresh [Paginator] per call. A paginator is stateful (a page number, a generation, its own
 * state flow) and generic per use, so it is never injected or scoped itself — inject this factory
 * and call [create] once per owner, typically a ViewModel, keeping the result private.
 *
 * @param identity what makes two items the same item: pages are de-duplicated by it, and
 *   `prepend`/`insertAt`/`remove` find items by it.
 */
interface PaginatorFactory {
    fun <T> create(
        loader: PageLoader<T>,
        identity: (T) -> Any,
    ): Paginator<T>
}
