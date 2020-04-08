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
import kotlin.math.*

class AnnualLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val year: Int, val month: Int, val value: Float)
    class Year(val value: Int, val color: Int, var isActive: Boolean, var region: Region = Region())
    class Dot(val x: Float, val y: Float, val year: Int, val value: Float)

    private val bgHeight: Int = dp2px(context, 280f)
    private val bottomMonthTextHeight: Int = dp2px(context, 23f)
    private val bottomYearTextHeight: Float = dp2px(context, 24f).toFloat()
    private val bgRadius: Float = dp2px(context, 8f).toFloat()
    private val lineTopMargin: Int = dp2px(context, 40f) // bgTopPadding 25 + barTextHeight 15
    private val lineBottomMargin: Int = dp2px(context, 5f)
    private val lineBetweenX: Int by lazy { measuredWidth / 13 }
    private val lineLastCircleOuterSize: Float = dp2px(context, 10f).toFloat()
    private val lineLastCircleInnerSize: Float = dp2px(context, 5f).toFloat()
    private val lineCircleOuterSize: Float = dp2px(context, 3.5f).toFloat()
    private val lineCircleInnerSize: Float = dp2px(context, 1.5f).toFloat()
    private val lineTextMarginY: Int = dp2px(context, 15f)
    private val bottomMonthTextYMargin: Int = dp2px(context, 17f)
    private val bottomYearMaxSize: Int = 5
    private val bottomYearXMargin: Int = dp2px(context, 13f)
    private val bottomYearYMargin: Int = dp2px(context, 33f) // between with bg
    private val bottomYearTextYMargin: Int = dp2px(context, 4f)
    private val bottomYearCircleSize: Float = dp2px(context, 5f).toFloat()
    private val popupTriangleWidth: Int = dp2px(context, 7f)
    private val popupTriangleHeight: Int = dp2px(context, 5f)
    private val popupPadding: Int = dp2px(context, 4f)
    private val popupYearMargin: Int = dp2px(context, 6f)
    private val popupUnitMargin: Int = dp2px(context, 2f)
    private val popupTextSize: Int = dp2px(context, 11f)
    private val popupUnitTextSize: Int = dp2px(context, 7f)
    private val popupYearTextSize: Int = dp2px(context, 6f)
    private val popupRadius: Float = dp2px(context, 2f).toFloat()
    private val selectedCircleOuterSize: Float = dp2px(context, 7f).toFloat()
    private val selectedCircleInnerSize: Float = dp2px(context, 6f).toFloat()

    private val rect: Rect = Rect()
    private val bgColor: Int = Color.parseColor("#ECECEC")
    private var textColor: Int = Color.parseColor("#3B3B3B")
    private val inactiveCircleColor = Color.parseColor("#BCBCBC")
    private val inactiveTextColor: Int = Color.parseColor("#D2D2D2")
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
    private val bottomMonthTextPaint: Paint = Paint()
    private val bottomYearTextPaint: Paint = Paint()
    private val bottomYearCirclePaint: Paint = Paint()
    private val popupBgPaint: Paint = Paint()
    private val popupTextPaint: Paint = Paint()
    private val popupUnitTextPaint: Paint = Paint()
    private val popupYearTextPaint: Paint = Paint()
    private val selectedBorderPaint: Paint = Paint()
    private val selectedCircleOuterPaint: Paint = Paint()
    private val selectedCircleInnerPaint: Paint = Paint()
    private val years = mutableListOf<Year>()

    // to draw Bezier Curve
    private val drawingDots = mutableListOf<Dot>()
    private val entireDrawingDots = mutableListOf<Dot>()
    private var selectedDots = mutableListOf<Dot>()

    private val bottomYearTextWidthWithPadding: Int by lazy {
        (bottomYearTextPaint.measureText("2020") + dp2px(context, 10f)).toInt()
    }

    var bgMinimumWidth: Int = 0
    var valueFormat: String = "%.1f"
    var lineColor: Int = Color.parseColor("#409CF0")
        set(value) {
            field = value
            linePaint.color = value
            lineTextPaint.color = value
            lineCircleOuterPaint.color = value
            bottomYearCirclePaint.color = value
        }
    var dataSet: ArrayList<ChartData> = arrayListOf()
        set(value) {
            var defaultActiveCount = 5
            val defaultColors = arrayListOf(
                Color.parseColor("#3EA5F5"),
                Color.parseColor("#46E580"),
                Color.parseColor("#9751FC"),
                Color.parseColor("#EF46C5"),
                Color.parseColor("#FCA551"),
                Color.parseColor("#FC516F"),
                Color.parseColor("#3EC0CD"),
                Color.parseColor("#43C954"),
                Color.parseColor("#F4C83A"),
                Color.parseColor("#CC53D0")
            )

            // order by descending, 2020 -> 2010
            years.addAll(value.map { chartData -> chartData.year }
                .distinct()
                .sortedDescending()
                .map { year ->
                    val color =
                        if (defaultColors.size == 0) inactiveCircleColor
                        else defaultColors.removeAt(0)
                    Year(year, color, (--defaultActiveCount >= 0))

                }
                .toMutableList())
            field = value
            requestLayout()
        }
    var unitText: String = ""
    var isDarkMode: Boolean = false
        set(value) {
            field = value
            textColor = if (value) Color.parseColor("#FFFFFF") else Color.parseColor("#3B3B3B")
            bottomMonthTextPaint.color = textColor
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

        bottomMonthTextPaint.run {
            isAntiAlias = true
            color = textColor
            textSize = dp2px(getContext(), 11f).toFloat()
            textAlign = Paint.Align.CENTER
        }

        bottomYearTextPaint.run {
            isAntiAlias = true
            color = textColor
            textSize = dp2px(getContext(), 11f).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        bottomYearCirclePaint.run {
            isAntiAlias = true
            color = lineColor
        }

        popupTextPaint.run {
            isAntiAlias = true
            color = popupTextColor
            textSize = popupTextSize.toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        popupUnitTextPaint.run {
            isAntiAlias = true
            color = popupTextColor
            textSize = popupUnitTextSize.toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        popupYearTextPaint.run {
            isAntiAlias = true
            color = popupTextColor
            textSize = popupYearTextSize.toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
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
        drawBottomMonthText(canvas)
        drawDataSet(canvas)
        drawSelectedDotIfNeeded(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0, 0, measuredWidth, bgHeight)
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)
    }

    private fun drawBottomMonthText(canvas: Canvas) {
        for (index in 1..12) {
            canvas.drawText(
                index.toString(),
                (index * lineBetweenX).toFloat(),
                (bgHeight + bottomMonthTextYMargin).toFloat(),
                bottomMonthTextPaint
            )
        }
    }

    private fun drawDataSet(canvas: Canvas) {
        val maxValue = calcMaxLineChartDataValue()
        entireDrawingDots.clear()

        // draw descending
        for ((index, year) in years.withIndex()) {
            drawBottomYear(canvas, index, year)
        }

        // draw ascending
        for ((index, year) in years.reversed().withIndex()) {
            if (!year.isActive) continue

            val values = createYearValues(year.value)
            drawPath(canvas, values, year, maxValue)
            drawCircle(canvas, values, year, maxValue)
            drawLastCircle(canvas, values, year, maxValue, index + 1 == years.size)
        }
    }

    private fun createYearValues(year: Int): Array<Float?> {
        val values = arrayOfNulls<Float>(12)
        dataSet
            .filter { chartData -> chartData.year == year }
            .map { chartData -> values[chartData.month - 1] = chartData.value }
        return values
    }

    private fun drawPath(canvas: Canvas, values: Array<Float?>, year: Year, maxValue: Float) {
        drawingDots.clear()

        drawingDots.add(Dot(0f, (bgHeight - lineBottomMargin).toFloat(), year.value, 0f))
        for (index in 0..11) {
            val value = values[index] ?: continue
            val ratio = if (maxValue == 0f) 0f else value / maxValue
            val x = ((index + 1) * lineBetweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin - lineBottomMargin)
            drawingDots.add(Dot(x, y, year.value, value))
        }

        entireDrawingDots.addAll(drawingDots)

        val x = drawingDots.map { it.x }.toFloatArray()
        val y = drawingDots.map { it.y }.toFloatArray()
        linePaint.color = year.color
        canvas.drawPath(MonotoneCubicSpline.computeControlPoints(x, y), linePaint)
    }

    private fun drawBottomYear(canvas: Canvas, index: Int, year: Year) {
        // draw BottomYearCircle + BottomYearText
        val maxSize = getMaxBottomYearSize()
        val position = index % maxSize
        val lines = floor(index.toFloat() / maxSize).toInt()
        val bottomYearWidth = measureWidth(measuredWidth) / maxSize

        val circleX =
            (bottomYearWidth * position + bottomYearXMargin).toFloat()
        val circleY = bgHeight + bottomYearYMargin + lines * bottomYearTextHeight

        bottomYearCirclePaint.color = if (year.isActive) year.color else inactiveCircleColor
        canvas.drawCircle(circleX, circleY, bottomYearCircleSize, bottomYearCirclePaint)

        bottomYearTextPaint.color = if (year.isActive) textColor else inactiveTextColor
        canvas.drawText(
            year.value.toString(),
            circleX + bottomYearCircleSize * 2,
            circleY + bottomYearTextYMargin,
            bottomYearTextPaint
        )

        year.region = Region(
            (bottomYearWidth * position),
            (circleY - bottomYearCircleSize * 2).toInt(),
            (bottomYearWidth * (position + 1)),
            (circleY + bottomYearCircleSize * 2).toInt()
        )
    }

    private fun getMaxBottomYearSize(): Int {
        val textWidth =
            bottomYearXMargin + bottomYearCircleSize * 2 + bottomYearTextWidthWithPadding
        val size = floor(measureWidth(measuredWidth) / textWidth).toInt()
        return min(size, bottomYearMaxSize)
    }

    private fun drawCircle(canvas: Canvas, values: Array<Float?>, year: Year, maxValue: Float) {
        for ((index, value) in values.withIndex()) {
            if (value == null) continue

            // draw circle if value is changed compare to previous month
            val isSameValue =
                (index == 0 && value == 0f) || (index >= 1 && values[index - 1] == value)
            if (isSameValue) continue

            val ratio = if (maxValue == 0f) 0f else value / maxValue
            val x = ((index + 1) * lineBetweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin - lineBottomMargin)
            canvas.drawCircle(x, y, lineCircleOuterSize, bgPaint)
            lineCircleOuterPaint.color = year.color
            canvas.drawCircle(x, y, lineCircleOuterSize, lineCircleOuterPaint)
            canvas.drawCircle(x, y, lineCircleInnerSize, lineCircleInnerPaint)
        }
    }

    private fun drawLastCircle(
        canvas: Canvas,
        values: Array<Float?>,
        year: Year,
        maxValue: Float,
        isLastYear: Boolean
    ) {
        val lastMonth = getLastMonthPair(values) ?: return
        val lastMontRatio = if (maxValue == 0f) 0f else lastMonth.second / maxValue
        val lastMonthX = ((lastMonth.first + 1) * lineBetweenX).toFloat()
        val lastMonthY =
            lineTopMargin + (1 - lastMontRatio) * (bgHeight - lineTopMargin - lineBottomMargin)
        canvas.drawCircle(lastMonthX, lastMonthY, lineLastCircleOuterSize, bgPaint)
        lineCircleOuterPaint.color = year.color
        canvas.drawCircle(lastMonthX, lastMonthY, lineLastCircleOuterSize, lineCircleOuterPaint)
        canvas.drawCircle(lastMonthX, lastMonthY, lineLastCircleInnerSize, lineCircleInnerPaint)

        if (!isLastYear) return
        val valueText = valueFormat.format(lastMonth.second)
        lineTextPaint.color = year.color
        canvas.drawText(valueText, lastMonthX, lastMonthY - lineTextMarginY, lineTextOutlinePaint)
        canvas.drawText(valueText, lastMonthX, lastMonthY - lineTextMarginY, lineTextPaint)
    }

    private fun drawSelectedDotIfNeeded(canvas: Canvas) {
        if (selectedDots.isEmpty()) return

        // draw divider
        val first = selectedDots.first()
        canvas.drawLine(first.x, 0f, first.x, bgHeight.toFloat(), selectedBorderPaint)

        for (dot in selectedDots) {
            canvas.drawCircle(dot.x, dot.y, selectedCircleOuterSize, selectedCircleOuterPaint)
            canvas.drawCircle(dot.x, dot.y, selectedCircleInnerSize, selectedCircleInnerPaint)
        }

        val popupTextMaxWidth =
            selectedDots.map { measurePopupTextWidth(valueFormat.format(it.value)) }.maxBy { it }!!
        val popupYearWidth = measurePopupYearTextWidth(first.year.toString())
        val popupUnitWidth = measurePopupUnitTextWidth(unitText)
        val popupWidth = if (unitText.isNotEmpty()) {
            popupPadding * 2 + popupYearWidth + popupYearMargin + popupTextMaxWidth +
                    popupUnitMargin + popupUnitWidth + popupPadding
        } else {
            popupPadding * 2 + popupYearWidth + popupYearMargin + popupTextMaxWidth + popupPadding
        }
        val popupTextLineHeight = popupTextSize * 1.1f
        val popupHeight = (popupTextLineHeight * selectedDots.size + popupPadding * 2)
        var popupRight = first.x + popupWidth / 2
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
        path.moveTo(first.x - triangleHalfWidth, popupHeight) // top left
        path.lineTo(first.x + triangleHalfWidth, popupHeight) // top right
        path.lineTo(first.x, popupHeight + popupTriangleHeight) // bottom center
        path.lineTo(first.x - triangleHalfWidth, popupHeight) // top left
        path.close()
        canvas.drawPath(path, popupBgPaint)

        for ((index, dot) in selectedDots.withIndex()) {
            val baseline = (popupTextLineHeight * (index + 1) + popupPadding / 2f)

            canvas.drawText(
                dot.year.toString(),
                popupLeft + popupPadding * 2,
                baseline - popupUnitTextSize / 4, // tweak center
                popupYearTextPaint
            )

            canvas.drawText(
                valueFormat.format(dot.value),
                popupRight - popupPadding - popupUnitMargin - popupUnitWidth,
                baseline,
                popupTextPaint
            )

            if (unitText.isNotEmpty()) {
                canvas.drawText(
                    unitText,
                    popupRight - popupPadding,
                    baseline,
                    popupUnitTextPaint
                )
            }
        }
    }

    private fun getLastMonthPair(values: Array<Float?>): Pair<Int, Float>? {
        for (index in values.size - 1 downTo 0) {
            val value = values[index] ?: continue
            return Pair(index, value)
        }

        return null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight())
    }

    private fun measureWidth(widthMeasureSpec: Int): Int {
        return max(bgMinimumWidth, widthMeasureSpec)
    }

    private fun measureHeight(): Int {
        val yearHeight =
            (ceil(years.size.toFloat() / getMaxBottomYearSize())) * bottomYearTextHeight
        return (bgHeight + bottomMonthTextHeight + yearHeight).toInt()
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

                        findSelectedDots(it.x.toInt(), it.y.toInt())
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
                    findSelectedDots(event.x.toInt(), event.y.toInt())
                    postInvalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                actionDownEvent = null
                timerTask?.cancel()
                disallowIntercept = false
                parent?.requestDisallowInterceptTouchEvent(false)

                selectedDots.clear()
                findSelectedYears(event.x.toInt(), event.y.toInt())
                postInvalidate()
            }
        }

        return true
    }

    private fun findSelectedDots(x: Int, y: Int) {
        selectedDots.clear()
        val r = Region()
        val range = lineBetweenX / 2f
        // reverse `entireDrawingDots` to show 2020 -> 2010 in popup
        for (dot in entireDrawingDots.reversed()) {
            if (dot.x == 0f) continue

            r.set((dot.x - range).toInt(), 0, (dot.x + range).toInt(), bgHeight)

            // `selectedDots` is guaranteed same x
            if (r.contains(x, y) && (selectedDots.size == 0 || selectedDots.first().x == dot.x)) {
                selectedDots.add(dot)
            }
        }
    }

    private fun findSelectedYears(x: Int, y: Int) {
        for (year in years) {
            if (year.region.contains(x, y)) {
                year.isActive = !year.isActive
            }
        }
    }

    private fun measurePopupTextWidth(text: String): Float {
        return popupTextPaint.measureText(text)
    }

    private fun measurePopupUnitTextWidth(text: String): Float {
        return popupUnitTextPaint.measureText(text)
    }

    private fun measurePopupYearTextWidth(text: String): Float {
        return popupYearTextPaint.measureText(text)
    }
}