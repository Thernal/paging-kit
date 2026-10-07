package io.thernal.pagingkit.sample.shared.app

import androidx.compose.runtime.Composable

/**
 * One entry on the catalog screen.
 *
 * Examples register themselves into the graph, so the catalog imports none of them and adding an
 * example is adding a file. [id] is what the root remembers about the open example, so it has to
 * be unique and stable.
 */
class SampleExample(
    val id: String,
    val group: String,
    val kind: ExampleKind,
    val title: String,
    val summary: String,
    val content: @Composable () -> Unit,
)
