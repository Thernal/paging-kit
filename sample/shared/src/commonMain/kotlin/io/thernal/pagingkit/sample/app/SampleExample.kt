package io.thernal.pagingkit.sample.app

import androidx.compose.runtime.Composable

/** Whether an example is the smallest thing that works, or the shape a real screen would have. */
enum class ExampleKind(val label: String) {
    SIMPLE("simple"),
    REAL_LIFE("real life"),
}

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
