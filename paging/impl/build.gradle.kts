plugins {
    alias(libs.plugins.pagingkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.paging.api)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
            }
        }
    }
}
