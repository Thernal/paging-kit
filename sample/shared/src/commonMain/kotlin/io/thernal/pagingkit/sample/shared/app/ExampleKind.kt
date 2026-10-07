package io.thernal.pagingkit.sample.shared.app

/**
 * Whether an example is the smallest thing that works, or the shape a real screen would have. Declared in
 * catalog order: the simple example of a group comes before its real-life sibling.
 */
enum class ExampleKind(val label: String) {
    SIMPLE("simple"),
    REAL_LIFE("real life"),
}
