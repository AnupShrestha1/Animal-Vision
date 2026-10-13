package com.animalvision.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Shared dark outline so every cartoon reads as one consistent sticker set. */
private val AvOutline = Color(0xFF23272E)

/** Per-species colors. Every animal uses the same rendering, just its own palette. */
private data class CartoonColors(
    val primary: Color,
    val secondary: Color,
    val backdrop: Color,
)

private fun cartoonColors(id: String): CartoonColors = when (id) {
    "human" -> CartoonColors(Color(0xFFF3C9A0), Color(0xFF6B4A2B), Color(0xFFFFE3C2))
    "dog" -> CartoonColors(Color(0xFFD2A56B), Color(0xFFA9764A), Color(0xFFF7E2BE))
    "cat" -> CartoonColors(Color(0xFF9AA3AD), Color(0xFFF2A6B8), Color(0xFFE4EAEF))
    "horse" -> CartoonColors(Color(0xFF9A6234), Color(0xFF52331B), Color(0xFFEFDCC3))
    "eagle" -> CartoonColors(Color(0xFFF5F5F5), Color(0xFFF5B301), Color(0xFFD8ECF7))
    "bee" -> CartoonColors(Color(0xFFFFC531), Color(0xFF33373D), Color(0xFFFFF2C2))
    "snake" -> CartoonColors(Color(0xFF5FAE63), Color(0xFFCDE7B0), Color(0xFFDFF2DA))
    "pigeon" -> CartoonColors(Color(0xFF8496B0), Color(0xFF4FB3A5), Color(0xFFDEE5EE))
    "chicken" -> CartoonColors(Color(0xFFFFFFFF), Color(0xFFE53935), Color(0xFFFFE2E2))
    "butterfly" -> CartoonColors(Color(0xFFB07DE0), Color(0xFFF06292), Color(0xFFEFDDF9))
    "fly" -> CartoonColors(Color(0xFF6B5540), Color(0xFFE53935), Color(0xFFEAE2D2))
    "octopus" -> CartoonColors(Color(0xFFB06AB3), Color(0xFF7E4A80), Color(0xFFF2DCF2))
    "mantisshrimp" -> CartoonColors(Color(0xFFFF7A4D), Color(0xFF5FC46B), Color(0xFFFFE1CE))
    "dolphin" -> CartoonColors(Color(0xFF7FB2E5), Color(0xFF4E86C2), Color(0xFFD9EAF8))
    else -> CartoonColors(Color(0xFFB0B8C1), Color(0xFF7E8794), Color(0xFFE6EAEF))
}

/**
 * A cartoon avatar for the given species id, drawn in one consistent art style
 * (thick outlines, uniform eyes, soft circular sticker backdrop). No assets,
 * network, or text glyphs — always renders and scales to any size.
 */
@Composable
fun CartoonAnimalFace(id: String, modifier: Modifier = Modifier) {
    val c = cartoonColors(id)
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawCircle(c.backdrop, radius = w / 2f, center = Offset(w / 2f, size.height / 2f))
            when (id) {
                "human" -> drawHuman(c)
                "dog" -> drawDog(c)
                "cat" -> drawCat(c)
                "horse" -> drawHorse(c)
                "eagle" -> drawEagle(c)
                "bee" -> drawBee(c)
                "snake" -> drawSnake(c)
                "pigeon" -> drawPigeon(c)
                "chicken" -> drawChicken(c)
                "butterfly" -> drawButterfly(c)
                "fly" -> drawFly(c)
                "octopus" -> drawOctopus(c)
                "mantisshrimp" -> drawMantisShrimp(c)
                "dolphin" -> drawDolphin(c)
                else -> drawCat(c)
            }
        }
    }
}

/** Shared cartoon eye: dark ring, white sclera, dark pupil, and a glint. */
private fun DrawScope.eye(cx: Float, cy: Float, r: Float) {
    drawCircle(AvOutline, r, Offset(cx, cy))
    drawCircle(Color.White, r * 0.84f, Offset(cx, cy))
    drawCircle(AvOutline, r * 0.46f, Offset(cx + r * 0.12f, cy + r * 0.06f))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.15f, Offset(cx - r * 0.16f, cy - r * 0.2f))
}

private fun DrawScope.cartoonEyes(cx: Float, cy: Float, r: Float, dx: Float) {
    eye(cx - dx, cy, r)
    eye(cx + dx, cy, r)
}

/** Fill a path and outline it in the shared style. */
private fun DrawScope.fillStroke(p: Path, fill: Color, w: Float) {
    drawPath(p, fill)
    drawPath(p, AvOutline, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawHuman(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.54f
    val hr = w * 0.26f
    val ln = w * 0.018f
    drawCircle(c.primary, hr * 0.2f, Offset(cx - hr, cy))
    drawCircle(AvOutline, hr * 0.2f, Offset(cx - hr, cy), style = Stroke(ln))
    drawCircle(c.primary, hr * 0.2f, Offset(cx + hr, cy))
    drawCircle(AvOutline, hr * 0.2f, Offset(cx + hr, cy), style = Stroke(ln))
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    val hair = Path().apply {
        moveTo(cx - hr, cy - hr * 0.1f)
        cubicTo(cx - hr * 1.05f, cy - hr * 1.3f, cx + hr * 1.05f, cy - hr * 1.3f, cx + hr, cy - hr * 0.1f)
        lineTo(cx + hr * 0.7f, cy - hr * 0.35f)
        cubicTo(cx, cy - hr * 0.9f, cx - hr * 0.6f, cy - hr * 0.5f, cx - hr, cy - hr * 0.05f)
        close()
    }
    drawPath(hair, c.secondary)
    cartoonEyes(cx, cy, hr * 0.15f, hr * 0.42f)
    drawArc(
        AvOutline, 20f, 140f, false,
        Offset(cx - hr * 0.28f, cy + hr * 0.12f),
        Size(hr * 0.56f, hr * 0.4f), style = Stroke(ln * 1.2f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawDog(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val hr = w * 0.26f
    val ln = w * 0.018f
    for (s in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(cx + s * hr * 0.85f, cy - hr * 0.7f)
            cubicTo(cx + s * hr * 1.7f, cy - hr * 0.4f, cx + s * hr * 1.5f, cy + hr * 0.7f, cx + s * hr * 0.7f, cy + hr * 0.55f)
            cubicTo(cx + s * hr * 0.9f, cy, cx + s * hr * 0.85f, cy - hr * 0.3f, cx + s * hr * 0.85f, cy - hr * 0.7f)
            close()
        }
        fillStroke(ear, c.secondary, ln)
    }
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    drawOval(c.backdrop, Offset(cx - hr * 0.42f, cy + hr * 0.18f), Size(hr * 0.84f, hr * 0.6f))
    drawOval(AvOutline, Offset(cx - hr * 0.42f, cy + hr * 0.18f), Size(hr * 0.84f, hr * 0.6f), style = Stroke(ln))
    drawOval(AvOutline, Offset(cx - hr * 0.14f, cy + hr * 0.24f), Size(hr * 0.28f, hr * 0.2f))
    cartoonEyes(cx, cy - hr * 0.15f, hr * 0.14f, hr * 0.4f)
    val tongue = Path().apply {
        moveTo(cx, cy + hr * 0.55f)
        cubicTo(cx - hr * 0.12f, cy + hr * 0.85f, cx + hr * 0.12f, cy + hr * 0.85f, cx, cy + hr * 0.55f)
        close()
    }
    fillStroke(tongue, Color(0xFFF27D95), ln)
}


private fun DrawScope.drawCat(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.58f
    val hr = w * 0.26f
    val ln = w * 0.018f
    for (s in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(cx + s * hr * 0.95f, cy - hr * 0.55f)
            lineTo(cx + s * hr * 1.25f, cy - hr * 1.5f)
            lineTo(cx + s * hr * 0.3f, cy - hr * 0.85f)
            close()
        }
        fillStroke(ear, c.primary, ln)
    }
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    cartoonEyes(cx, cy - hr * 0.05f, hr * 0.16f, hr * 0.42f)
    val nose = Path().apply {
        moveTo(cx, cy + hr * 0.32f)
        lineTo(cx - hr * 0.11f, cy + hr * 0.16f)
        lineTo(cx + hr * 0.11f, cy + hr * 0.16f)
        close()
    }
    fillStroke(nose, c.secondary, ln * 0.8f)
    for (s in listOf(-1f, 1f)) {
        drawLine(AvOutline, Offset(cx + s * hr * 0.18f, cy + hr * 0.25f), Offset(cx + s * hr * 1.25f, cy + hr * 0.1f), ln * 0.8f, StrokeCap.Round)
        drawLine(AvOutline, Offset(cx + s * hr * 0.18f, cy + hr * 0.35f), Offset(cx + s * hr * 1.3f, cy + hr * 0.4f), ln * 0.8f, StrokeCap.Round)
    }
}

private fun DrawScope.drawHorse(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val hr = w * 0.24f
    val ln = w * 0.018f
    for (s in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(cx + s * hr * 0.5f, cy - hr * 0.6f)
            lineTo(cx + s * hr * 0.42f, cy - hr * 1.35f)
            lineTo(cx + s * hr * 1.0f, cy - hr * 0.55f)
            close()
        }
        fillStroke(ear, c.primary, ln)
    }
    val head = Path().apply {
        moveTo(cx - hr * 0.75f, cy - hr * 0.35f)
        cubicTo(cx - hr * 1.15f, cy + hr * 0.4f, cx - hr * 0.8f, cy + hr * 1.4f, cx, cy + hr * 1.5f)
        lineTo(cx + hr * 0.8f, cy + hr * 1.4f)
        cubicTo(cx + hr * 1.15f, cy + hr * 0.4f, cx + hr * 0.75f, cy - hr * 0.35f, cx + hr * 0.5f, cy - hr * 0.75f)
        cubicTo(cx, cy - hr * 1.05f, cx - hr * 0.5f, cy - hr * 0.75f, cx - hr * 0.75f, cy - hr * 0.35f)
        close()
    }
    fillStroke(head, c.primary, ln * 1.3f)
    val mane = Path().apply {
        moveTo(cx + hr * 0.15f, cy - hr * 0.95f)
        cubicTo(cx + hr * 0.9f, cy - hr * 0.9f, cx + hr * 1.1f, cy + hr * 0.2f, cx + hr * 0.55f, cy + hr * 0.9f)
        lineTo(cx + hr * 0.7f, cy + hr * 1.2f)
        lineTo(cx + hr * 0.05f, cy + hr * 1.05f)
        cubicTo(cx + hr * 0.1f, cy, cx - hr * 0.1f, cy - hr * 0.5f, cx + hr * 0.15f, cy - hr * 0.95f)
        close()
    }
    drawPath(mane, c.secondary)
    cartoonEyes(cx, cy - hr * 0.15f, hr * 0.13f, hr * 0.35f)
    drawOval(AvOutline, Offset(cx - hr * 0.32f, cy + hr * 1.18f), Size(hr * 0.22f, hr * 0.16f))
    drawOval(AvOutline, Offset(cx + hr * 0.1f, cy + hr * 1.18f), Size(hr * 0.22f, hr * 0.16f))
}


private fun DrawScope.drawEagle(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val hr = w * 0.24f
    val ln = w * 0.018f
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    val beak = Path().apply {
        moveTo(cx - hr * 0.2f, cy + hr * 0.15f)
        lineTo(cx, cy + hr * 1.15f)
        lineTo(cx + hr * 0.2f, cy + hr * 0.15f)
        close()
    }
    fillStroke(beak, c.secondary, ln)
    eye(cx - hr * 0.42f, cy - hr * 0.2f, hr * 0.16f)
    eye(cx + hr * 0.42f, cy - hr * 0.2f, hr * 0.16f)
    drawLine(AvOutline, Offset(cx - hr * 0.85f, cy - hr * 0.5f), Offset(cx - hr * 0.05f, cy - hr * 0.32f), ln * 1.2f, StrokeCap.Round)
    drawLine(AvOutline, Offset(cx + hr * 0.85f, cy - hr * 0.5f), Offset(cx + hr * 0.05f, cy - hr * 0.32f), ln * 1.2f, StrokeCap.Round)
}

private fun DrawScope.drawBee(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val hr = w * 0.23f
    val ln = w * 0.018f
    for (s in listOf(-1f, 1f)) {
        drawLine(AvOutline, Offset(cx + s * hr * 0.35f, cy - hr * 0.85f), Offset(cx + s * hr * 0.9f, cy - hr * 1.5f), ln, StrokeCap.Round)
        drawCircle(AvOutline, hr * 0.1f, Offset(cx + s * hr * 0.9f, cy - hr * 1.5f))
    }
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    drawArc(c.secondary, 200f, 140f, false, Offset(cx - hr, cy - hr), Size(hr * 2f, hr * 2f), style = Stroke(hr * 0.3f))
    for (s in listOf(-1f, 1f)) {
        drawOval(AvOutline, Offset(cx + (if (s < 0) -hr * 0.62f else hr * 0.3f), cy - hr * 0.5f), Size(hr * 0.32f, hr * 0.5f))
    }
    drawArc(AvOutline, 20f, 140f, false, Offset(cx - hr * 0.22f, cy + hr * 0.05f), Size(hr * 0.44f, hr * 0.35f), style = Stroke(ln, cap = StrokeCap.Round))
    for (s in listOf(-1f, 1f)) {
        val wing = Path().apply {
            moveTo(cx + s * hr * 0.5f, cy + hr * 0.1f)
            cubicTo(cx + s * hr * 1.6f, cy - hr * 0.6f, cx + s * hr * 1.2f, cy + hr * 0.9f, cx + s * hr * 0.4f, cy + hr * 0.55f)
            close()
        }
        drawPath(wing, Color.White.copy(alpha = 0.55f))
        drawPath(wing, AvOutline, style = Stroke(ln * 0.8f))
    }
}

private fun DrawScope.drawSnake(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val hr = w * 0.23f
    val ln = w * 0.018f
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    drawOval(c.secondary, Offset(cx - hr * 0.5f, cy + hr * 0.3f), Size(hr, hr * 0.7f))
    cartoonEyes(cx, cy - hr * 0.1f, hr * 0.15f, hr * 0.42f)
    val tongue = Path().apply {
        moveTo(cx, cy + hr * 0.95f)
        lineTo(cx, cy + hr * 1.6f)
        lineTo(cx - hr * 0.16f, cy + hr * 1.85f)
        moveTo(cx, cy + hr * 1.6f)
        lineTo(cx + hr * 0.16f, cy + hr * 1.85f)
    }
    drawPath(tongue, Color(0xFFE5394A), style = Stroke(ln * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}


private fun DrawScope.drawPigeon(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val hr = w * 0.25f
    val ln = w * 0.018f
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    drawArc(c.secondary, 20f, 140f, false, Offset(cx - hr * 0.7f, cy + hr * 0.15f), Size(hr * 1.4f, hr * 1.1f), style = Stroke(hr * 0.32f))
    val beak = Path().apply {
        moveTo(cx - hr * 0.18f, cy + hr * 0.05f)
        lineTo(cx - hr * 0.55f, cy + hr * 0.3f)
        lineTo(cx - hr * 0.18f, cy + hr * 0.42f)
        close()
    }
    fillStroke(beak, Color(0xFFE8913A), ln)
    cartoonEyes(cx + hr * 0.05f, cy - hr * 0.15f, hr * 0.14f, hr * 0.4f)
}

private fun DrawScope.drawChicken(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.58f
    val hr = w * 0.25f
    val ln = w * 0.018f
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    val comb = Path().apply {
        moveTo(cx - hr * 0.55f, cy - hr * 0.7f)
        cubicTo(cx - hr * 0.4f, cy - hr * 1.35f, cx - hr * 0.05f, cy - hr * 0.95f, cx + hr * 0.1f, cy - hr * 1.45f)
        cubicTo(cx + hr * 0.28f, cy - hr * 0.95f, cx + hr * 0.55f, cy - hr * 1.2f, cx + hr * 0.6f, cy - hr * 0.65f)
        close()
    }
    fillStroke(comb, c.secondary, ln)
    val beak = Path().apply {
        moveTo(cx - hr * 0.15f, cy + hr * 0.05f)
        lineTo(cx - hr * 0.7f, cy + hr * 0.35f)
        lineTo(cx - hr * 0.15f, cy + hr * 0.5f)
        close()
    }
    fillStroke(beak, Color(0xFFF5B301), ln)
    drawOval(c.secondary, Offset(cx - hr * 0.3f, cy + hr * 0.55f), Size(hr * 0.34f, hr * 0.5f))
    cartoonEyes(cx, cy - hr * 0.12f, hr * 0.14f, hr * 0.4f)
}

private fun DrawScope.drawButterfly(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.54f
    val hr = w * 0.18f
    val ln = w * 0.016f
    for (s in listOf(-1f, 1f)) {
        val upper = Path().apply {
            moveTo(cx + s * hr * 0.2f, cy - hr * 0.3f)
            cubicTo(cx + s * hr * 1.4f, cy - hr * 1.9f, cx + s * hr * 2.2f, cy - hr * 0.4f, cx + s * hr * 0.35f, cy + hr * 0.25f)
            close()
        }
        fillStroke(upper, c.primary, ln)
        val lower = Path().apply {
            moveTo(cx + s * hr * 0.35f, cy + hr * 0.25f)
            cubicTo(cx + s * hr * 1.9f, cy + hr * 0.2f, cx + s * hr * 1.6f, cy + hr * 1.6f, cx + s * hr * 0.3f, cy + hr * 0.85f)
            close()
        }
        fillStroke(lower, c.secondary, ln)
    }
    drawRoundRect(AvOutline, Offset(cx - hr * 0.18f, cy - hr * 0.6f), Size(hr * 0.36f, hr * 1.9f), CornerRadius(hr * 0.18f))
    drawCircle(AvOutline, hr * 0.42f, Offset(cx, cy - hr * 0.7f))
    for (s in listOf(-1f, 1f)) {
        drawLine(AvOutline, Offset(cx + s * hr * 0.2f, cy - hr * 0.95f), Offset(cx + s * hr * 0.6f, cy - hr * 1.7f), ln, StrokeCap.Round)
        drawCircle(AvOutline, hr * 0.11f, Offset(cx + s * hr * 0.6f, cy - hr * 1.7f))
    }
    for (s in listOf(-1f, 1f)) {
        drawCircle(Color.White.copy(alpha = 0.8f), hr * 0.18f, Offset(cx + s * hr * 1.1f, cy - hr * 0.55f))
    }
}


private fun DrawScope.drawFly(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.56f
    val hr = w * 0.22f
    val ln = w * 0.018f
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    for (s in listOf(-1f, 1f)) {
        drawCircle(c.secondary, hr * 0.42f, Offset(cx + s * hr * 0.45f, cy - hr * 0.1f))
        drawCircle(AvOutline, hr * 0.42f, Offset(cx + s * hr * 0.45f, cy - hr * 0.1f), style = Stroke(ln * 0.8f))
        drawCircle(Color.White.copy(alpha = 0.7f), hr * 0.12f, Offset(cx + s * hr * 0.45f, cy - hr * 0.1f))
    }
    for (s in listOf(-1f, 1f)) {
        drawLine(AvOutline, Offset(cx + s * hr * 0.15f, cy - hr * 0.9f), Offset(cx + s * hr * 0.25f, cy - hr * 1.5f), ln, StrokeCap.Round)
    }
}

private fun DrawScope.drawOctopus(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.48f
    val hr = w * 0.24f
    val ln = w * 0.018f
    for (i in -2..2) {
        val sx = cx + i * hr * 0.4f
        val tent = Path().apply {
            moveTo(sx - hr * 0.18f, cy + hr * 0.55f)
            cubicTo(sx - hr * 0.35f, cy + hr * 1.1f, sx + hr * 0.35f, cy + hr * 1.35f, sx + hr * 0.18f, cy + hr * 0.55f)
            close()
        }
        drawPath(tent, c.primary)
    }
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    cartoonEyes(cx, cy - hr * 0.05f, hr * 0.17f, hr * 0.42f)
    drawArc(AvOutline, 20f, 140f, false, Offset(cx - hr * 0.22f, cy + hr * 0.2f), Size(hr * 0.44f, hr * 0.32f), style = Stroke(ln, cap = StrokeCap.Round))
}

private fun DrawScope.drawMantisShrimp(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.58f
    val hr = w * 0.24f
    val ln = w * 0.018f
    drawCircle(c.primary, hr, Offset(cx, cy))
    drawCircle(AvOutline, hr, Offset(cx, cy), style = Stroke(ln * 1.4f))
    for (s in listOf(-1f, 1f)) {
        val ex = cx + s * hr * 0.5f
        val ey = cy - hr * 0.6f
        drawLine(c.primary, Offset(ex, ey + hr * 0.12f), Offset(ex, cy - hr * 0.25f), hr * 0.16f)
        drawCircle(c.primary, hr * 0.2f, Offset(ex, ey))
        drawCircle(AvOutline, hr * 0.2f, Offset(ex, ey), style = Stroke(ln * 0.9f))
        drawCircle(Color(0xFF5FC46B), hr * 0.12f, Offset(ex, ey))
        drawCircle(Color.White.copy(alpha = 0.85f), hr * 0.05f, Offset(ex - hr * 0.05f, ey - hr * 0.05f))
    }
    for (s in listOf(-1f, 1f)) {
        drawLine(AvOutline, Offset(cx + s * hr * 0.3f, cy + hr * 0.55f), Offset(cx + s * hr * 0.9f, cy + hr * 1.0f), ln * 0.9f, StrokeCap.Round)
    }
    drawArc(AvOutline, 20f, 140f, false, Offset(cx - hr * 0.2f, cy + hr * 0.15f), Size(hr * 0.4f, hr * 0.3f), style = Stroke(ln, cap = StrokeCap.Round))
}

private fun DrawScope.drawDolphin(c: CartoonColors) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val hr = w * 0.27f
    val ln = w * 0.018f
    val head = Path().apply {
        moveTo(cx - hr * 0.4f, cy - hr * 0.4f)
        cubicTo(cx - hr * 0.2f, cy - hr * 1.05f, cx + hr * 0.6f, cy - hr * 1.05f, cx + hr * 0.85f, cy - hr * 0.25f)
        cubicTo(cx + hr * 1.05f, cy + hr * 0.2f, cx + hr * 0.7f, cy + hr * 0.85f, cx + hr * 0.15f, cy + hr * 0.85f)
        cubicTo(cx - hr * 0.35f, cy + hr * 0.85f, cx - hr * 0.75f, cy + hr * 0.35f, cx - hr * 0.4f, cy - hr * 0.4f)
        close()
    }
    fillStroke(head, c.primary, ln * 1.3f)
    val belly = Path().apply {
        moveTo(cx + hr * 0.7f, cy + hr * 0.55f)
        cubicTo(cx + hr * 0.5f, cy + hr * 0.95f, cx + hr * 0.05f, cy + hr * 0.95f, cx - hr * 0.15f, cy + hr * 0.75f)
        close()
    }
    drawPath(belly, c.backdrop)
    cartoonEyes(cx + hr * 0.1f, cy - hr * 0.05f, hr * 0.13f, hr * 0.32f)
    drawArc(AvOutline, 20f, 90f, false, Offset(cx + hr * 0.25f, cy + hr * 0.15f), Size(hr * 0.5f, hr * 0.35f), style = Stroke(ln, cap = StrokeCap.Round))
}

