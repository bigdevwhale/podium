package app.podium

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.podium.ui.art.Pictograms
import app.podium.ui.art.picto
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws every pictogram on one sheet (run with `-Prender`): a look at the set without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    @Test
    fun pictograms() {
        val cell = 160f
        val cols = 6
        val rows = (Pictograms.all.size + cols - 1) / cols
        val image = ImageBitmap((cell * cols).toInt(), (cell * rows).toInt())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(cell * cols, cell * rows)) {
            drawRect(Color.White)
            Pictograms.all.forEachIndexed { i, p ->
                val at = Offset((i % cols) * cell + 12, (i / cols) * cell + 12)
                drawRoundRect(Color(0xFF1C88CC), at, Size(cell - 24, cell - 24), CornerRadius(26f, 26f))
                picto(p, Color.White, at + Offset(18f, 18f), cell - 60)
            }
        }
        File(out, "pictograms.png").outputStream().use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
