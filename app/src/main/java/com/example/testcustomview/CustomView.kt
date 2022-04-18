package com.example.testcustomview

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.withStyledAttributes
import kotlin.math.roundToInt


class CustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = context.resources.displayMetrics.density
    private val sp = context.resources.displayMetrics.scaledDensity
    private var unselectedTextColor = resources.getColorCompat(R.color.unselectedTextColor)
    private var selectedTextColor = resources.getColorCompat(R.color.selectedTextColor)
    private var selectorElevationColor = resources.getColorCompat(R.color.elevationColor)
    private var barElevationColor = Color.GRAY
    private var barColor = resources.getColorCompat(R.color.bgColor)
    private var selectorColor = resources.getColorCompat(R.color.bgColor)

    private var vTextSize = 20f * sp

    private val viewPadding = 12f * dp

    private val acc: Int

    private var viewPaddingHorizontal = viewPadding
    private var viewPaddingVertical = viewPadding

    private var listener: CustomViewListener? = null
    private var selectedIndex = 0
    private var cornerRadius = 12f * dp
    private var barElevationCorner = 8 * dp
    private var selectorElevationCorner = 12 * dp

    private var text = listOf("50$", "100$", "200$")
    private var textRects = mutableListOf<RectF>()
    private var maxTextWidth = 0f

    private var textPaddingVertical = (8f * dp).roundToInt().toFloat()
    private var textPaddingHorizontal = (12f * dp).roundToInt().toFloat()

    private val selectorRect = RectF(0f, 0f, 0f, 0f)
    private val barRect = RectF(0f, 0f, 0f, 0f)
    private val bgRect = Rect(0, 0, 0, 0)

    private var corners = floatArrayOf()

    private val barPath = Path()
    private val selectorPath = Path()

    private val barPaint = Paint()
    private val selectorPaint = Paint()
    private val textContainerPaint = Paint()
    private val selectedTextPaint = Paint()
    private val textPaint = Paint()


    init {
        calculations()
        acc = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
            LAYER_TYPE_SOFTWARE else LAYER_TYPE_HARDWARE

        context.withStyledAttributes(attrs, R.styleable.CustomView) {
            barColor = getColor(R.styleable.CustomView_barColor, barColor)
            unselectedTextColor = getColor(R.styleable.CustomView_unselectedTextColor, unselectedTextColor)
            selectedTextColor = getColor(R.styleable.CustomView_selectedTextColor, selectedTextColor)
            selectorColor = getColor(R.styleable.CustomView_selectorColor, selectorColor)
            selectorElevationColor = getColor(R.styleable.CustomView_selectorElevationColor, selectorElevationColor)
            barElevationColor = getColor(R.styleable.CustomView_barElevationColor, barElevationColor)
            vTextSize = getDimension(R.styleable.CustomView_textSize, vTextSize)
            cornerRadius = getDimension(R.styleable.CustomView_barCorner, cornerRadius)
            barElevationCorner = getDimension(R.styleable.CustomView_barElevationCorner, barElevationCorner)
            selectorElevationCorner = getDimension(R.styleable.CustomView_selectorElevationCorner, selectorElevationCorner)
            viewPaddingHorizontal = getDimension(R.styleable.CustomView_viewPaddingHorizontal, viewPaddingHorizontal)
            viewPaddingVertical= getDimension(R.styleable.CustomView_viewPaddingVertical, viewPaddingVertical)
            textPaddingHorizontal = getDimension(R.styleable.CustomView_textPaddingHorizontal, textPaddingHorizontal)
            textPaddingVertical= getDimension(R.styleable.CustomView_textPaddingVertical, textPaddingVertical)
        }

        updateCorners()

        updateBarPaint()
        updateSelectorPaint()
        updateTextContainerPaint()
        updateTextPaint()
        updateSelectedTextPaint()
    }


    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            drawBar(it)
            drawTextContainers(it)
            drawSelector(it)
            drawText(it)

        }
        super.onDraw(canvas)

    }

    private fun drawBar(canvas: Canvas) {
        barPath.addRoundRect(barRect, corners, Path.Direction.CW)
        canvas.drawPath(barPath, barPaint)
    }

    private fun drawTextContainers(canvas: Canvas) {
        textRects.forEach {
            canvas.drawRect(it, textContainerPaint)
        }
    }

    private fun drawSelector(canvas: Canvas) {
        selectorPath.reset()
        selectorPath.addRoundRect(selectorRect, corners, Path.Direction.CCW)
        canvas.drawPath(selectorPath, selectorPaint)
    }

    private fun drawText(canvas: Canvas) {
        text.forEachIndexed { index, text ->
            canvas.drawText(
                text,
                textRects[index].centerX(),
                textRects[index].centerY() - ((selectedTextPaint.descent() + selectedTextPaint.ascent()) / 2),
                if (index == selectedIndex) selectedTextPaint else textPaint
            )
        }
    }

    private fun calculations() {
        setLayerType(acc, selectorPaint)
        maxTextWidth = text.map {
            selectedTextPaint.measureText(it)
        }.maxOrNull() ?: 0f
        val metrics = selectedTextPaint.fontMetrics
        val rects = mutableListOf<RectF>()
        text.forEachIndexed { index, _ ->
            rects.add(
                RectF(
                    index * (2 * textPaddingHorizontal + maxTextWidth) + viewPaddingHorizontal,
                    viewPaddingVertical,
                    (index + 1) * (2 * textPaddingHorizontal + maxTextWidth) + viewPaddingHorizontal,
                    2 * textPaddingVertical + metrics.bottom - metrics.top + viewPaddingVertical
                )
            )
        }

        selectorRect.apply {
            left = viewPaddingHorizontal
            top = viewPaddingVertical
            right = 2 * textPaddingHorizontal + maxTextWidth + viewPaddingHorizontal
            bottom = 2 * textPaddingVertical + metrics.bottom - metrics.top + metrics.leading + viewPaddingVertical
        }
        barRect.apply {
            left = rects.first().left
            top = rects.first().top
            bottom = rects.first().bottom
            right = rects.last().right
        }
        bgRect.apply {
            right = (barRect.right + viewPaddingHorizontal).toInt()
            bottom = (barRect.bottom + viewPaddingHorizontal).toInt()
        }

        textRects = rects
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event?.x ?: return true
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!checkSelectorRect(x, y)) {
                    val rect = findRect(x, y)
                    rect?.let {
                        moveSelectorToRect(it)
                    }
                }

            }
            else -> Unit
        }
        return true
    }

    private fun checkSelectorRect(x: Float, y: Float): Boolean {
        val rect = selectorRect
        if (x in rect.left..rect.right && y in rect.top..rect.bottom) {
            return true
        }
        return false
    }

    private fun findRect(x: Float, y: Float): RectF? {
        textRects.forEachIndexed { index, it ->
            if (x in it.left..it.right && y in it.top..it.bottom) {
                listener?.onValueSelected(text[index])
                selectedIndex = index
                return it
            }

        }
        return null
    }

    private fun moveSelectorToRect(rect: RectF) {
        val duration = 100L
        val interpolator = AccelerateDecelerateInterpolator()
        val anim1 = ValueAnimator.ofFloat(selectorRect.left, rect.left).apply {
            this.interpolator = interpolator
            this.duration = duration
            addUpdateListener {
                selectorRect.left = it.animatedValue as Float
                invalidate()
            }
        }

        val anim2 = ValueAnimator.ofFloat(selectorRect.right, rect.right).apply {
            this.interpolator = interpolator
            this.duration = duration
            addUpdateListener {
                selectorRect.right = it.animatedValue as Float
                invalidate()
            }
        }
        anim1.start()
        anim2.start()
    }

    fun setListener(listener: CustomViewListener) {
        this.listener = listener
    }

    fun removeListener() {
        this.listener = null
    }

    fun setElements(elems: List<String>) {
        text = elems
        calculations()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(bgRect.right - bgRect.left, bgRect.bottom - bgRect.top)
    }

    private fun updateBarPaint() {
        barPaint.apply {
            color = barColor
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(barElevationCorner, 0f, 0f, barElevationColor)
        }
    }

    private fun updateSelectorPaint() {
        selectorPaint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = selectorColor
            setShadowLayer(selectorElevationCorner, 0f, 0f, selectorElevationColor)
        }
    }

    private fun updateTextContainerPaint() {
        textContainerPaint.apply {
            isAntiAlias = true
            color = Color.TRANSPARENT
            style = Paint.Style.FILL
        }
    }

    private fun updateSelectedTextPaint() {
        selectedTextPaint.apply {
            isAntiAlias = true
            textSize = vTextSize
            color = selectedTextColor
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
    }

    private fun updateTextPaint() {
        textPaint.apply {
            isAntiAlias = true
            textSize = vTextSize
            color = unselectedTextColor
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
    }

    private fun updateCorners() {
        corners = floatArrayOf(
            cornerRadius, cornerRadius,
            cornerRadius, cornerRadius,
            cornerRadius, cornerRadius,
            cornerRadius, cornerRadius
        )
    }


}

interface CustomViewListener {
    fun onValueSelected(value: String)
}

@ColorInt
fun Resources.getColorCompat(@ColorRes color: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.getColor(color, null)
    } else {
        this.getColor(color)
    }
}