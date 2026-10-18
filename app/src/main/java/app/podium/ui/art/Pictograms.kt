package app.podium.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A shape a pictogram holds or stands at, on the same 24-unit grid as the figure. */
sealed interface Prop {
    data class Line(val points: List<Offset>, val width: Float = 1.5f) : Prop
    data class Fill(val points: List<Offset>) : Prop
    data class Ring(val c: Offset, val r: Float, val width: Float = 1.4f) : Prop
    data class Dot(val c: Offset, val r: Float) : Prop
}

/**
 * A figure in the manner of Otl Aicher's Munich pictograms: a round head, limbs as thick bars at
 * steady angles, nothing else. [limbs] are polylines on a 24×24 grid, y down; a sign without
 * a figure (the car) has no [head].
 */
class Picto(val key: String, val head: Offset?, val limbs: List<List<Offset>>, val props: List<Prop> = emptyList())

private fun o(x: Number, y: Number) = Offset(x.toFloat(), y.toFloat())
private fun line(p: List<Offset>, w: Float = 1.5f) = Prop.Line(p, w)
private fun fill(p: List<Offset>) = Prop.Fill(p)
private fun rect(x0: Number, y0: Number, x1: Number, y1: Number) = fill(listOf(o(x0, y0), o(x1, y0), o(x1, y1), o(x0, y1)))

object Pictograms {
    /** In the order they're stored: an index never changes meaning. */
    val all: List<Picto> = listOf(
        Picto(
            "run", o(14.5, 3.4),
            listOf(
                listOf(o(13.6, 7.2), o(10.6, 13)),
                listOf(o(10.6, 13), o(15, 15.2), o(14, 20.8)),
                listOf(o(10.6, 13), o(7.8, 16.8), o(3.8, 17.6)),
                listOf(o(13.2, 8), o(16.4, 10), o(18.8, 7.8)),
                listOf(o(12.8, 8.4), o(9.6, 9.6), o(7.8, 12.4)),
            ),
        ),
        Picto(
            "walk", o(12.4, 3.4),
            listOf(
                listOf(o(12.2, 7.2), o(11.6, 13.4)),
                listOf(o(11.6, 13.4), o(14.6, 21)),
                listOf(o(11.6, 13.4), o(8.4, 21)),
                listOf(o(12.1, 8.2), o(14.8, 12.8)),
                listOf(o(12.1, 8.2), o(9.2, 12.6)),
            ),
        ),
        Picto(
            "bike", o(14, 4.2),
            listOf(
                listOf(o(13, 7.6), o(9.6, 12.4)),
                listOf(o(13, 8.2), o(16.4, 11.2)),
                listOf(o(9.6, 12.4), o(13.2, 14.6), o(12.2, 18.4)),
            ),
            listOf(
                Prop.Ring(o(6, 18), 3.6f, 1.5f),
                Prop.Ring(o(18.4, 18), 3.6f, 1.5f),
                line(listOf(o(6, 18), o(9.4, 13.2), o(15.6, 13.2), o(18.4, 18))),
                line(listOf(o(9.4, 13.2), o(12.2, 18.2), o(15.6, 13.2))),
                line(listOf(o(16.4, 11.2), o(15.6, 13.2))),
            ),
        ),
        Picto(
            "swim", o(5.6, 11.6),
            listOf(
                listOf(o(8.4, 12.6), o(16, 13.8)),
                listOf(o(8.8, 12.2), o(7, 8.2), o(3.2, 8.8)),
                listOf(o(16, 13.8), o(21, 12.6)),
                listOf(o(16, 13.8), o(20.6, 15.6)),
            ),
            listOf(
                line(listOf(o(2, 18), o(4.5, 16.8), o(7, 18), o(9.5, 16.8), o(12, 18), o(14.5, 16.8), o(17, 18), o(19.5, 16.8), o(22, 18)), w = 1.4f),
                line(listOf(o(2, 21.2), o(4.5, 20), o(7, 21.2), o(9.5, 20), o(12, 21.2), o(14.5, 20), o(17, 21.2), o(19.5, 20), o(22, 21.2)), w = 1.4f),
            ),
        ),
        Picto(
            "lift", o(12, 6.4),
            listOf(
                listOf(o(12, 9.8), o(12, 15.4)),
                listOf(o(12, 15.4), o(9, 21.4)),
                listOf(o(12, 15.4), o(15, 21.4)),
                listOf(o(12, 10.4), o(8, 8.4), o(8, 3.6)),
                listOf(o(12, 10.4), o(16, 8.4), o(16, 3.6)),
            ),
            listOf(line(listOf(o(4.6, 3), o(19.4, 3)), w = 1.5f), rect(3.8, 1.2, 5.6, 4.8), rect(18.4, 1.2, 20.2, 4.8)),
        ),
        Picto(
            "plank", o(4.4, 12.2),
            listOf(
                listOf(o(7.4, 14), o(20.6, 19.2)),
                listOf(o(7.2, 14.4), o(7.2, 19.4), o(10.8, 19.4)),
            ),
            listOf(line(listOf(o(2, 21.4), o(22, 21.4)), w = 1.3f)),
        ),
        Picto(
            "stretch", o(12, 3.4),
            listOf(
                listOf(o(12, 7.2), o(12, 13.2)),
                listOf(o(12, 8.6), o(4.2, 8.6)),
                listOf(o(12, 8.6), o(19.8, 8.6)),
                listOf(o(12, 13.2), o(17, 15.8), o(17, 21.4)),
                listOf(o(12, 13.2), o(6.2, 21.4)),
            ),
        ),
        Picto(
            "meditate", o(12, 4.8),
            listOf(
                listOf(o(12, 8.4), o(12, 14.8)),
                listOf(o(12, 9.4), o(8.2, 13.2), o(7.4, 16.8)),
                listOf(o(12, 9.4), o(15.8, 13.2), o(16.6, 16.8)),
                listOf(o(12, 15.4), o(5.4, 18.4), o(12.6, 19.6)),
                listOf(o(12, 15.4), o(18.6, 18.4), o(11.4, 19.6)),
            ),
        ),
        Picto(
            "read", o(10.2, 3.8),
            listOf(
                listOf(o(10, 7.4), o(9.2, 14.2)),
                listOf(o(9.2, 14.2), o(15.2, 14.2), o(15.2, 21.2)),
                listOf(o(10, 8.6), o(12.8, 12), o(14.4, 10.2)),
            ),
            listOf(
                line(listOf(o(11.8, 8.2), o(14.4, 10), o(17.4, 8.2)), w = 1.6f),
                line(listOf(o(5.8, 8.6), o(5.8, 21.4)), w = 1.4f),
                line(listOf(o(5.8, 16.4), o(13, 16.4)), w = 1.4f),
                line(listOf(o(12.6, 16.4), o(12.6, 21.4)), w = 1.4f),
            ),
        ),
        Picto(
            "desk", o(7.8, 4.6),
            listOf(
                listOf(o(7.8, 8.2), o(7.4, 15)),
                listOf(o(7.4, 15), o(12.8, 15), o(12.8, 21.4)),
                listOf(o(7.8, 9.4), o(10.8, 12.2), o(14.2, 11.4)),
            ),
            listOf(
                line(listOf(o(11.4, 13), o(22, 13)), w = 1.6f),
                line(listOf(o(20.6, 13), o(20.6, 21.4)), w = 1.4f),
                line(listOf(o(14, 12.2), o(18.4, 12.2), o(20, 7)), w = 1.5f),
            ),
        ),
        Picto(
            "sweep", o(10, 3.4),
            listOf(
                listOf(o(10, 7), o(10.4, 13.4)),
                listOf(o(10.4, 13.4), o(8, 21.4)),
                listOf(o(10.4, 13.4), o(13.4, 21.4)),
                listOf(o(10.2, 8), o(13.1, 10.9)),
                listOf(o(10.2, 8.6), o(11.4, 12.6), o(14.4, 13.2)),
            ),
            listOf(
                line(listOf(o(11.6, 8.2), o(17.8, 19.2)), w = 1.3f),
                fill(listOf(o(15.4, 19.4), o(19.4, 17.6), o(22, 21.6), o(16.4, 21.8))),
            ),
        ),
        Picto(
            "cook", o(8, 3.4),
            listOf(
                listOf(o(8, 7), o(8, 13.4)),
                listOf(o(8, 13.4), o(6, 21.4)),
                listOf(o(8, 13.4), o(10.4, 21.4)),
                listOf(o(8, 8), o(10.4, 10.6), o(12.2, 11)),
            ),
            listOf(
                rect(13, 14.4, 21.6, 21.6),
                line(listOf(o(14.2, 11.8), o(20, 11.8)), w = 2.2f),
                line(listOf(o(11.8, 11), o(14.2, 11.6)), w = 1.3f),
                line(listOf(o(15.6, 9), o(15.6, 6.4)), w = 1.2f),
                line(listOf(o(18.4, 9), o(18.4, 6.4)), w = 1.2f),
            ),
        ),
        Picto(
            "wash", o(8, 3.4),
            listOf(
                listOf(o(8, 7), o(8.4, 13.4)),
                listOf(o(8.4, 13.4), o(6.4, 21.4)),
                listOf(o(8.4, 13.4), o(11, 21.4)),
                listOf(o(8, 8), o(11, 10.6), o(13.6, 9.8)),
            ),
            listOf(
                Prop.Ring(o(16, 9.6), 2.4f, 1.4f),
                Prop.Dot(o(19.6, 6.4), 0.9f),
                Prop.Dot(o(21, 9), 0.6f),
                line(listOf(o(11.6, 13.4), o(22, 13.4)), w = 1.6f),
                line(listOf(o(12.6, 13.4), o(12.6, 21.4)), w = 1.4f),
                line(listOf(o(21, 13.4), o(21, 21.4)), w = 1.4f),
            ),
        ),
        Picto(
            "carry", o(11, 3.4),
            listOf(
                listOf(o(11, 7), o(11, 13.4)),
                listOf(o(11, 13.4), o(8.4, 21.4)),
                listOf(o(11, 13.4), o(14, 21.4)),
                listOf(o(11, 8), o(13.8, 13)),
                listOf(o(11, 8), o(8.4, 12.8)),
            ),
            listOf(fill(listOf(o(12.4, 13.6), o(18, 13.6), o(17.6, 19.4), o(12.8, 19.4))), line(listOf(o(13.6, 13.6), o(15.2, 11.6), o(16.8, 13.6)), w = 1.1f)),
        ),
        Picto(
            "water", o(7, 3.4),
            listOf(
                listOf(o(7, 7), o(7, 13.4)),
                listOf(o(7, 13.4), o(5, 21.4)),
                listOf(o(7, 13.4), o(9.6, 21.4)),
                listOf(o(7, 8), o(10.4, 10.4)),
            ),
            listOf(
                rect(10, 9.6, 14, 13.2),
                line(listOf(o(14, 11), o(17.4, 8.2)), w = 1.3f),
                Prop.Dot(o(18, 10.6), 0.6f),
                Prop.Dot(o(18.8, 12.6), 0.6f),
                fill(listOf(o(15.8, 18), o(21.2, 18), o(20.4, 21.6), o(16.6, 21.6))),
                line(listOf(o(18.5, 18), o(18.5, 14.2)), w = 1.2f),
                line(listOf(o(18.5, 16), o(16.4, 14.4)), w = 1.2f),
                line(listOf(o(18.5, 15.4), o(20.6, 13.8)), w = 1.2f),
            ),
        ),
        Picto(
            "phone", o(10, 4.4),
            listOf(
                listOf(o(10, 8), o(10, 14.6)),
                listOf(o(10, 14.6), o(8, 21.4)),
                listOf(o(10, 14.6), o(12.6, 21.4)),
                listOf(o(10, 9), o(13.6, 10), o(13, 6.6)),
                listOf(o(10, 9), o(7.6, 13.6)),
            ),
            listOf(rect(12, 3.4, 14.2, 7.2), line(listOf(o(16.4, 3.6), o(17.4, 5.2), o(16.4, 6.8)), w = 1.1f), line(listOf(o(18.4, 2.6), o(19.8, 5.2), o(18.4, 7.8)), w = 1.1f)),
        ),
        Picto(
            "sleep", o(5.2, 12.8),
            listOf(listOf(o(7.8, 14.2), o(13, 14.2))),
            listOf(
                fill(listOf(o(11, 12.2), o(21.4, 12.2), o(21.4, 16), o(11, 16))),
                line(listOf(o(2.4, 11.4), o(2.4, 21.2)), w = 1.4f),
                line(listOf(o(2.4, 17), o(21.6, 17)), w = 1.4f),
                line(listOf(o(21.6, 17), o(21.6, 21.2)), w = 1.4f),
                line(listOf(o(16.4, 4.4), o(19, 4.4), o(16.4, 7.4), o(19, 7.4)), w = 1f),
            ),
        ),
        Picto(
            "pushup", o(3.8, 9.4),
            listOf(listOf(o(6.6, 11.6), o(20.8, 19.2)), listOf(o(6.8, 11.8), o(7.4, 20.6))),
            listOf(line(listOf(o(2, 21.4), o(22, 21.4)), w = 1.3f)),
        ),
        Picto(
            "squat", o(10.4, 4.4),
            listOf(
                listOf(o(10.6, 8), o(9, 13.6)),
                listOf(o(9, 13.6), o(14.8, 14.6), o(13.8, 21.2)),
                listOf(o(10.6, 9.2), o(18.4, 9.2)),
            ),
            listOf(line(listOf(o(8, 21.4), o(17, 21.4)), w = 1.3f)),
        ),
        Picto(
            "rope", o(12, 3.4),
            listOf(
                listOf(o(12, 7), o(12, 13.2)),
                listOf(o(12, 13.2), o(10.4, 19)),
                listOf(o(12, 13.2), o(13.6, 19)),
                listOf(o(12, 8.4), o(7.6, 11.8)),
                listOf(o(12, 8.4), o(16.4, 11.8)),
            ),
            listOf(line(listOf(o(7.6, 11.8), o(5.2, 16.6), o(7, 21.2), o(12, 22.4), o(17, 21.2), o(18.8, 16.6), o(16.4, 11.8)), w = 1.1f)),
        ),
        Picto(
            "ball", o(9, 3.4),
            listOf(
                listOf(o(9, 7), o(9, 13.4)),
                listOf(o(9, 13.4), o(7.8, 21.4)),
                listOf(o(9, 13.4), o(13.8, 16.8)),
                listOf(o(9, 8), o(5, 11.2)),
                listOf(o(9, 8), o(13, 10.4)),
            ),
            listOf(Prop.Ring(o(17.8, 18.6), 2.6f, 1.4f), Prop.Dot(o(17.8, 18.6), 0.9f)),
        ),
        Picto(
            "dance", o(11.6, 3.4),
            listOf(
                listOf(o(11.8, 7), o(12.4, 13.4)),
                listOf(o(12, 8), o(15.4, 5.2), o(17, 1.8)),
                listOf(o(12, 8.6), o(8, 10.6), o(5.8, 8.4)),
                listOf(o(12.4, 13.4), o(16.2, 16.8), o(15.2, 21.4)),
                listOf(o(12.4, 13.4), o(9.4, 21.4)),
            ),
        ),
        Picto(
            "guitar", o(8.4, 3.4),
            listOf(
                listOf(o(8.4, 7), o(8.4, 13.4)),
                listOf(o(8.4, 13.4), o(6.6, 21.4)),
                listOf(o(8.4, 13.4), o(11, 21.4)),
                listOf(o(8.4, 8), o(11.6, 12.6)),
                listOf(o(8.4, 8), o(13.2, 9), o(17, 8.6)),
            ),
            listOf(Prop.Dot(o(13, 13.6), 2.7f), Prop.Dot(o(15, 11.6), 2f), line(listOf(o(14.6, 11.8), o(20.4, 6.2)), w = 1.2f)),
        ),
        Picto(
            "paint", o(6.6, 3.4),
            listOf(
                listOf(o(6.6, 7), o(6.6, 13.4)),
                listOf(o(6.6, 13.4), o(4.8, 21.4)),
                listOf(o(6.6, 13.4), o(9.2, 21.4)),
                listOf(o(6.6, 8), o(10, 9.8), o(12.4, 8.2)),
            ),
            listOf(
                rect(13.4, 4, 20.6, 12),
                line(listOf(o(14.6, 12), o(13, 21.4)), w = 1.3f),
                line(listOf(o(19.4, 12), o(21, 21.4)), w = 1.3f),
            ),
        ),
        Picto(
            "board", o(5.8, 3.8),
            listOf(
                listOf(o(5.8, 7.4), o(5.8, 13.8)),
                listOf(o(5.8, 13.8), o(4, 21.4)),
                listOf(o(5.8, 13.8), o(8.4, 21.4)),
                listOf(o(5.8, 8.6), o(9.6, 7), o(12.4, 6.4)),
            ),
            listOf(
                line(listOf(o(11.6, 2.6), o(21.6, 2.6), o(21.6, 12.4), o(11.6, 12.4), o(11.6, 2.6)), w = 1.3f),
                line(listOf(o(13.8, 5.6), o(19.4, 5.6)), w = 1.1f),
                line(listOf(o(13.8, 8.8), o(17.4, 8.8)), w = 1.1f),
            ),
        ),
        Picto(
            "laundry", o(5.6, 3.4),
            listOf(
                listOf(o(5.6, 7), o(5.6, 13.4)),
                listOf(o(5.6, 13.4), o(3.8, 21.4)),
                listOf(o(5.6, 13.4), o(8.2, 21.4)),
                listOf(o(5.6, 8.2), o(8.8, 11.6), o(11.4, 14.4)),
            ),
            listOf(
                line(listOf(o(11.2, 8), o(21.4, 8), o(21.4, 21.6), o(11.2, 21.6), o(11.2, 8)), w = 1.4f),
                line(listOf(o(11.2, 10.8), o(21.4, 10.8)), w = 1.1f),
                Prop.Ring(o(16.3, 15.8), 3f, 1.4f),
                Prop.Dot(o(19.4, 9.4), 0.6f),
            ),
        ),
        Picto(
            "dog", o(7.6, 3.4),
            listOf(
                listOf(o(7.6, 7), o(7.4, 13.4)),
                listOf(o(7.4, 13.4), o(10.2, 21.2)),
                listOf(o(7.4, 13.4), o(4.6, 21.2)),
                listOf(o(7.6, 8.2), o(10.6, 12)),
                listOf(o(7.6, 8.2), o(5, 12.4)),
            ),
            listOf(
                line(listOf(o(10.6, 12), o(15.6, 15.2)), w = 1f),
                line(listOf(o(15.6, 16.6), o(20.4, 16.6)), w = 2.4f),
                Prop.Dot(o(21, 14.6), 1.5f),
                line(listOf(o(16, 16.6), o(15.4, 20.8)), w = 1.6f),
                line(listOf(o(20, 16.6), o(20.6, 20.8)), w = 1.6f),
                line(listOf(o(15.6, 16.6), o(14.2, 14.4)), w = 1.2f),
            ),
        ),
        Picto(
            "child", o(8, 3.4),
            listOf(
                listOf(o(8, 7), o(8, 13.4)),
                listOf(o(8, 13.4), o(6, 21.4)),
                listOf(o(8, 13.4), o(10.6, 21.4)),
                listOf(o(8, 8), o(12.4, 13.2)),
                listOf(o(8, 8), o(5.4, 12.6)),
            ),
            listOf(
                Prop.Dot(o(16.4, 11), 1.8f),
                line(listOf(o(16.4, 13.4), o(16.4, 17.2)), w = 1.9f),
                line(listOf(o(16.4, 17.2), o(15, 21.4)), w = 1.9f),
                line(listOf(o(16.4, 17.2), o(17.8, 21.4)), w = 1.9f),
                line(listOf(o(16.4, 14.2), o(12.6, 13.4)), w = 1.9f),
            ),
        ),
        Picto(
            "bath", o(6, 10.8),
            listOf(listOf(o(8.2, 12.6), o(11, 13.6)), listOf(o(12.6, 13.6), o(15.6, 10.4), o(18.4, 13.6))),
            listOf(
                line(listOf(o(2.4, 14), o(21.6, 14)), w = 1.4f),
                fill(listOf(o(3.4, 14.6), o(20.6, 14.6), o(19, 19.6), o(5, 19.6))),
                line(listOf(o(6, 19.6), o(5.4, 21.6)), w = 1.3f),
                line(listOf(o(18, 19.6), o(18.6, 21.6)), w = 1.3f),
                Prop.Dot(o(19, 9.6), 0.8f),
                Prop.Dot(o(21, 7.6), 0.6f),
                Prop.Dot(o(10.6, 9.4), 0.6f),
            ),
        ),
        Picto(
            "car", null,
            emptyList(),
            listOf(
                fill(listOf(o(2.4, 17.4), o(2.4, 14.6), o(4.6, 13.8), o(7.6, 9.6), o(16, 9.6), o(19.4, 13.6), o(21.6, 14.4), o(21.6, 17.4))),
                Prop.Dot(o(7, 18.2), 2.3f),
                Prop.Dot(o(17.2, 18.2), 2.3f),
            ),
        ),
        Picto(
            "stairs", o(12.6, 2.6),
            listOf(
                listOf(o(12.4, 6.2), o(11.6, 11.2)),
                listOf(o(11.6, 11.2), o(9.4, 14), o(9.2, 17.2)),
                listOf(o(11.6, 11.2), o(14.8, 12.2), o(14.6, 13.2)),
                listOf(o(12.2, 7.2), o(9.6, 9.8)),
                listOf(o(12.2, 7.2), o(14.8, 9.4)),
            ),
            listOf(line(listOf(o(2, 21.4), o(7, 21.4), o(7, 17.4), o(12, 17.4), o(12, 13.4), o(17, 13.4), o(17, 9.4), o(22, 9.4)), w = 1.5f)),
        ),
        Picto(
            "dig", o(7.6, 3.4),
            listOf(
                listOf(o(7.8, 7), o(8.8, 13.4)),
                listOf(o(8.8, 13.4), o(6.8, 21.4)),
                listOf(o(8.8, 13.4), o(12.6, 16.4), o(14.2, 17.4)),
                listOf(o(8, 8), o(15.4, 7.4)),
                listOf(o(8.2, 9), o(14.9, 10.8)),
            ),
            listOf(
                line(listOf(o(16, 5.2), o(14.4, 18.4)), w = 1.3f),
                fill(listOf(o(12.8, 18.4), o(16, 18.6), o(15.6, 22), o(13, 21.8))),
            ),
        ),
        Picto(
            "eat", o(7.8, 4.6),
            listOf(
                listOf(o(7.8, 8.2), o(7.4, 15)),
                listOf(o(7.4, 15), o(12.8, 15), o(12.8, 21.4)),
                listOf(o(7.8, 9.4), o(10.8, 12.4), o(13.6, 11)),
            ),
            listOf(
                line(listOf(o(11.4, 13), o(22, 13)), w = 1.6f),
                line(listOf(o(20.6, 13), o(20.6, 21.4)), w = 1.4f),
                line(listOf(o(14.4, 12.2), o(18.4, 12.2)), w = 1.8f),
                line(listOf(o(13.6, 11), o(14.8, 8.2)), w = 1f),
                rect(19, 9.8, 20.8, 12.4),
            ),
        ),
    )

    fun of(i: Int): Picto = all[((i % all.size) + all.size) % all.size]
}

/** Draws [p] filling [size] (square), in [color]. */
fun DrawScope.picto(p: Picto, color: Color, at: Offset = Offset.Zero, size: Float = this.size.minDimension) {
    val k = size / 24f
    fun Offset.s() = Offset(at.x + x * k, at.y + y * k)
    val bar = Stroke(2.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
    for (limb in p.limbs) {
        val path = Path().apply {
            moveTo(limb[0].s().x, limb[0].s().y)
            limb.drop(1).forEach { lineTo(it.s().x, it.s().y) }
        }
        drawPath(path, color, style = bar)
    }
    p.head?.let { drawCircle(color, 2.3f * k, it.s()) }
    for (prop in p.props) when (prop) {
        is Prop.Line -> drawPath(
            Path().apply {
                moveTo(prop.points[0].s().x, prop.points[0].s().y)
                prop.points.drop(1).forEach { lineTo(it.s().x, it.s().y) }
            },
            color, style = Stroke(prop.width * k, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        is Prop.Fill -> drawPath(
            Path().apply {
                moveTo(prop.points[0].s().x, prop.points[0].s().y)
                prop.points.drop(1).forEach { lineTo(it.s().x, it.s().y) }
                close()
            },
            color,
        )
        is Prop.Ring -> drawCircle(color, prop.r * k, prop.c.s(), style = Stroke(prop.width * k))
        is Prop.Dot -> drawCircle(color, prop.r * k, prop.c.s())
    }
}

@Composable
fun Pictogram(index: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val p = Pictograms.of(index)
    Canvas(modifier.size(size)) { picto(p, color) }
}

/** A pictogram on a square tile of its colour, as on the Munich signs. */
@Composable
fun PictoTile(index: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 48.dp, ink: Color = Color.White) {
    val p = Pictograms.of(index)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension * 0.2f
        drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
        val pad = this.size.minDimension * 0.14f
        picto(p, ink, Offset(pad, pad), this.size.minDimension - 2 * pad)
    }
}
