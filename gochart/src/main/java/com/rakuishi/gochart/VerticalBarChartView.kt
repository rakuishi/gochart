package com.rakuishi.gochart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.rakuishi.gochart.Util.Companion.dp2px
import kotlin.math.max

class VerticalBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val name: String, val value: Float)

    private val bgRadius: Float = dp2px(context, 8f).toFloat()
    private val barRadius: Float = dp2px(context, 2f).toFloat()
    private val barHeight: Int = dp2px(context, 17f)
    private val barVerticalMargin: Int = dp2px(context, 23f)
    private val barSideMargin: Int = dp2px(context, 14f)
    private val barLeft: Int = dp2px(context, 97f)
    private val barEndMargin: Int = dp2px(context, 34f)
    private val barTextXMargin: Int = dp2px(context, 4f)
    private val barTextYMargin: Int = dp2px(context, 5f)
    private val leftTextXMargin: Int = dp2px(context, 11f)
    private val leftTextYMargin: Int = dp2px(context, 3f)
    private val leftTextMaxLength: Int by lazy {
        val width = barLeft - leftTextXMargin * 2
        val fullWidthTextWidth = leftTextPaint.measureText("あ")
        (width / fullWidthTextWidth).toInt()
    }

    private val rect: Rect = Rect()
    private val bgColor: Int = Color.parseColor("#F5F6F7")
    private val textColor: Int = Color.parseColor("#1F1F1F")
    private val bgPaint: Paint = Paint()
    private val barPaint: Paint = Paint()
    private val barTextPaint: Paint = Paint()
    private val leftTextPaint: Paint = Paint()

    var valueFormat: String = "%.1f"
    var barColor: Int = Color.parseColor("#2898EE")
        set(value) {
            field = value
            barPaint.color = value
            barTextPaint.color = value
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

        barPaint.run {
            isAntiAlias = true
            color = barColor
        }

        barTextPaint.run {
            isAntiAlias = true
            color = barColor
            textSize = dp2px(getContext(), 12f).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        leftTextPaint.run {
            isAntiAlias = true
            color = textColor
            textSize = dp2px(getContext(), 13f).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawBar(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0, 0, measuredWidth, measureHeight())
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)
    }

    private fun drawBar(canvas: Canvas) {
        val maxValue = calcMaxBarChartDataValue()
        for ((index, data) in dataSet.withIndex()) {
            val ratio = data.value / maxValue

            val barMaxWidth = measuredWidth - barLeft - barEndMargin
            val barRight = barLeft + (barMaxWidth * ratio).toInt()
            val barTop = barVerticalMargin + index * (barSideMargin + barHeight)
            val barBottom = barVerticalMargin + index * (barSideMargin + barHeight) + barHeight
            rect.set(barLeft, barTop, barRight, barBottom)

            // draw Bar
            canvas.drawRoundRect(RectF(rect), barRadius, barRadius, barPaint)

            // draw BarText
            canvas.drawText(
                valueFormat.format(data.value),
                (barRight + barTextXMargin).toFloat(),
                (barBottom - barTextYMargin).toFloat(),
                barTextPaint
            )

            // draw BarLeftText
            val leftText =
                if (data.name.length > leftTextMaxLength) data.name.substring(
                    0,
                    leftTextMaxLength - 1
                ) + "..."
                else data.name

            canvas.drawText(
                leftText,
                (barLeft - leftTextXMargin).toFloat(),
                (barBottom - leftTextYMargin).toFloat(),
                leftTextPaint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(widthMeasureSpec, measureHeight())
    }

    private fun measureHeight(): Int {
        val totalBarVerticalMargin = barVerticalMargin * 2
        val totalBarSideMargin = barSideMargin * (dataSet.size - 1)
        val totalBarHeight = barHeight * dataSet.size
        return totalBarVerticalMargin + totalBarSideMargin + totalBarHeight
    }

    private fun calcMaxBarChartDataValue(): Float {
        if (dataSet.isEmpty()) return 0f

        var maxValue = 0f
        for (data in dataSet) {
            maxValue = max(maxValue, data.value)
        }
        return maxValue
    }
}
