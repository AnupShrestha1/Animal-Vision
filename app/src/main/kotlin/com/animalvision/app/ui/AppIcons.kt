package com.animalvision.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small line icons the core Material icon set lacks (flash, camera switch), drawn on the
 * standard 24-unit grid so they sit alongside Icons.Filled.* at the same visual weight.
 */
object AppIcons {

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    val FlashOn: ImageVector = icon("FlashOn") {
        path(fill = SolidColor(Color.White)) {
            moveTo(13.5f, 2f)
            lineTo(5f, 13.5f)
            lineTo(11f, 13.5f)
            lineTo(10f, 22f)
            lineTo(19f, 10f)
            lineTo(13f, 10f)
            close()
        }
    }

    val FlashOff: ImageVector = icon("FlashOff") {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(13.5f, 2f)
            lineTo(5f, 13.5f)
            lineTo(11f, 13.5f)
            lineTo(10f, 22f)
            lineTo(19f, 10f)
            lineTo(13f, 10f)
            close()
        }
        path(stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(3.5f, 3.5f)
            lineTo(20.5f, 20.5f)
        }
    }

    /** Two curved arrows chasing each other around a lens. */
    val CameraSwitch: ImageVector = icon("CameraSwitch") {
        val arrows = SolidColor(Color.White)
        path(stroke = arrows, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(4.5f, 11f)
            arcTo(7.5f, 7.5f, 0f, false, true, 18.2f, 7f)
        }
        path(fill = arrows) {
            moveTo(19.8f, 3.6f)
            lineTo(20.4f, 9.2f)
            lineTo(15f, 8.2f)
            close()
        }
        path(stroke = arrows, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(19.5f, 13f)
            arcTo(7.5f, 7.5f, 0f, false, true, 5.8f, 17f)
        }
        path(fill = arrows) {
            moveTo(4.2f, 20.4f)
            lineTo(3.6f, 14.8f)
            lineTo(9f, 15.8f)
            close()
        }
        path(fill = arrows) {
            moveTo(12f, 9.5f)
            arcTo(2.5f, 2.5f, 0f, true, true, 12f, 14.5f)
            arcTo(2.5f, 2.5f, 0f, true, true, 12f, 9.5f)
            close()
        }
    }
}
