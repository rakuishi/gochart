package com.rakuishi.gochart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.rakuishi.gochart.Util.Companion.dp2px
import kotlin.math.max

class CumulativeLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val year: Int, val value: Float)
    class Dot(val x: Float, val y: Float, val value: Float)

    private val bgHeight: Int = dp2px(context, 280f)
    private val bottomTextHeight: Int = dp2px(context, 17f)
    private val bgRadius: Float = dp2px(context, 8f).toFloat()
    private val lineTopMargin: Int = dp2px(context, 40f) // bgTopPadding 25 + barTextHeight 15
    private val lineBottomMargin: Int = dp2px(context, 15f)
    private val lineCircleOuterSize: Float = dp2px(context, 10f).toFloat()
    private val lineCircleInnerSize: Float = dp2px(context, 5f).toFloat()
    private val lineTextMarginY: Int = dp2px(context, 15f)
    private val bottomMonthTextYMargin: Int = dp2px(context, 17f)

    private val rect: Rect = Rect()
    private val bgColor: Int = Color.parseColor("#F5F6F7")
    private val textColor: Int = Color.parseColor("#3B3B3B")
    private val circleInnerColor: Int = Color.parseColor("#FFFFFF")
    private val bgPaint: Paint = Paint()
    private val linePaint: Paint = Paint()
    private val lineTextPaint: Paint = Paint()
    private val lineTextOutlinePaint: Paint = Paint()
    private val lineCircleOuterPaint: Paint = Paint()
    private val lineCircleInnerPaint: Paint = Paint()
    private val bottomMonthTextPaint: Paint = Paint()

    // to draw Bezier Curve
    private val drawingDots = mutableListOf<Dot>()

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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawDataSet(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0, 0, measuredWidth, bgHeight)
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)
    }

    private fun drawDataSet(canvas: Canvas) {
        if (dataSet.size == 0) return
        val maxValue = calcMaxLineChartDataValue()
        val betweenX: Int = measuredWidth / (dataSet.size + 1)
        drawPath(canvas, maxValue, betweenX)

        for ((index, data) in dataSet.withIndex()) {
            val ratio = data.value / maxValue
            val centerX = ((index + 1) * betweenX).toFloat()
            val centerY =
                lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin - lineBottomMargin)
            val bottomY = (bgHeight + bottomMonthTextYMargin).toFloat()

            // draw BottomMonthText
            canvas.drawText(data.year.toString(), centerX, bottomY, bottomMonthTextPaint)

            // draw Circle
            val valueText = valueFormat.format(data.value)
            canvas.drawCircle(centerX, centerY, lineCircleOuterSize, lineCircleOuterPaint)
            canvas.drawCircle(centerX, centerY, lineCircleInnerSize, lineCircleInnerPaint)
            canvas.drawText(valueText, centerX, centerY - lineTextMarginY, lineTextOutlinePaint)
            canvas.drawText(valueText, centerX, centerY - lineTextMarginY, lineTextPaint)
        }
    }

    private fun drawPath(canvas: Canvas, maxValue: Float, betweenX: Int) {
        drawingDots.clear()

        for (index in 0 until dataSet.size) {
            val value = dataSet[index].value
            val ratio = value / maxValue
            val x = ((index + 1) * betweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin - lineBottomMargin)
            drawingDots.add(Dot(x, y, value))
        }

        val path = Path()
        path.moveTo(drawingDots[0].x, drawingDots[0].y)

        val n = drawingDots.size - 1
        if (n == 1) {
            path.lineTo(drawingDots[1].x, drawingDots[1].y)
        } else if (n > 1) {
            val x = drawingDots.map { dot -> dot.x }.toFloatArray()
            val y = drawingDots.map { dot -> dot.y }.toFloatArray()
            val (x1, x2) = MonotoneCubicSpline.computeControlPoints(x)
            val (y1, y2) = MonotoneCubicSpline.computeControlPoints(y)
            for (i in 0 until n) {
                path.cubicTo(x1[i], y1[i], x2[i], y2[i], x[i + 1], y[i + 1])
            }
        }

        canvas.drawPath(path, linePaint)
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