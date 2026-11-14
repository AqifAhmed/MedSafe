package com.medsafe.app.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.medsafe.app.data.model.PillShape

/**
 * Draws pill glyphs with a plain Android Canvas. The same code powers the Compose
 * [PillIcon] and the notification large icon, so there are no raster assets anywhere.
 */
object PillPainter {

    fun paint(
        canvas: Canvas,
        shape: PillShape,
        color1: Int,
        color2: Int?,
        sizePx: Float,
    ) {
        if (sizePx <= 0f) return
        val inset = sizePx * 0.1f
        val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val second = color2 ?: shade(color1, 0.78f)
        when (shape) {
            PillShape.CAPSULE -> capsule(canvas, bounds, color1, second, split = false)
            PillShape.HALF_CAPSULE -> capsule(canvas, bounds, color1, second, split = true)
            PillShape.ROUND_TABLET -> roundTablet(canvas, bounds, color1)
            PillShape.OVAL_TABLET -> ovalTablet(canvas, bounds, color1)
            PillShape.DROP -> drop(canvas, bounds, color1)
            PillShape.INJECTION -> injection(canvas, bounds, color1, second)
            PillShape.INHALER -> inhaler(canvas, bounds, color1, second)
        }
    }

    private fun capsule(canvas: Canvas, bounds: RectF, color1: Int, color2: Int, split: Boolean) {
        val width = bounds.width() * 0.62f
        val rect = RectF(
            bounds.centerX() - width / 2f,
            bounds.top,
            bounds.centerX() + width / 2f,
            bounds.bottom,
        )
        val radius = width / 2f
        val path = Path().apply {
            addRoundRect(rect, radius, radius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        if (split) {
            canvas.drawRect(rect.left, rect.top, rect.right, rect.centerY(), fill(color1))
            canvas.drawRect(rect.left, rect.centerY(), rect.right, rect.bottom, fill(color2))
        } else {
            canvas.drawRect(rect, fill(color1))
            canvas.drawOval(
                RectF(
                    rect.left + width * 0.14f,
                    rect.top + rect.height() * 0.12f,
                    rect.left + width * 0.40f,
                    rect.top + rect.height() * 0.46f,
                ),
                fill(Color.argb(64, 255, 255, 255)),
            )
        }
        canvas.restore()
        outline(canvas, path, bounds.width() * 0.035f)
    }

    private fun roundTablet(canvas: Canvas, bounds: RectF, color1: Int) {
        val radius = bounds.width() * 0.42f
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, fill(color1))
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shade(color1, 0.6f)
            strokeWidth = radius * 0.08f
        }
        canvas.drawLine(
            bounds.centerX() - radius * 0.65f,
            bounds.centerY(),
            bounds.centerX() + radius * 0.65f,
            bounds.centerY(),
            line,
        )
        canvas.drawCircle(
            bounds.centerX(),
            bounds.centerY(),
            radius,
            stroke(color1, radius * 0.07f),
        )
    }

    private fun ovalTablet(canvas: Canvas, bounds: RectF, color1: Int) {
        val rect = RectF(
            bounds.left + bounds.width() * 0.04f,
            bounds.top + bounds.height() * 0.22f,
            bounds.right - bounds.width() * 0.04f,
            bounds.bottom - bounds.height() * 0.22f,
        )
        canvas.drawOval(rect, fill(color1))
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shade(color1, 0.6f)
            strokeWidth = rect.height() * 0.08f
        }
        canvas.drawLine(rect.centerX() - rect.width() * 0.3f, rect.centerY(), rect.centerX() + rect.width() * 0.3f, rect.centerY(), line)
        canvas.drawOval(rect, stroke(color1, rect.height() * 0.08f))
    }

    private fun drop(canvas: Canvas, bounds: RectF, color1: Int) {
        val path = Path()
        val cx = bounds.centerX()
        val top = bounds.top + bounds.height() * 0.02f
        val bottom = bounds.bottom
        val left = bounds.left + bounds.width() * 0.16f
        val right = bounds.right - bounds.width() * 0.16f
        path.moveTo(cx, top)
        path.cubicTo(
            right,
            top + bounds.height() * 0.35f,
            right,
            bottom - bounds.height() * 0.28f,
            cx,
            bottom,
        )
        path.cubicTo(
            left,
            bottom - bounds.height() * 0.28f,
            left,
            top + bounds.height() * 0.35f,
            cx,
            top,
        )
        path.close()
        canvas.drawPath(path, fill(color1))
        canvas.drawOval(
            RectF(
                cx - bounds.width() * 0.10f,
                bottom - bounds.height() * 0.34f,
                cx - bounds.width() * 0.01f,
                bottom - bounds.height() * 0.18f,
            ),
            fill(Color.argb(72, 255, 255, 255)),
        )
        canvas.drawPath(path, stroke(color1, bounds.width() * 0.035f))
    }

    private fun injection(canvas: Canvas, bounds: RectF, color1: Int, color2: Int) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val w = bounds.width()
        val h = bounds.height()
        canvas.save()
        canvas.rotate(-42f, cx, cy)
        val barrel = RectF(cx - w * 0.26f, cy - h * 0.11f, cx + w * 0.20f, cy + h * 0.11f)
        canvas.drawRoundRect(barrel, h * 0.09f, h * 0.09f, fill(color1))
        canvas.save()
        canvas.clipRect(barrel.left, barrel.top, barrel.centerX(), barrel.bottom)
        canvas.drawRoundRect(barrel, h * 0.09f, h * 0.09f, fill(color2))
        canvas.restore()
        // Flange
        canvas.drawRect(barrel.right, cy - h * 0.17f, barrel.right + w * 0.045f, cy + h * 0.17f, fill(color2))
        // Plunger
        val plungerPaint = fill(shade(color1, 0.65f))
        canvas.drawRect(barrel.left - w * 0.14f, cy - h * 0.035f, barrel.left, cy + h * 0.035f, plungerPaint)
        canvas.drawRect(barrel.left - w * 0.19f, cy - h * 0.12f, barrel.left - w * 0.14f, cy + h * 0.12f, plungerPaint)
        // Needle
        val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shade(color1, 0.5f)
            strokeWidth = h * 0.035f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(barrel.right + w * 0.045f, cy, cx + w * 0.48f, cy, needle)
        canvas.drawRoundRect(barrel, h * 0.09f, h * 0.09f, stroke(color1, w * 0.025f))
        canvas.restore()
    }

    private fun inhaler(canvas: Canvas, bounds: RectF, color1: Int, color2: Int) {
        val w = bounds.width()
        val h = bounds.height()
        val cx = bounds.centerX()
        val body = RectF(cx - w * 0.30f, bounds.top + h * 0.06f, cx + w * 0.12f, bounds.bottom - h * 0.10f)
        canvas.drawRoundRect(body, w * 0.09f, w * 0.09f, fill(color1))
        // Cap / canister highlight
        canvas.drawRoundRect(
            RectF(body.left + w * 0.05f, body.top + h * 0.06f, body.right - w * 0.04f, body.top + h * 0.30f),
            w * 0.05f,
            w * 0.05f,
            fill(color2),
        )
        // Mouthpiece
        val mouth = RectF(cx + w * 0.02f, bounds.top + h * 0.38f, cx + w * 0.42f, bounds.top + h * 0.58f)
        canvas.drawRoundRect(mouth, h * 0.06f, h * 0.06f, fill(shade(color1, 0.75f)))
        // Spray dots
        val spray = fill(shade(color1, 0.45f))
        canvas.drawCircle(cx + w * 0.46f, bounds.top + h * 0.31f, w * 0.025f, spray)
        canvas.drawCircle(cx + w * 0.37f, bounds.top + h * 0.26f, w * 0.020f, spray)
        canvas.drawRoundRect(body, w * 0.09f, w * 0.09f, stroke(color1, w * 0.03f))
    }

    private fun fill(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.argb(110, Color.red(shade(color, 0.55f)), Color.green(shade(color, 0.55f)), Color.blue(shade(color, 0.55f)))
        style = Paint.Style.STROKE
        strokeWidth = width.coerceAtLeast(1f)
    }

    private fun outline(canvas: Canvas, path: Path, width: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 59, 42, 34)
            style = Paint.Style.STROKE
            strokeWidth = width.coerceAtLeast(1f)
        }
        canvas.drawPath(path, paint)
    }

    private fun shade(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
}
