plugins {
    alias(libs.plugins.pagingkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            // api, not implementation: coroutines and the immutable collections both appear in this
            // module's own public signatures (`Flow<PagingState<T>>`, `ImmutableList<T>`), so a
            // consumer cannot compile against it without them.
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.collections.immutable)
            }
        }
    }
}
