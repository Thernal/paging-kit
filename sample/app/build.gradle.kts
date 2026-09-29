plugins {
    alias(libs.plugins.pagingkit.android.application)
}

dependencies {
    implementation(projects.sample.shared)
    // `setContent` is the only thing this module needs that the shared module does not already
    // expose: everything on screen is Compose Multiplatform, hosted in one activity.
    implementation(libs.androidx.activity.compose)
}
