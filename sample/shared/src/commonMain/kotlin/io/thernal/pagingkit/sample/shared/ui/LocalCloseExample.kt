package io.thernal.pagingkit.sample.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Closes the open example; installed by the root, read by [ExampleScaffold]'s back button. */
val LocalCloseExample = staticCompositionLocalOf<() -> Unit> { {} }
