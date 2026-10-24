package io.github.maniramezan.testcommons.samples.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A small, offline catalog demonstrating stable automation IDs and testable Compose content. */
public class MainActivity : ComponentActivity() {
    /**
     * Creates the catalog and its screen-scoped asynchronous loader.
     *
     * @param savedInstanceState State supplied by Android after recreation, or `null` on first launch.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent {
            val model = remember {
                CatalogModel {
                    delay(300)
                    List(40) { "Lesson ${it + 1}" }
                }
            }
            val state by model.state.collectAsState()
            val scope = rememberCoroutineScope()
            LaunchedEffect(model) { model.load() }
            SampleTheme {
                CatalogScreen(state, onReload = { scope.launch { model.load() } })
            }
        }
    }
}
