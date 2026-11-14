package com.medsafe.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medsafe.app.data.model.PillShape

/**
 * Reusable pill icon. Used in lists, cards, the add/edit flow and (via PillPainter)
 * rendered to a bitmap for notification large icons.
 */
@Composable
fun PillIcon(
    shape: PillShape,
    color1: Color,
    color2: Color? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        drawIntoCanvas { canvas ->
            PillPainter.paint(
                canvas = canvas.nativeCanvas,
                shape = shape,
                color1 = color1.toArgb(),
                color2 = color2?.toArgb(),
                sizePx = side,
            )
        }
    }
}
