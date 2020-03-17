package com.rakuishi.gochart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.rakuishi.gochart.Util.Companion.dp2px
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class AnnualLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val year: Int, val month: Int, val value: Float)
    class Dot(val x: Float, val y: Float, val year: Int, val value: Float)

    private val bgHeight: Int = dp2px(context, 280f)
    private val bottomMonthTextHeight: Int = dp2px(context, 23f)
    private val bottomYearTextHeight: Float = dp2px(context, 17f).toFloat()
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
    private val textColor: Int = Color.parseColor("#3B3B3B")
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
    private val alphas = arrayListOf(255, 150, 96)

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
            field = value
            requestLayout()
        }
    var unitText: String = ""

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
            textAlign = Paint.Align.LEFT
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
        val years = createYearDataSet()
        entireDrawingDots.clear()

        for ((index, year) in years.withIndex()) {
            val values = createYearValues(year)
            val alphaIndex = (years.size - 1) - index
            val alpha = if (alphaIndex >= alphas.size) 48 else alphas[alphaIndex]
            linePaint.alpha = alpha
            lineCircleOuterPaint.alpha = alpha
            bottomYearCirclePaint.alpha = alpha

            drawPath(canvas, values, year, maxValue)
            drawBottomYear(canvas, index, year)
            drawCircle(canvas, values, maxValue)
            drawLastCircle(canvas, values, maxValue, index + 1 == years.size)
        }
    }

    private fun createYearDataSet(): List<Int> {
        return dataSet.map { chartData -> chartData.year }.distinct()
    }

    private fun createYearValues(year: Int): Array<Float?> {
        val values = arrayOfNulls<Float>(12)
        dataSet
            .filter { chartData -> chartData.year == year }
            .map { chartData -> values[chartData.month - 1] = chartData.value }
        return values
    }

    private fun drawPath(canvas: Canvas, values: Array<Float?>, year: Int, maxValue: Float) {
        drawingDots.clear()

        drawingDots.add(Dot(0f, (bgHeight - lineBottomMargin).toFloat(), year, 0f))
        var dot: Dot? = null
        for (index in 0..11) {
            val value = values[index] ?: continue
            val ratio = value / maxValue
            val x = ((index + 1) * lineBetweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin - lineBottomMargin)
            dot = Dot(x, y, year, value)
            // Remove same value to draw a smooth line
            if (index >= 1 && values[index] == values[index - 1]) continue
            drawingDots.add(dot)
        }

        // Add the last one certainly. If the last value is same as previous one, a path will be broken.
        if (dot != null && !drawingDots.contains(dot)) {
            drawingDots.add(dot)
        }

        entireDrawingDots.addAll(drawingDots)

        val x = drawingDots.map { it.x }.toFloatArray()
        val y = drawingDots.map { it.y }.toFloatArray()
        canvas.drawPath(MonotoneCubicSpline.computeControlPoints(x, y), linePaint)
    }

    private fun drawBottomYear(canvas: Canvas, index: Int, year: Int) {
        // draw BottomYearCircle + BottomYearText
        val maxSize = getMaxBottomYearSize()
        val position = index % maxSize
        val lines = floor(index.toFloat() / maxSize).toInt()

        val circleX =
            (bottomYearXMargin * (position + 1)) + (bottomYearCircleSize * 2 + bottomYearTextWidthWithPadding) * position
        val circleY = bgHeight + bottomYearYMargin + lines * bottomYearTextHeight

        canvas.drawCircle(circleX, circleY, bottomYearCircleSize, bottomYearCirclePaint)
        canvas.drawText(
            year.toString(),
            circleX + bottomYearCircleSize * 2,
            circleY + bottomYearTextYMargin,
            bottomYearTextPaint
        )
    }

    private fun getMaxBottomYearSize(): Int {
        val textWidth =
            bottomYearXMargin + bottomYearCircleSize * 2 + bottomYearTextWidthWithPadding
        return floor(measureWidth(measuredWidth) / textWidth).toInt()
    }

    private fun drawCircle(canvas: Canvas, values: Array<Float?>, maxValue: Float) {
        for ((index, value) in values.withIndex()) {
            if (value == null) continue

            // draw circle if value is changed compare to previous month
            val isSameValue =
                (index == 0 && value == 0f) || (index >= 1 && values[index - 1] == value)
            if (isSameValue) continue

            val ratio = value / maxValue
            val x = ((index + 1) * lineBetweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin - lineBottomMargin)
            canvas.drawCircle(x, y, lineCircleOuterSize, bgPaint)
            canvas.drawCircle(x, y, lineCircleOuterSize, lineCircleOuterPaint)
            canvas.drawCircle(x, y, lineCircleInnerSize, lineCircleInnerPaint)
        }
    }

    private fun drawLastCircle(
        canvas: Canvas,
        values: Array<Float?>,
        maxValue: Float,
        isLastYear: Boolean
    ) {
        val lastMonth = getLastMonthPair(values) ?: return
        val lastMontRatio = lastMonth.second / maxValue
        val lastMonthX = ((lastMonth.first + 1) * lineBetweenX).toFloat()
        val lastMonthY =
            lineTopMargin + (1 - lastMontRatio) * (bgHeight - lineTopMargin - lineBottomMargin)
        canvas.drawCircle(lastMonthX, lastMonthY, lineLastCircleOuterSize, bgPaint)
        canvas.drawCircle(lastMonthX, lastMonthY, lineLastCircleOuterSize, lineCircleOuterPaint)
        canvas.drawCircle(lastMonthX, lastMonthY, lineLastCircleInnerSize, lineCircleInnerPaint)

        if (!isLastYear) return
        val valueText = valueFormat.format(lastMonth.second)
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
        val popupWidth = if (unitText.isNotEmpty()) {
            popupYearWidth + popupTextMaxWidth + measurePopupUnitTextWidth(unitText) +
                    popupPadding * 3 + popupYearMargin + popupUnitMargin
        } else {
            popupYearWidth + popupTextMaxWidth +
                    popupPadding * 3 + popupYearMargin
        }
        val popupTextLineHeight = popupTextSize * 1.1f
        val popupHeight = (popupTextLineHeight * selectedDots.size + popupPadding * 2)
        val popupLeft = first.x - popupWidth / 2
        val popupRight = first.x + popupWidth / 2

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
                popupLeft + popupPadding * 2 + popupYearWidth + popupYearMargin,
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
            (ceil(createYearDataSet().size.toFloat() / getMaxBottomYearSize())) * bottomYearTextHeight
        return (bgHeight + bottomMonthTextHeight + yearHeight).toInt()
    }

    private fun calcMaxLineChartDataValue(): Float {
        if (dataSet.isEmpty()) return 0f

        var maxValue = 0f
        for (data in dataSet) {
            maxValue = max(maxValue, data.value)
        }
        return maxValue
    }

    private var downY: Float? = null
    private val draggableYRange = dp2px(context, 20f)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN)
            downY = event.y

        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            // TODO: Don't call `postInvalidate()` frequently
            findSelectedDots(event.x.toInt(), event.y.toInt())
            postInvalidate()
        }

        val disallow = event.action == MotionEvent.ACTION_MOVE
                && (downY != null && downY!! - draggableYRange < event.y && event.y < downY!! + draggableYRange)
        parent?.requestDisallowInterceptTouchEvent(disallow)
        return true
    }

    private fun findSelectedDots(x: Int, y: Int) {
        selectedDots.clear()
        val r = Region()
        val range = lineBetweenX / 2f
        for (dot in entireDrawingDots) {
            if (dot.x == 0f) continue

            r.set((dot.x - range).toInt(), 0, (dot.x + range).toInt(), bgHeight)

            // `selectedDots` is guaranteed same x
            if (r.contains(x, y) && (selectedDots.size == 0 || selectedDots.first().x == dot.x)) {
                selectedDots.add(dot)
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