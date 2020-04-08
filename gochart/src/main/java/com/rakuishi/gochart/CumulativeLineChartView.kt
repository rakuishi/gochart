package com.rakuishi.gochart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.rakuishi.gochart.Util.Companion.dp2px
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.max

class CumulativeLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val year: Int, val value: Float)
    class Dot(val x: Float, val y: Float, val value: Float)

    private val bgHeight: Int = dp2px(context, 280f)
    private val bgPaddingX: Int = dp2px(context, 24f)
    private val bottomTextHeight: Int = dp2px(context, 17f)
    private val bgRadius: Float = dp2px(context, 8f).toFloat()
    private val lineTopMargin: Int = dp2px(context, 40f) // bgTopPadding 25 + barTextHeight 15
    private val lineBottomMargin: Int = dp2px(context, 5f)
    private val lineCircleOuterSize: Float = dp2px(context, 10f).toFloat()
    private val lineCircleInnerSize: Float = dp2px(context, 5f).toFloat()
    private val lineTextMarginY: Int = dp2px(context, 15f)
    private val bottomMonthTextYMargin: Int = dp2px(context, 17f)
    private val popupTriangleWidth: Int = dp2px(context, 7f)
    private val popupTriangleHeight: Int = dp2px(context, 5f)
    private val popupPadding: Int = dp2px(context, 4f)
    private val popupUnitMargin: Int = dp2px(context, 2f)
    private val popupTextSize: Int = dp2px(context, 11f)
    private val popupUnitTextSize: Int = dp2px(context, 7f)
    private val popupRadius: Float = dp2px(context, 2f).toFloat()
    private val selectedCircleOuterSize: Float = dp2px(context, 7f).toFloat()
    private val selectedCircleInnerSize: Float = dp2px(context, 6f).toFloat()

    private val rect: Rect = Rect()
    private val bgColor: Int = Color.parseColor("#ECECEC")
    private var textColor: Int = Color.parseColor("#3B3B3B")
    private val circleInnerColor: Int = Color.parseColor("#FFFFFF")
    private val popupBgColor: Int = Color.parseColor("#000000")
    private val popupTextColor: Int = Color.parseColor("#FFFFFF")
    private val selectedColor: Int = Color.parseColor("#B20505")
    private val selectedCircleOuterColor: Int = Color.parseColor("#FFFFFF")
    private val bgPaint: Paint = Paint()
    private val linePaint: Paint = Paint()
    private val lineTextPaint: Paint = Paint()
    private val lineTextOutlinePaint: Paint = Paint()
    private val lineCircleOuterPaint: Paint = Paint()
    private val lineCircleInnerPaint: Paint = Paint()
    private val bottomYearTextPaint: Paint = Paint()
    private val popupBgPaint: Paint = Paint()
    private val popupTextPaint: Paint = Paint()
    private val popupUnitTextPaint: Paint = Paint()
    private val selectedBorderPaint: Paint = Paint()
    private val selectedCircleOuterPaint: Paint = Paint()
    private val selectedCircleInnerPaint: Paint = Paint()

    // Save dot's position for handling touch events
    private val drawingDots = mutableListOf<Dot>()
    private var selectedDot: Dot? = null

    var bgMinimumWidth: Int = 0
    var valueFormat: String = "%.1f"
    var lineColor: Int = Color.parseColor("#409CF0")
        set(value) {
            field = value
            linePaint.color = value
            lineTextPaint.color = value
            lineCircleOuterPaint.color = value
        }
    var dataSet: ArrayList<ChartData> = arrayListOf()
        set(value) {
            field = value
            requestLayout()
        }
    var unitText: String = ""
    var isDarkMode: Boolean = false
        set(value) {
            field = value
            textColor = if (value) Color.parseColor("#FFFFFF") else Color.parseColor("#3B3B3B")
            bottomYearTextPaint.color = textColor
        }

    init {
        bgPaint.run {
            isAntiAlias = true
            color = bgColor
        }

        linePaint.run {
            isAntiAlias = true
            color = lineColor
            strokeWidth = dp2px(context, 5f).toFloat()
            style = Paint.Style.STROKE
        }

        lineTextPaint.run {
            isAntiAlias = true
            color = lineColor
            textSize = dp2px(getContext(), 11f).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        lineTextOutlinePaint.run {
            isAntiAlias = true
            color = bgColor
            textSize = dp2px(getContext(), 11f).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            strokeWidth = dp2px(getContext(), 2f).toFloat()
            style = Paint.Style.STROKE
        }

        lineCircleOuterPaint.run {
            isAntiAlias = true
            color = lineColor
        }

        lineCircleInnerPaint.run {
            isAntiAlias = true
            color = circleInnerColor
        }

        bottomYearTextPaint.run {
            isAntiAlias = true
            color = textColor
            textSize = dp2px(getContext(), 11f).toFloat()
            textAlign = Paint.Align.CENTER
        }

        popupTextPaint.run {
            isAntiAlias = true
            color = popupTextColor
            textSize = popupTextSize.toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        popupUnitTextPaint.run {
            isAntiAlias = true
            color = popupTextColor
            textSize = popupUnitTextSize.toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        popupBgPaint.run {
            isAntiAlias = true
            color = popupBgColor
        }

        selectedBorderPaint.run {
            isAntiAlias = true
            strokeWidth = dp2px(context, 1f).toFloat()
            color = selectedColor
        }

        selectedCircleOuterPaint.run {
            isAntiAlias = true
            color = selectedCircleOuterColor
        }

        selectedCircleInnerPaint.run {
            isAntiAlias = true
            color = selectedColor
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timerTask?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawDataSet(canvas)
        drawSelectedDotIfNeeded(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0, 0, measuredWidth, bgHeight)
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)
    }

    private fun drawDataSet(canvas: Canvas) {
        if (dataSet.size == 0) return
        val maxValue = calcMaxLineChartDataValue()
        drawPath(canvas, maxValue)

        for ((index, data) in dataSet.withIndex()) {
            val (centerX, centerY) = calcDotXY(data, index, maxValue)
            val bottomY = (bgHeight + bottomMonthTextYMargin).toFloat()

            // draw BottomYearText
            canvas.drawText(data.year.toString(), centerX, bottomY, bottomYearTextPaint)

            // draw Circle
            val valueText = valueFormat.format(data.value)
            canvas.drawCircle(centerX, centerY, lineCircleOuterSize, lineCircleOuterPaint)
            canvas.drawCircle(centerX, centerY, lineCircleInnerSize, lineCircleInnerPaint)
            canvas.drawText(valueText, centerX, centerY - lineTextMarginY, lineTextOutlinePaint)
            canvas.drawText(valueText, centerX, centerY - lineTextMarginY, lineTextPaint)
        }
    }

    private fun drawPath(canvas: Canvas, maxValue: Float) {
        drawingDots.clear()

        drawingDots.add(Dot(bgPaddingX.toFloat(), (bgHeight - lineBottomMargin).toFloat(), 0f))
        for (index in 0 until dataSet.size) {
            val (centerX, centerY) = calcDotXY(dataSet[index], index, maxValue)
            drawingDots.add(Dot(centerX, centerY, dataSet[index].value))
        }

        val x = drawingDots.map { dot -> dot.x }.toFloatArray()
        val y = drawingDots.map { dot -> dot.y }.toFloatArray()
        canvas.drawPath(MonotoneCubicSpline.computeControlPoints(x, y), linePaint)
    }

    private fun calcDotXY(data: ChartData, index: Int, maxValue: Float): Pair<Float, Float> {
        val ratio = if (maxValue == 0f) 0f else data.value / maxValue
        val betweenX: Int = (measuredWidth - bgPaddingX * 2) / dataSet.size
        val x = bgPaddingX + ((index + 1) * betweenX).toFloat()
        val y =
            lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin - lineBottomMargin)
        return Pair(x, y)
    }

    private fun drawSelectedDotIfNeeded(canvas: Canvas) {
        selectedDot?.let {
            // draw divider
            canvas.drawLine(it.x, 0f, it.x, bgHeight.toFloat(), selectedBorderPaint)
            canvas.drawCircle(it.x, it.y, selectedCircleOuterSize, selectedCircleOuterPaint)
            canvas.drawCircle(it.x, it.y, selectedCircleInnerSize, selectedCircleInnerPaint)

            val popupTextWidth = measurePopupTextWidth(valueFormat.format(it.value))
            val popupWidth = if (unitText.isNotEmpty()) {
                popupTextWidth + popupUnitMargin + measurePopupUnitTextWidth(unitText) + popupPadding * 2
            } else {
                popupTextWidth + popupPadding * 2
            }
            val popupHeight = (popupTextSize + popupPadding * 2).toFloat()
            var popupRight = it.x + popupWidth / 2
            if (popupRight > measuredWidth) {
                popupRight = measuredWidth.toFloat()
            }
            var popupLeft = popupRight - popupWidth
            if (popupLeft < 0) {
                popupLeft = 0f
                popupRight = popupWidth
            }

            val rectF = RectF(popupLeft, 0f, popupRight, popupHeight)
            canvas.drawRoundRect(rectF, popupRadius, popupRadius, popupBgPaint)

            // draw bottom triangle
            val path = Path()
            val triangleHalfWidth = popupTriangleWidth / 2f
            path.moveTo(it.x - triangleHalfWidth, popupHeight) // top left
            path.lineTo(it.x + triangleHalfWidth, popupHeight) // top right
            path.lineTo(it.x, popupHeight + popupTriangleHeight) // bottom center
            path.lineTo(it.x - triangleHalfWidth, popupHeight) // top left
            path.close()
            canvas.drawPath(path, popupBgPaint)

            canvas.drawText(
                valueFormat.format(it.value),
                popupLeft + popupPadding,
                popupHeight - popupTextSize / 2, // tweaked baseline
                popupTextPaint
            )

            if (unitText.isNotEmpty()) {
                canvas.drawText(
                    unitText,
                    popupRight - popupPadding,
                    popupHeight - popupTextSize / 2, // tweaked baseline
                    popupUnitTextPaint
                )
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight())
    }

    private fun measureWidth(widthMeasureSpec: Int): Int {
        return max(bgMinimumWidth, widthMeasureSpec)
    }

    private fun measureHeight(): Int {
        return bgHeight + bottomTextHeight
    }

    private fun calcMaxLineChartDataValue(): Float {
        if (dataSet.isEmpty()) return 0f

        var maxValue = 0f
        for (data in dataSet) {
            maxValue = max(maxValue, data.value)
        }

        // Remove value less than 1
        return if (maxValue < 1f) 0f else maxValue
    }

    private var timerTask: TimerTask? = null
    private var disallowIntercept: Boolean = false
    private var actionDownEvent: ActionDownEvent? = null
    private val holdMillis = 200L
    private val holdRange = dp2px(context, 8f)

    class ActionDownEvent(event: MotionEvent) {
        val x = event.x
        val y = event.y
        private val millis = System.currentTimeMillis()

        fun isDisturbed(holdMillis: Long, holdRange: Int, event: MotionEvent): Boolean {
            val isInHoldMillis = System.currentTimeMillis() - millis < holdMillis
            val isOutOfRange = abs(event.x - x) + abs(event.y - y) > holdRange
            return isInHoldMillis && isOutOfRange
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                actionDownEvent = ActionDownEvent(event)
                disallowIntercept = false
                timerTask = Timer().schedule(holdMillis) {
                    actionDownEvent?.let {
                        disallowIntercept = true
                        parent?.requestDisallowInterceptTouchEvent(true)

                        findSelectedDot(it.x.toInt(), it.y.toInt())
                        postInvalidate()
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val isDisturbed =
                    actionDownEvent?.isDisturbed(holdMillis, holdRange, event) != false
                if (!disallowIntercept && isDisturbed) {
                    timerTask?.cancel()
                    disallowIntercept = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                }

                if (disallowIntercept) {
                    // TODO: Don't call `postInvalidate()` frequently
                    findSelectedDot(event.x.toInt(), event.y.toInt())
                    postInvalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                actionDownEvent = null
                timerTask?.cancel()
                disallowIntercept = false
                parent?.requestDisallowInterceptTouchEvent(false)

                selectedDot = null
                postInvalidate()
            }
        }

        return true
    }

    private fun findSelectedDot(x: Int, y: Int) {
        selectedDot = null
        val r = Region()
        val range = (measuredWidth - bgPaddingX * 2) / (dataSet.size * 2)

        for (dot in drawingDots) {
            if (dot.x == bgPaddingX.toFloat()) continue

            r.set((dot.x - range).toInt(), 0, (dot.x + range).toInt(), bgHeight)

            if (r.contains(x, y)) {
                selectedDot = dot
                return
            }
        }
    }

    private fun measurePopupTextWidth(text: String): Float {
        return popupTextPaint.measureText(text)
    }

    private fun measurePopupUnitTextWidth(text: String): Float {
        return popupUnitTextPaint.measureText(text)
    }
}