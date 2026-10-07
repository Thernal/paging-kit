package io.thernal.pagingkit.sample.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.thernal.pagingkit.sample.shared.app.SampleApp

/**
 * One activity, one composition. Everything a reader is here to look at lives in the shared module
 * and runs unchanged on iOS.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as SampleApplication).graph
        setContent {
            SampleApp(graph)
        }
    }
}
