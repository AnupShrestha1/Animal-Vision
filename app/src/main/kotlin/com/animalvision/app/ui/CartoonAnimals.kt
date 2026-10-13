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

/** Stroke [p] as a thick outlined tube (tentacles, coils, stalks) in the shared style. */
private fun DrawScope.tube(p: Path, fill: Color, width: Float, ln: Float) {
    drawPath(p, AvOutline, style = Stroke(width + ln * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(p, fill, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
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
    val cx = w / 2f
    val cy = size.height * 0.5f
    val hr = w * 0.23f
    val ln = w * 0.018f
    fun x(u: Float) = cx + u * hr
    fun y(v: Float) = cy + v * hr
    // ears
    for (s in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(x(s * 0.22f), y(-0.85f))
            lineTo(x(s * 0.5f), y(-1.45f))
            lineTo(x(s * 0.62f), y(-0.7f))
            close()
        }
        fillStroke(ear, c.primary, ln)
    }
    // long face, narrowing at the cheeks and widening again at the muzzle
    val head = Path().apply {
        moveTo(x(-0.62f), y(-0.7f))
        cubicTo(x(-0.66f), y(-0.1f), x(-0.42f), y(0.3f), x(-0.48f), y(0.75f))
        cubicTo(x(-0.6f), y(1.35f), x(0.6f), y(1.35f), x(0.48f), y(0.75f))
        cubicTo(x(0.42f), y(0.3f), x(0.66f), y(-0.1f), x(0.62f), y(-0.7f))
        cubicTo(x(0.45f), y(-1.05f), x(-0.45f), y(-1.05f), x(-0.62f), y(-0.7f))
        close()
    }
    fillStroke(head, c.primary, ln * 1.3f)
    // white blaze down the nose
    val blaze = Path().apply {
        moveTo(x(-0.08f), y(-0.55f))
        cubicTo(x(-0.16f), y(0.0f), x(-0.12f), y(0.4f), x(-0.06f), y(0.62f))
        lineTo(x(0.06f), y(0.62f))
        cubicTo(x(0.12f), y(0.4f), x(0.16f), y(0.0f), x(0.08f), y(-0.55f))
        close()
    }
    drawPath(blaze, Color.White.copy(alpha = 0.9f))
    // pale muzzle with nostrils
    drawOval(Color(0xFFC79A6B), Offset(x(-0.5f), y(0.62f)), Size(hr * 1.0f, hr * 0.62f))
    drawOval(AvOutline, Offset(x(-0.5f), y(0.62f)), Size(hr * 1.0f, hr * 0.62f), style = Stroke(ln))
    drawOval(AvOutline, Offset(x(-0.3f), y(0.82f)), Size(hr * 0.16f, hr * 0.22f))
    drawOval(AvOutline, Offset(x(0.14f), y(0.82f)), Size(hr * 0.16f, hr * 0.22f))
    // forelock between the ears
    val forelock = Path().apply {
        moveTo(x(-0.32f), y(-0.82f))
        cubicTo(x(-0.25f), y(-1.2f), x(0.2f), y(-1.25f), x(0.32f), y(-0.9f))
        cubicTo(x(0.2f), y(-0.62f), x(0.06f), y(-0.5f), x(0.0f), y(-0.38f))
        cubicTo(x(-0.08f), y(-0.55f), x(-0.25f), y(-0.62f), x(-0.32f), y(-0.82f))
        close()
    }
    fillStroke(forelock, c.secondary, ln * 0.8f)
    // eyes sit wide, on the sides of the head
    cartoonEyes(cx, y(-0.22f), hr * 0.13f, hr * 0.4f)
}

private fun DrawScope.drawEagle(c: CartoonColors) {
    val w = size.width
    val cx = w / 2f
    val cy = size.height * 0.48f
    val hr = w * 0.27f
    val ln = w * 0.018f
    fun x(u: Float) = cx + u * hr
    fun y(v: Float) = cy + v * hr
    // dark brown shoulders (clipped by the round sticker)
    drawOval(Color(0xFF5B3A1E), Offset(x(-1.4f), y(0.75f)), Size(hr * 3.0f, hr * 1.6f))
    drawOval(AvOutline, Offset(x(-1.4f), y(0.75f)), Size(hr * 3.0f, hr * 1.6f), style = Stroke(ln * 1.3f))
    // white head in profile, facing left
    val head = Path().apply {
        moveTo(x(0.55f), y(1.05f))
        cubicTo(x(0.95f), y(0.35f), x(0.85f), y(-0.75f), x(0.1f), y(-0.8f))
        cubicTo(x(-0.42f), y(-0.82f), x(-0.72f), y(-0.45f), x(-0.68f), y(-0.12f))
        lineTo(x(-0.55f), y(0.18f))
        cubicTo(x(-0.5f), y(0.55f), x(-0.35f), y(0.85f), x(-0.25f), y(1.05f))
        close()
    }
    fillStroke(head, c.primary, ln * 1.3f)
    // hooked yellow beak
    val beak = Path().apply {
        moveTo(x(-0.6f), y(-0.24f))
        cubicTo(x(-0.95f), y(-0.32f), x(-1.28f), y(-0.08f), x(-1.12f), y(0.34f))
        cubicTo(x(-1.06f), y(0.2f), x(-0.98f), y(0.14f), x(-0.88f), y(0.13f))
        lineTo(x(-0.55f), y(0.16f))
        close()
    }
    fillStroke(beak, c.secondary, ln)
    drawLine(AvOutline, Offset(x(-0.9f), y(0.04f)), Offset(x(-0.58f), y(0.02f)), ln * 0.9f, StrokeCap.Round)
    // fierce eye under a heavy brow
    eye(x(-0.3f), y(-0.3f), hr * 0.13f)
    drawLine(AvOutline, Offset(x(-0.58f), y(-0.5f)), Offset(x(-0.08f), y(-0.42f)), ln * 1.6f, StrokeCap.Round)
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
    val cx = w / 2f
    val cy = size.height * 0.5f
    val hr = w * 0.22f
    val ln = w * 0.018f
    fun x(u: Float) = cx + u * hr
    fun y(v: Float) = cy + v * hr
    val body = hr * 0.42f
    // two coils, the lower one larger
    val lowerCoil = Path().apply {
        moveTo(x(0.55f), y(1.12f))
        cubicTo(x(1.4f), y(1.05f), x(1.25f), y(0.7f), x(0.3f), y(0.72f))
        cubicTo(x(-1.1f), y(0.75f), x(-1.4f), y(1.25f), x(-0.2f), y(1.32f))
    }
    tube(lowerCoil, c.primary, body, ln)
    val upperCoil = Path().apply {
        moveTo(x(-0.75f), y(0.95f))
        cubicTo(x(-1.05f), y(0.5f), x(0.6f), y(0.35f), x(0.75f), y(0.72f))
    }
    tube(upperCoil, c.primary, body, ln)
    // neck rising to the head
    val neck = Path().apply {
        moveTo(x(0.7f), y(0.7f))
        cubicTo(x(0.75f), y(0.15f), x(0.1f), y(0.25f), x(0.0f), y(-0.3f))
    }
    tube(neck, c.primary, body, ln)
    // scale spots along the front coil
    for (i in 0..3) {
        drawCircle(c.secondary, hr * 0.07f, Offset(x(-0.6f + i * 0.4f), y(if (i % 2 == 0) 0.95f else 0.91f)))
    }
    // head: wider than the neck, flat-topped
    val head = Path().apply {
        moveTo(x(0.0f), y(-1.0f))
        cubicTo(x(0.62f), y(-1.0f), x(0.7f), y(-0.45f), x(0.32f), y(-0.12f))
        cubicTo(x(0.15f), y(0.0f), x(-0.15f), y(0.0f), x(-0.32f), y(-0.12f))
        cubicTo(x(-0.7f), y(-0.45f), x(-0.62f), y(-1.0f), x(0.0f), y(-1.0f))
        close()
    }
    fillStroke(head, c.primary, ln * 1.2f)
    cartoonEyes(cx, y(-0.6f), hr * 0.13f, hr * 0.3f)
    // forked tongue
    val tongue = Path().apply {
        moveTo(x(0.0f), y(-0.04f))
        lineTo(x(0.0f), y(0.32f))
        lineTo(x(-0.12f), y(0.48f))
        moveTo(x(0.0f), y(0.32f))
        lineTo(x(0.12f), y(0.48f))
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
    // iridescent neck: purple over green
    drawArc(Color(0xFF8E5BB5), 20f, 140f, false, Offset(cx - hr * 0.7f, cy + hr * 0.05f), Size(hr * 1.4f, hr * 1.1f), style = Stroke(hr * 0.2f))
    drawArc(c.secondary, 20f, 140f, false, Offset(cx - hr * 0.7f, cy + hr * 0.25f), Size(hr * 1.4f, hr * 1.1f), style = Stroke(hr * 0.24f))
    val beak = Path().apply {
        moveTo(cx - hr * 0.2f, cy + hr * 0.05f)
        lineTo(cx - hr * 0.62f, cy + hr * 0.3f)
        lineTo(cx - hr * 0.2f, cy + hr * 0.4f)
        close()
    }
    fillStroke(beak, Color(0xFF4A4F57), ln)
    // white cere at the base of the beak
    drawOval(Color.White, Offset(cx - hr * 0.34f, cy - hr * 0.02f), Size(hr * 0.22f, hr * 0.16f))
    drawOval(AvOutline, Offset(cx - hr * 0.34f, cy - hr * 0.02f), Size(hr * 0.22f, hr * 0.16f), style = Stroke(ln * 0.6f))
    cartoonEyes(cx + hr * 0.05f, cy - hr * 0.2f, hr * 0.14f, hr * 0.4f)
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
    val cx = w / 2f
    val cy = size.height * 0.42f
    val hr = w * 0.22f
    val ln = w * 0.018f
    fun x(u: Float) = cx + u * hr
    fun y(v: Float) = cy + v * hr
    // six curling arms fanning out below the mantle
    val arm = hr * 0.24f
    for (i in 0 until 6) {
        val t = (i - 2.5f) / 2.5f // -1 .. 1
        val s = if (t < 0f) -1f else 1f
        val p = Path().apply {
            moveTo(x(t * 0.45f), y(0.55f))
            cubicTo(x(t * 0.7f), y(1.05f), x(t * 1.35f), y(1.25f), x(t * 1.3f + s * 0.05f), y(0.95f))
            cubicTo(x(t * 1.25f + s * 0.02f), y(0.8f), x(t * 1.05f), y(0.82f), x(t * 1.08f), y(0.95f))
        }
        tube(p, c.primary, arm, ln)
    }
    // suckers on the outer arms
    for (s in listOf(-1f, 1f)) {
        drawCircle(Color(0xFFF2C6F3), hr * 0.06f, Offset(x(s * 0.88f), y(1.1f)))
        drawCircle(Color(0xFFF2C6F3), hr * 0.06f, Offset(x(s * 0.6f), y(0.98f)))
    }
    // mantle (head), taller than wide
    drawOval(c.primary, Offset(x(-0.85f), y(-1.0f)), Size(hr * 1.7f, hr * 1.75f))
    drawOval(AvOutline, Offset(x(-0.85f), y(-1.0f)), Size(hr * 1.7f, hr * 1.75f), style = Stroke(ln * 1.4f))
    drawOval(Color.White.copy(alpha = 0.25f), Offset(x(-0.45f), y(-0.82f)), Size(hr * 0.5f, hr * 0.32f))
    cartoonEyes(cx, y(0.12f), hr * 0.17f, hr * 0.38f)
    drawArc(AvOutline, 20f, 140f, false, Offset(x(-0.18f), y(0.3f)), Size(hr * 0.36f, hr * 0.22f), style = Stroke(ln, cap = StrokeCap.Round))
}

private fun DrawScope.drawMantisShrimp(c: CartoonColors) {
    val w = size.width
    val cx = w / 2f
    val cy = size.height * 0.5f
    val hr = w * 0.24f
    val ln = w * 0.018f
    fun x(u: Float) = cx + u * hr
    fun y(v: Float) = cy + v * hr
    // segmented body running off the bottom of the sticker
    val bodyTop = y(-0.35f)
    drawRoundRect(c.primary, Offset(x(-0.55f), bodyTop), Size(hr * 1.1f, hr * 2.2f), CornerRadius(hr * 0.5f))
    drawRoundRect(AvOutline, Offset(x(-0.55f), bodyTop), Size(hr * 1.1f, hr * 2.2f), CornerRadius(hr * 0.5f), style = Stroke(ln * 1.3f))
    for (v in listOf(0.35f, 0.7f, 1.05f, 1.4f)) {
        drawArc(AvOutline, 20f, 140f, false, Offset(x(-0.55f), y(v - 0.25f)), Size(hr * 1.1f, hr * 0.4f), style = Stroke(ln * 0.8f))
    }
    // folded raptorial appendages ("smashers") at the sides, in the species' green
    for (s in listOf(-1f, 1f)) {
        val claw = Path().apply {
            moveTo(x(s * 0.45f), y(-0.05f))
            cubicTo(x(s * 1.2f), y(-0.2f), x(s * 1.25f), y(0.55f), x(s * 0.75f), y(0.62f))
        }
        tube(claw, c.secondary, hr * 0.22f, ln)
    }
    // antennal scales
    for (s in listOf(-1f, 1f)) {
        val scale = Path().apply {
            moveTo(x(s * 0.25f), y(-0.35f))
            cubicTo(x(s * 0.75f), y(-0.75f), x(s * 1.05f), y(-0.55f), x(s * 0.85f), y(-0.3f))
            close()
        }
        fillStroke(scale, Color(0xFFE5484D), ln * 0.8f)
    }
    // eyes on stalks; each eye carries the mid-band of specialised receptor rows
    for (s in listOf(-1f, 1f)) {
        val stalk = Path().apply {
            moveTo(x(s * 0.15f), y(-0.35f))
            lineTo(x(s * 0.42f), y(-0.95f))
        }
        tube(stalk, c.primary, hr * 0.14f, ln)
        val ex = x(s * 0.46f)
        val ey = y(-1.1f)
        val ew = hr * 0.48f
        val eh = hr * 0.36f
        drawOval(Color(0xFF3FB27F), Offset(ex - ew / 2f, ey - eh / 2f), Size(ew, eh))
        drawRect(Color(0xFF1F6F8B), Offset(ex - ew / 2f, ey - eh * 0.12f), Size(ew, eh * 0.24f))
        drawOval(AvOutline, Offset(ex - ew / 2f, ey - eh / 2f), Size(ew, eh), style = Stroke(ln))
        drawCircle(AvOutline, hr * 0.06f, Offset(ex, ey))
    }
}

private fun DrawScope.drawDolphin(c: CartoonColors) {
    val w = size.width
    val cx = w / 2f
    val cy = size.height * 0.56f
    val hr = w * 0.3f
    val ln = w * 0.018f
    fun x(u: Float) = cx + u * hr
    fun y(v: Float) = cy + v * hr
    // dorsal fin (drawn first so the back overlaps its base)
    val fin = Path().apply {
        moveTo(x(-0.04f), y(-0.7f))
        cubicTo(x(0.08f), y(-0.95f), x(0.25f), y(-1.12f), x(0.44f), y(-1.15f))
        cubicTo(x(0.34f), y(-0.98f), x(0.36f), y(-0.78f), x(0.44f), y(-0.6f))
        close()
    }
    fillStroke(fin, c.secondary, ln)
    // body in profile: beak to the left, tail flukes to the right
    val body = Path().apply {
        moveTo(x(-1.05f), y(0.12f))
        cubicTo(x(-0.92f), y(0.02f), x(-0.8f), y(-0.04f), x(-0.64f), y(-0.16f))
        cubicTo(x(-0.56f), y(-0.62f), x(-0.06f), y(-0.86f), x(0.36f), y(-0.66f))
        cubicTo(x(0.64f), y(-0.52f), x(0.8f), y(-0.36f), x(0.88f), y(-0.2f))
        lineTo(x(1.12f), y(-0.44f))
        cubicTo(x(1.2f), y(-0.22f), x(1.15f), y(0.0f), x(1.2f), y(0.14f))
        lineTo(x(0.9f), y(0.0f))
        cubicTo(x(0.6f), y(0.26f), x(0.0f), y(0.46f), x(-0.45f), y(0.38f))
        cubicTo(x(-0.7f), y(0.33f), x(-0.9f), y(0.25f), x(-1.05f), y(0.12f))
        close()
    }
    fillStroke(body, c.primary, ln * 1.3f)
    // pale belly
    val belly = Path().apply {
        moveTo(x(-0.98f), y(0.17f))
        cubicTo(x(-0.6f), y(0.2f), x(0.0f), y(0.22f), x(0.55f), y(0.12f))
        cubicTo(x(0.2f), y(0.36f), x(-0.5f), y(0.42f), x(-0.98f), y(0.17f))
        close()
    }
    drawPath(belly, Color(0xFFE8F2FB))
    // pectoral flipper
    val flipper = Path().apply {
        moveTo(x(-0.3f), y(0.3f))
        cubicTo(x(-0.22f), y(0.6f), x(0.02f), y(0.72f), x(0.12f), y(0.68f))
        cubicTo(x(0.04f), y(0.52f), x(0.0f), y(0.4f), x(0.02f), y(0.32f))
        close()
    }
    fillStroke(flipper, c.secondary, ln)
    // smile along the beak, and one eye (profile view)
    val smile = Path().apply {
        moveTo(x(-1.0f), y(0.12f))
        quadraticTo(x(-0.82f), y(0.2f), x(-0.62f), y(0.1f))
    }
    drawPath(smile, AvOutline, style = Stroke(ln, cap = StrokeCap.Round))
    eye(x(-0.48f), y(-0.16f), hr * 0.12f)
}
