plugins {
    alias(libs.plugins.pagingkit.compose)
    alias(libs.plugins.pagingkit.injection)
}

kotlin {
    // One static framework per iOS target, embedded by the Xcode project in `sample/iosApp`.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SampleShared"
            isStatic = true
            // Kotlin/Native cannot infer one from the source packages, and says so on every link.
            binaryOption("bundleId", "io.thernal.pagingkit.sample.shared")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // The sample is a consumer, so it names the three modules an application names:
                // the contracts, the implementation that backs them, and the bindings that install
                // both into a graph.
                implementation(projects.paging.api)
                implementation(projects.paging.impl)
                implementation(projects.paging.wiring)
                // PagingPreviewParameterProvider, for the previews next to each screen.
                implementation(projects.paging.preview)
                implementation(projects.sample.designsystem)
                // `@Preview` and the provider's supertype; the preview module re-exports nothing.
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                // The kit names no design system; the sample needs one to be worth running, and
                // Material 3 is it.
                implementation(libs.compose.material3)
                // Every paginator in the sample is owned by a ViewModel, which is what the kit
                // expects of a caller.
                implementation(libs.lifecycle.viewmodel.compose)
                // System back closes an open example.
                implementation(libs.navigationevent.compose)
            }
        }
    }
}
