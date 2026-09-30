plugins {
    alias(libs.plugins.pagingkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // `PagingState` and `PreviewParameterProvider` are both in this module's signatures;
                // `api(...)` is not used in this repository, so a consumer declares both itself.
                implementation(projects.paging.api)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.kotlinx.collections.immutable)
            }
        }
    }
}
