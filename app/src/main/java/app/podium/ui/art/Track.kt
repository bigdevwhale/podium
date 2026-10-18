package app.podium.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.podium.domain.Medal
import app.podium.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The shape of a 400 m track seen from above: two straights and two bends. */
private class Oval(val cx: Float, val cy: Float, val half: Float, val r: Float) {
    private val straight = 2 * half

    /**
     * A point [p] laps round, counter-clockwise as races are run, from the start line a quarter of
     * the way down the home straight; [lane] moves it out from the inside edge.
     */
    fun at(p: Float, lane: Float): Offset {
        val rr = r + lane
        val total = 2 * straight + 2 * PI.toFloat() * r
        var s = ((p % 1f + 1f) % 1f) * total + straight * 0.25f
        if (s >= total) s -= total
        val x0 = cx - half
        val x1 = cx + half
        val bend = PI.toFloat() * r
        return when {
            s < straight -> Offset(x0 + s, cy + rr)
            s < straight + bend -> {
                val a = (s - straight) / r
                Offset(x1 + rr * sin(a), cy + rr * cos(a))
            }
            s < 2 * straight + bend -> Offset(x1 - (s - straight - bend), cy - rr)
            else -> {
                val a = (s - 2 * straight - bend) / r
                Offset(x0 - rr * sin(a), cy - rr * cos(a))
            }
        }
    }
}

/**
 * The stadium during a heat: the ghost in the inside lane, you outside it, a tick at every split and
 * the finish line. [you] can be past 1 in a marathon: the laps go on.
 */
@Composable
fun Stadium(ghostAt: Float?, youAt: Float, sections: Int, modifier: Modifier = Modifier, you: Color = U.c.accent, ghost: Color = U.c.ghost) {
    val track = U.c.track
    val grass = U.c.grass
    val line = Color.White
    val ink = U.c.ink
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val band = min(w, h) * 0.13f
        val r = min(h / 2f - band - 4f, w * 0.22f)
        val half = max(0f, w / 2f - r - band - 4f)
        val oval = Oval(w / 2f, h / 2f, half, r)
        // The track: one wide band round the grass.
        val outer = r + band
        drawRoundRect(track, Offset(w / 2 - half - outer, h / 2 - outer), Size(2 * (half + outer), 2 * outer), CornerRadius(outer, outer))
        drawRoundRect(grass, Offset(w / 2 - half - r, h / 2 - r), Size(2 * (half + r), 2 * r), CornerRadius(r, r))
        // Lane lines.
        for (i in 1..2) {
            val lr = r + band * i / 3f
            drawRoundRect(line.copy(alpha = 0.55f), Offset(w / 2 - half - lr, h / 2 - lr), Size(2 * (half + lr), 2 * lr), CornerRadius(lr, lr), style = Stroke(1.2f))
        }
        // The finish line and the splits.
        val n = sections.coerceAtLeast(1)
        for (i in 0 until n) {
            val a = oval.at(i.toFloat() / n, 0f)
            val b = oval.at(i.toFloat() / n, band)
            drawLine(if (i == 0) line else line.copy(alpha = 0.8f), a, b, strokeWidth = if (i == 0) 4f else 2f, pathEffect = if (i == 0) null else PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
        }
        val dot = band * 0.3f
        if (ghostAt != null) {
            val g = oval.at(ghostAt, band / 3f * 0.5f)
            drawCircle(ghost.copy(alpha = 0.55f), dot, g)
            drawCircle(Color.White.copy(alpha = 0.7f), dot, g, style = Stroke(1.6f))
        }
        val y = oval.at(youAt, band / 3f * 1.5f)
        drawCircle(Color.White, dot + 2.5f, y)
        drawCircle(you, dot, y)
        // Grass gets a quiet centre line, like the infield at the Olympiastadion.
        drawLine(ink.copy(alpha = 0.06f), Offset(w / 2 - half, h / 2), Offset(w / 2 + half, h / 2), strokeWidth = 2f)
    }
}

/** A medal on its ribbon. */
fun DrawScope.medal(m: Medal, color: Color, c: Offset, r: Float) {
    val ribbon = Path().apply {
        moveTo(c.x - r * 0.75f, c.y - r * 2.1f)
        lineTo(c.x - r * 0.15f, c.y - r * 2.1f)
        lineTo(c.x + r * 0.3f, c.y - r * 0.6f)
        lineTo(c.x - r * 0.2f, c.y - r * 0.6f)
        close()
        moveTo(c.x + r * 0.75f, c.y - r * 2.1f)
        lineTo(c.x + r * 0.15f, c.y - r * 2.1f)
        lineTo(c.x - r * 0.3f, c.y - r * 0.6f)
        lineTo(c.x + r * 0.2f, c.y - r * 0.6f)
        close()
    }
    drawPath(ribbon, ribbonColor(m))
    drawCircle(color, r, c)
    drawCircle(Color.White.copy(alpha = 0.45f), r * 0.72f, c, style = Stroke(r * 0.09f))
}

private fun ribbonColor(m: Medal) = when (m) {
    Medal.GOLD -> Color(0xFF2B92D4)
    Medal.SILVER -> Color(0xFF3C9D50)
    Medal.BRONZE -> Color(0xFFEA8424)
}

@Composable
fun MedalMark(m: Medal, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = min(size.width, size.height / 1.55f) / 2f
        medal(m, color, Offset(size.width / 2, size.height - r), r)
    }
}

/**
 * A discipline's heats over time: each heat a dot (hollow when rushed), the record as a step line,
 * the standard as a dashed one. Better is always up.
 */
@Composable
fun Chart(points: List<Pair<Long, Boolean>>, better: (Long, Long) -> Boolean, standard: Long?, modifier: Modifier = Modifier) {
    val ink = U.c.accent
    val faint = U.c.line
    val gold = U.c.gold
    val muted = U.c.faint
    Canvas(modifier) {
        if (points.isEmpty()) return@Canvas
        val vals = points.map { it.first }
        val lo = vals.min()
        val hi = vals.max()
        val span = (hi - lo).coerceAtLeast(1)
        val pad = 10f
        val upIsLow = better(lo, hi)
        fun y(v: Long): Float {
            val f = (v - lo).toFloat() / span
            val t = if (upIsLow) f else 1 - f
            return pad + t * (size.height - 2 * pad)
        }
        fun x(i: Int) = if (points.size == 1) size.width / 2 else pad + i * (size.width - 2 * pad) / (points.size - 1)
        for (k in 0..3) {
            val yy = pad + k * (size.height - 2 * pad) / 3
            drawLine(faint, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 1f)
        }
        if (standard != null && standard in lo..hi) {
            drawLine(muted, Offset(0f, y(standard)), Offset(size.width, y(standard)), strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        }
        // The record so far, stepping only when it's beaten.
        var best: Long? = null
        val steps = Path()
        points.forEachIndexed { i, (v, counts) ->
            if (!counts) return@forEachIndexed
            val b = best
            if (b == null) {
                best = v
                steps.moveTo(x(i), y(v))
            } else if (better(v, b)) {
                steps.lineTo(x(i), y(b))
                steps.lineTo(x(i), y(v))
                best = v
            }
        }
        best?.let { steps.lineTo(size.width - pad, y(it)) }
        drawPath(steps, gold, style = Stroke(2.5f, cap = StrokeCap.Round))
        val line = Path()
        points.forEachIndexed { i, (v, _) -> if (i == 0) line.moveTo(x(i), y(v)) else line.lineTo(x(i), y(v)) }
        drawPath(line, ink.copy(alpha = 0.35f), style = Stroke(1.5f))
        points.forEachIndexed { i, (v, counts) ->
            if (counts) drawCircle(ink, 4f, Offset(x(i), y(v)))
            else drawCircle(ink, 4f, Offset(x(i), y(v)), style = Stroke(1.5f))
        }
    }
}
