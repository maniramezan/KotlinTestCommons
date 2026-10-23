package io.github.maniramezan.testcommons.compose

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import java.io.ByteArrayOutputStream

/**
 * Captures the node's pixels as PNG bytes, for attaching to a failure or comparing with a
 * reference outside a snapshot library.
 *
 * Under Robolectric, set `robolectric.pixelCopyRenderMode=hardware` and use native graphics.
 *
 * @return The PNG-encoded image.
 */
public fun SemanticsNodeInteraction.captureToPng(): ByteArray {
    val output = ByteArrayOutputStream()
    captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
    return output.toByteArray()
}
