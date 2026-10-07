// An app's design system, small: the tokens the sample adds to Material 3, and the one file that maps
// them onto paging-kit's styles. It is the sample's, never copied into an app — an app maps its own.
plugins {
    alias(libs.plugins.pagingkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.paging.api)
                implementation(libs.compose.material3)
            }
        }
    }
}
