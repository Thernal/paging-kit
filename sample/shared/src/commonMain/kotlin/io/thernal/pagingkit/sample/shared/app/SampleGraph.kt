package io.thernal.pagingkit.sample.shared.app

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

/**
 * The application graph. Nothing here names a feature or the kit: composition locals arrive in one
 * set — the two renderers `PagingProvidersModule` contributes — and examples in another, so adding either
 * never changes this file. The `PaginatorFactory` each example needs is injected into that
 * example's binding, not asked for here.
 */
@DependencyGraph(AppScope::class)
interface SampleGraph {
    val providedValues: Set<ProvidedValue<*>>

    val examples: Set<SampleExample>
}

fun createSampleGraph(): SampleGraph {
    return createGraph<SampleGraph>()
}
