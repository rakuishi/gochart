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

    private val bgHeight: Int = dp2px(context, 280f)
    private val bottomTextHeight: Int = dp2px(context, 17f)
    private val bgRadius: Float = dp2px(context, 8f).toFloat()
    private val lineTopMargin: Int = dp2px(context, 27f) // bgTopPadding 12 + barTextHeight 15
    private val lineCircleOuterSize: Float = dp2px(context, 10f).toFloat()
    private val lineCircleInnerSize: Float = dp2px(context, 5f).toFloat()
    private val lineTextMarginY: Int = dp2px(context, 15f)
    private val bottomMonthTextYMargin: Int = dp2px(context, 17f)

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
        drawLine(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0, 0, measuredWidth, bgHeight)
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)
    }

    private fun drawLine(canvas: Canvas) {
        val maxValue = calcMaxLineChartDataValue()
        val lineBetweenX: Int = measuredWidth / (dataSet.size + 1)
        canvas.drawPath(createPath(dataSet, maxValue, lineBetweenX), linePaint)

        for ((index, data) in dataSet.withIndex()) {
            val ratio = data.value / maxValue
            val x = ((index + 1) * lineBetweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin)

            // draw BottomMonthText
            canvas.drawText(
                data.year.toString(),
                x,
                (bgHeight + bottomMonthTextYMargin).toFloat(),
                bottomMonthTextPaint
            )

            // draw Circle
            canvas.drawCircle(x, y, lineCircleOuterSize, lineCircleOuterPaint)
            canvas.drawCircle(x, y, lineCircleInnerSize, lineCircleInnerPaint)
            canvas.drawText(
                valueFormat.format(data.value),
                x,
                y - lineTextMarginY,
                lineTextOutlinePaint
            )
            canvas.drawText(
                valueFormat.format(data.value),
                x,
                y - lineTextMarginY,
                lineTextPaint
            )
        }
    }

    private fun createPath(
        dataSet: ArrayList<ChartData>,
        maxValue: Float,
        lineBetweenX: Int
    ): Path {
        val path = Path()

        for ((index, data) in dataSet.withIndex()) {
            val ratio = data.value / maxValue
            val x = ((index + 1) * lineBetweenX).toFloat()
            val y = lineTopMargin + (1 - ratio) * (bgHeight - lineTopMargin)

            // draw Path
            if (path.isEmpty) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        return path
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