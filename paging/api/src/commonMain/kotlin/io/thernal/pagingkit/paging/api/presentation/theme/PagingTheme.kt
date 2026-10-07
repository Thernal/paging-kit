package io.thernal.pagingkit.paging.api.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPagingStyles = staticCompositionLocalOf { PagingStyles() }

/** Installs [styles] for every paginated list below — once, at the root, inside the app's own theme. */
@Composable
fun PagingTheme(
    styles: PagingStyles,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPagingStyles provides styles) {
        content()
    }
}

object PagingTheme {
    val styles: PagingStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalPagingStyles.current
}
