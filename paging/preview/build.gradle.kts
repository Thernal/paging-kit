plugins {
    alias(libs.plugins.pagingkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // api: `PagingState` and `PreviewParameterProvider` are both in this module's own
                // signatures, and a consumer extending the provider compiles against both.
                api(projects.paging.api)
                api(libs.compose.ui.tooling.preview)
            }
        }
    }
}
