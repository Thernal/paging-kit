plugins {
    `kotlin-dsl`
}

group = "io.thernal.pagingkit.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

// compileOnly throughout: the plugins themselves are put on the consuming build's classpath by the
// root `build.gradle.kts`, which declares each one `apply false`. These entries only supply the
// Gradle DSL types the conventions below configure.
dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    // implementation, not compileOnly: unlike the others, the Detekt plugin is applied by a
    // convention rather than declared in the root build, so it has to travel with build-logic.
    implementation(libs.detekt.gradle.plugin)
    testImplementation(libs.junit4)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "io.thernal.pagingkit.kmp.library"
            implementationClass = "io.thernal.pagingkit.buildlogic.KmpLibraryConventionPlugin"
        }
        register("compose") {
            id = "io.thernal.pagingkit.compose"
            implementationClass = "io.thernal.pagingkit.buildlogic.ComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "io.thernal.pagingkit.android.application"
            implementationClass = "io.thernal.pagingkit.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("injection") {
            id = "io.thernal.pagingkit.injection"
            implementationClass = "io.thernal.pagingkit.buildlogic.InjectionConventionPlugin"
        }
    }
}
