package io.github.maniramezan.testcommons.compose

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Captures the node's pixels as PNG bytes, for attaching to a failure or comparing with a
 * reference outside a snapshot library.
 *
 * @return The PNG-encoded image.
 */
public fun SemanticsNodeInteraction.captureToPng(): ByteArray {
    val output = ByteArrayOutputStream()
    check(ImageIO.write(captureToImage().toAwtImage(), "png", output)) { "No PNG writer is available" }
    return output.toByteArray()
}
