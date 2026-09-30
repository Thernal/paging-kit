plugins {
    alias(libs.plugins.pagingkit.compose)
    alias(libs.plugins.pagingkit.injection)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // The contracts are not re-exported: an app that injects them depends on `api` itself.
                implementation(projects.paging.api)
                implementation(projects.paging.impl)
            }
        }
    }
}
