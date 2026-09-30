plugins {
    alias(libs.plugins.pagingkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            // Coroutines and the immutable collections both appear in this module's public
            // signatures (`Flow<PagingState<T>>`, `ImmutableList<T>`), yet neither is re-exported:
            // `api(...)` is not used in this repository, so a consumer declares both itself
            // (paging/api/README.md → Dependencies you declare).
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
            }
        }
    }
}
