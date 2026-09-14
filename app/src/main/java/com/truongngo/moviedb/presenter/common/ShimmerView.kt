package com.truongngo.moviedb.presenter.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils

/** Content-shaped placeholders. Animation only runs while this view is visible. */
class ShimmerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private val density = resources.displayMetrics.density
    private val foreground = TypedValue().let {
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, it, true)
        if (it.resourceId != 0) context.getColorStateList(it.resourceId).defaultColor else it.data
    }
    private val baseColor = ColorUtils.setAlphaComponent(foreground, 22)
    private val highlightColor = ColorUtils.setAlphaComponent(foreground, 7)

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible && ValueAnimator.areAnimatorsEnabled()) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 1300
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener { progress = it.animatedValue as Float; invalidate() }
                    start()
                }
            }
        } else stopAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width / density
        val h = height / density
        if (w <= 0f || h <= 0f) return
        val x = -w + progress * w * 3
        paint.shader = LinearGradient(x, 0f, x + w, 0f,
            intArrayOf(baseColor, highlightColor, baseColor), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        canvas.save()
        canvas.scale(density, density)
        fun block(left: Float, top: Float, right: Float, bottom: Float, radius: Float = 8f) {
            if (right > left) canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
        }
        when (tag?.toString()) {
            "banner" -> {
                block(20f, 0f, w - 20f, 222f, 16f)
                block(20f, 238f, w * .65f, 256f)
                block(20f, 266f, w * .4f, 278f)
            }
            "detail" -> {
                block(0f, 0f, w, 240f, 0f)
                block(20f, 260f, 120f, 410f)
                block(136f, 264f, w - 20f, 288f)
                block(136f, 302f, w - 48f, 318f)
                block(136f, 338f, w - 20f, 352f)
                block(136f, 366f, w - 64f, 380f)
                block(20f, 432f, w * .7f, 448f)
                block(20f, 480f, w * .45f, 502f)
                repeat(5) { row -> block(20f, 524f + row * 26, w - if (row == 4) 70f else 20f, 538f + row * 26) }
            }
            "compact" -> block(0f, 4f, w, h - 4f)
            else -> {
                var left = 20f
                while (left < w) {
                    block(left, 0f, left + 140f, 210f)
                    block(left, 222f, left + 124f, 238f)
                    block(left, 248f, left + 84f, 260f)
                    left += 152f
                }
            }
        }
        canvas.restore()
    }
}
