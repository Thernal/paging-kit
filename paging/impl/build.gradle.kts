plugins {
    alias(libs.plugins.pagingkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.animation)
                implementation(projects.paging.api)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
            }
        }
    }
}
