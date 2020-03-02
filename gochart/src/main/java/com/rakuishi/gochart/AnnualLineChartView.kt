package com.rakuishi.gochart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.rakuishi.gochart.Util.Companion.dp2px
import kotlin.math.max

class AnnualLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val year: Int, val month: Int, val value: Float)

    private val bgHeight: Int = dp2px(context, 280f)
    private val bottomTextHeight: Int = dp2px(context, 38f)
    private val bgRadius: Float = dp2px(context, 8f).toFloat()
    private val lineTopMargin: Int = dp2px(context, 27f) // bgTopPadding 12 + barTextHeight 15
    private val lineBetweenX: Int by lazy { measuredWidth / 13 }
    private val lineCircleOuterSize: Float = dp2px(context, 10f).toFloat()
    private val lineCircleInnerSize: Float = dp2px(context, 5f).toFloat()
    private val lineTextMarginY: Int = dp2px(context, 15f)
    private val bottomMonthTextYMargin: Int = dp2px(context, 17f)
    private val bottomYearXMargin: Int = dp2px(context, 13f)
    private val bottomYearYMargin: Int = dp2px(context, 33f) // between with bg
    private val bottomYearTextYMargin: Int = dp2px(context, 4f)
    private val bottomYearCircleSize: Float = dp2px(context, 5f).toFloat()

    private val rect: Rect = Rect()
    private val bgColor: Int = Color.parseColor("#F3F3F3")
    private val textColor: Int = Color.parseColor("#3B3B3B")
    private val circleInnerColor: Int = Color.parseColor("#FFFFFF")
    private val bgPaint: Paint = Paint()
    private val linePaint: Paint = Paint()
    private val lineTextPaint: Paint = Paint()
    private val lineTextOutlinePaint: Paint = Paint()
    private val lineCircleOuterPaint: Paint = Paint()
    private val lineCircleInnerPaint: Paint = Paint()
    private val bottomMonthTextPaint: Paint = Paint()
    private val bottomYearTextPaint: Paint = Paint()
    private val bottomYearCirclePaint: Paint = Paint()
    private val alphas = arrayListOf(255, 150, 96)

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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawLine(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0, 0, measuredWidth, bgHeight)
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)
    }

    private fun drawLine(canvas: Canvas) {
        val maxValue = calcMaxLineChartDataValue()
        val years = dataSet.map { chartData -> chartData.year }.distinct()

        for ((index, year) in years.withIndex()) {
            val values = createYearValues(year)
            val alphaIndex = (years.size - 1) - index
            val alpha = if (alphaIndex >= alphas.size) 48 else alphas[alphaIndex]

            linePaint.alpha = alpha
            canvas.drawPath(createPath(values, maxValue), linePaint)

            drawBottomMonthText(canvas, values)

            // draw BottomYearCircle + BottomYearText
            bottomYearCirclePaint.alpha = alpha
            val circleX =
                (bottomYearXMargin * (index + 1)) + (bottomYearCircleSize * 2 + bottomYearTextWidthWithPadding) * index
            canvas.drawCircle(
                circleX,
                (bgHeight + bottomYearYMargin).toFloat(),
                bottomYearCircleSize,
                bottomYearCirclePaint
            )
            canvas.drawText(
                year.toString(),
                circleX + bottomYearCircleSize * 2,
                (bgHeight + bottomYearYMargin + bottomYearTextYMargin).toFloat(),
                bottomYearTextPaint
            )

            // draw last month
            val lastMonth = getLastMonthPair(values) ?: continue
            val ratio = lastMonth.second / maxValue
            val lastMonthX = ((lastMonth.first + 1) * lineBetweenX).toFloat()
            val lastMonthY = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin)
            lineCircleOuterPaint.alpha = alpha
            canvas.drawCircle(lastMonthX, lastMonthY, lineCircleOuterSize, lineCircleOuterPaint)
            canvas.drawCircle(lastMonthX, lastMonthY, lineCircleInnerSize, lineCircleInnerPaint)

            if (index + 1 == years.size) {
                canvas.drawText(
                    valueFormat.format(lastMonth.second),
                    lastMonthX,
                    lastMonthY - lineTextMarginY,
                    lineTextOutlinePaint
                )
                canvas.drawText(
                    valueFormat.format(lastMonth.second),
                    lastMonthX,
                    lastMonthY - lineTextMarginY,
                    lineTextPaint
                )
            }
        }
    }

    private fun drawBottomMonthText(canvas: Canvas, values: Array<Float?>) {
        for ((index, _) in values.withIndex()) {
            canvas.drawText(
                (index + 1).toString(),
                ((index + 1) * lineBetweenX).toFloat(),
                (bgHeight + bottomMonthTextYMargin).toFloat(),
                bottomMonthTextPaint
            )
        }
    }

    private fun createYearValues(year: Int): Array<Float?> {
        val values = arrayOfNulls<Float>(12)
        dataSet
            .filter { chartData -> chartData.year == year }
            .map { chartData -> values[chartData.month - 1] = chartData.value }
        return values
    }

    private fun createPath(values: Array<Float?>, maxValue: Float): Path {
        val path = Path()

        for (index in 0..11) {
            if (values[index] == null) continue

            val ratio = (values[index] ?: 0f).div(maxValue)
            val x = ((index + 1) * lineBetweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin)

            if (path.isEmpty) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        return path
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
        return bgHeight + bottomTextHeight
    }

    private fun calcMaxLineChartDataValue(): Float {
        if (dataSet.isEmpty()) return 0f

        var maxValue = 0f
        for (data in dataSet) {
            maxValue = max(maxValue, data.value)
        }
        return maxValue
    }
}