package io.thernal.pagingkit.sample.shared.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/** The one multibinding the sample itself declares; the kit's own bindings are in `PagingProvidersModule`. */
@BindingContainer
@ContributesTo(AppScope::class)
interface SampleProvidersModule {
    /** Each example contributes its catalog entry here, so the index screen imports none of them. */
    @Multibinds(allowEmpty = true)
    val examples: Set<SampleExample>
}
