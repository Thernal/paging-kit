package io.thernal.pagingkit.sample.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The frame every example screen sits in: a title, a line saying what to look at, and whatever the
 * example is. Unlike a form, a paged list scrolls itself, so [content] is given the remaining
 * height rather than a scrolling column.
 */
@Composable
fun ExampleScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val close = LocalCloseExample.current
    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = close) { Text(text = "‹ Back") }
            }
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                content = content,
            )
        }
    }
}
