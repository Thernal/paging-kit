package io.thernal.pagingkit.sample.shared.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.thernal.pagingkit.paging.api.presentation.theme.PagingTheme
import io.thernal.pagingkit.sample.designsystem.SampleTheme
import io.thernal.pagingkit.sample.designsystem.paging.samplePagingStyles
import io.thernal.pagingkit.sample.shared.catalog.CatalogScreen
import io.thernal.pagingkit.sample.shared.ui.LocalCloseExample
import kotlinx.collections.immutable.toImmutableList

/**
 * The composition root.
 *
 * The one line every application using the kit has is the first: the graph's composition locals —
 * the list and flow-row renderers `PagingProvidersModule` contributed — installed in one spread. Without it
 * every `PaginationList` draws nothing.
 *
 * The rest is the sample's own catalog: an index, and the example it opens.
 */
@Composable
fun SampleApp(graph: SampleGraph) {
    CompositionLocalProvider(
        values = graph.providedValues.toTypedArray(),
    ) {
        SampleTheme {
            PagingTheme(styles = samplePagingStyles()) {
                val root: RootViewModel = viewModel { RootViewModel() }
                val openExampleId by root.openExampleId.collectAsState()
                val examples = remember(graph) {
                    graph.examples
                        .sortedWith(compareBy<SampleExample> { it.group }.thenBy { it.kind.ordinal })
                        .toImmutableList()
                }
                val open = examples.firstOrNull { example -> example.id == openExampleId }

                if (open == null) {
                    CatalogScreen(examples = examples, onOpen = { example -> root.onOpen(example.id) })
                } else {
                    NavigationBackHandler(
                        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                        onBackCompleted = root::onClose,
                    )
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides root.exampleStoreOwner,
                        LocalCloseExample provides root::onClose,
                    ) {
                        open.content()
                    }
                }
            }
        }
    }
}
