plugins {
    alias(libs.plugins.pagingkit.compose)
    alias(libs.plugins.pagingkit.injection)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // api: the contracts an app injects are this module's whole point.
                api(projects.paging.api)
                // implementation: which concrete class satisfies a contract is nobody else's business.
                implementation(projects.paging.impl)
            }
        }
    }
}
