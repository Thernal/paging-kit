package io.thernal.pagingkit.sample.android

import android.app.Application
import io.thernal.pagingkit.sample.app.SampleGraph
import io.thernal.pagingkit.sample.app.createSampleGraph

/**
 * Owns the application graph for the life of the process. An activity recreated for a rotation
 * starts a new composition; a graph remembered there would be rebuilt with it.
 */
class SampleApplication : Application() {
    val graph: SampleGraph by lazy { createSampleGraph() }
}
