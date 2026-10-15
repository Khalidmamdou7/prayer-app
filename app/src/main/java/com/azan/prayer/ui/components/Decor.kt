package com.azan.prayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.azan.prayer.prayer.Prayer
import kotlin.math.cos
import kotlin.math.sin

/** A faint lattice of eight-pointed stars (khatam), drawn behind the hero header. */
@Composable
fun StarPattern(color: Color, modifier: Modifier = Modifier, cell: Dp = 64.dp) {
    Canvas(modifier.clipToBounds()) {
        val step = cell.toPx()
        val r = step * 0.36f
        val stroke = Stroke(width = 1.dp.toPx())
        var row = 0
        var y = -step / 2
        while (y < size.height + step) {
            var x = if (row % 2 == 0) -step / 2 else 0f
            while (x < size.width + step) {
                val c = Offset(x, y)
                drawRect(color, topLeft = c - Offset(r, r), size = Size(2 * r, 2 * r), style = stroke)
                rotate(45f, pivot = c) {
                    drawRect(color, topLeft = c - Offset(r, r), size = Size(2 * r, 2 * r), style = stroke)
                }
                drawCircle(color, radius = r * 0.38f, center = c, style = stroke)
                x += step
            }
            y += step / 2
            row++
        }
    }
}

/** Small time-of-day pictogram for each prayer. */
@Composable
fun PrayerGlyph(prayer: Prayer, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val u = size.minDimension / 24f
        val stroke = Stroke(width = 1.7f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun horizon(y: Float) = drawLine(color, Offset(3 * u, y * u), Offset(21 * u, y * u), 1.7f * u, StrokeCap.Round)
        when (prayer) {
            Prayer.FAJR -> {
                // Before sunrise: the dawn star over the horizon, first light below it.
                horizon(18f)
                drawPath(star(Offset(12 * u, 9.5f * u), 5.5f * u), color)
                drawLine(color, Offset(7 * u, 21.5f * u), Offset(17 * u, 21.5f * u), 1.7f * u, StrokeCap.Round)
            }
            Prayer.SUNRISE -> {
                horizon(17f)
                clipRect(bottom = 16f * u) { drawCircle(color, 5f * u, Offset(12 * u, 16 * u), style = stroke) }
                rays(color, Offset(12 * u, 16 * u), 7.5f * u, 9.5f * u, listOf(-90f, -45f, -135f, 0f, 180f))
            }
            Prayer.DHUHR -> {
                drawCircle(color, 4.2f * u, Offset(12 * u, 12 * u), style = stroke)
                rays(color, Offset(12 * u, 12 * u), 7f * u, 9.5f * u, (0 until 8).map { it * 45f })
            }
            Prayer.ASR -> {
                // Afternoon: the sun has come down, rays only above the horizon.
                horizon(20f)
                drawCircle(color, 4f * u, Offset(12 * u, 13 * u), style = stroke)
                rays(color, Offset(12 * u, 13 * u), 6.8f * u, 9f * u, listOf(-90f, -45f, -135f, 0f, 180f))
            }
            Prayer.MAGHRIB -> {
                horizon(15f)
                clipRect(bottom = 14f * u) { drawCircle(color, 6f * u, Offset(12 * u, 15 * u), style = stroke) }
                drawLine(color, Offset(7 * u, 19 * u), Offset(17 * u, 19 * u), 1.7f * u, StrokeCap.Round)
            }
            Prayer.ISHA -> {
                drawPath(crescent(Offset(11 * u, 13 * u), 7.5f * u), color, style = stroke)
                drawPath(star(Offset(18.5f * u, 6f * u), 3f * u), color)
            }
        }
    }
}

private fun DrawScope.rays(color: Color, c: Offset, from: Float, to: Float, angles: List<Float>) {
    for (a in angles) {
        val rad = Math.toRadians(a.toDouble())
        val d = Offset(cos(rad).toFloat(), sin(rad).toFloat())
        drawLine(color, c + d * from, c + d * to, 1.7f * (size.minDimension / 24f), StrokeCap.Round)
    }
}

private fun crescent(c: Offset, r: Float): Path {
    val outer = Path().apply { addOval(Rect(c, r)) }
    val bite = Path().apply { addOval(Rect(c + Offset(r * 0.45f, -r * 0.35f), r * 0.82f)) }
    return Path.combine(PathOperation.Difference, outer, bite)
}

private fun star(c: Offset, r: Float): Path = Path().apply {
    for (i in 0 until 8) {
        val radius = if (i % 2 == 0) r else r * 0.38f
        val a = Math.toRadians(-90.0 + i * 45.0)
        val p = Offset(c.x + radius * cos(a).toFloat(), c.y + radius * sin(a).toFloat())
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** Round "prayed" toggle: an empty ring, or a filled circle with a tick. */
@Composable
fun PrayedToggle(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (checked) scheme.primary else Color.Transparent, label = "fill")
    val ring = when {
        checked -> scheme.primary
        enabled -> scheme.outline
        else -> scheme.outlineVariant
    }
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.6.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                val tick = scheme.onPrimary
                Canvas(Modifier.size(14.dp)) {
                    val w = size.width
                    val path = Path().apply {
                        moveTo(w * 0.12f, w * 0.52f)
                        lineTo(w * 0.40f, w * 0.78f)
                        lineTo(w * 0.88f, w * 0.24f)
                    }
                    drawPath(path, tick, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}
