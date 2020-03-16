package com.rakuishi.gochart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView
import com.rakuishi.gochart.Util.Companion.dp2px
import kotlin.math.ceil
import kotlin.math.max

class HorizontalBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val year: Int, val month: Int, val value: Float?)

    private val bgHeight: Int = dp2px(context, 280f)
    private val bottomTextHeight: Int = dp2px(context, 30f)
    private val bgRadius: Float = dp2px(context, 8f).toFloat()
    private val borderWidth: Float = dp2px(context, 1f).toFloat()
    private val emptyTextSize: Float = dp2px(getContext(), 13f).toFloat()
    private val emptyTextLineHeight: Float = dp2px(getContext(), 23f).toFloat()
    private val barRadius: Float = dp2px(context, 2f).toFloat()
    private val barWidth: Int = dp2px(context, 14f)
    private val barSideMargin: Int = dp2px(context, 12f)
    private val barTopMargin: Int = dp2px(context, 40f) // bgTopPadding 25 + barTextHeight 15
    private val barTextYMargin: Int = dp2px(context, 6f)
    private val bottomMonthTextYMargin: Int = dp2px(context, 15f)
    private val bottomYearTextYMargin: Int = dp2px(context, 30f)

    private val rect: Rect = Rect()
    private val bgColor: Int = Color.parseColor("#ECECEC")
    private val borderColor: Int = Color.parseColor("#DBDBDB")
    private val emptyBgColor: Int = Color.parseColor("#D2D2D2")
    private val textColor: Int = Color.parseColor("#3B3B3B")
    private val bgPaint: Paint = Paint()
    private val borderPaint: Paint = Paint()
    private val emptyBgPaint: Paint = Paint()
    private val emptyTextPaint: Paint = Paint()
    private val barPaint: Paint = Paint()
    private val barTextPaint: Paint = Paint()
    private val bottomMonthTextPaint: Paint = Paint()
    private val bottomYearTextPaint: Paint = Paint()

    var bgMinimumWidth: Int = 0
    var valueFormat: String = "%.1f"
    var barColor: Int = Color.parseColor("#2898EE")
        set(value) {
            field = value
            barPaint.color = value
            barTextPaint.color = value
        }
    var dataSet: ArrayList<ChartData> = arrayListOf()
        set(value) {
            if (isEmptyPastData && value.isNotEmpty()) {
                val first = value.first()
                for (i in 1..12) {
                    val chartData = if (first.month - i <= 0) {
                        ChartData(first.year - 1, first.month - i + 12, null)
                    } else {
                        ChartData(first.year, first.month - i, null)
                    }
                    value.add(0, chartData)
                }
            }
            field = value
            requestLayout()
        }
    var isEmptyPastData: Boolean = false
    var emptyPastDataText: String = "No chart data available."

    init {
        bgPaint.run {
            isAntiAlias = true
            color = bgColor
        }

        borderPaint.run {
            isAntiAlias = true
            strokeWidth = borderWidth
            color = borderColor
        }

        emptyBgPaint.run {
            isAntiAlias = true
            color = emptyBgColor
        }

        emptyTextPaint.run {
            isAntiAlias = true
            color = textColor
            textSize = emptyTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        barPaint.run {
            isAntiAlias = true
            color = barColor
        }

        barTextPaint.run {
            isAntiAlias = true
            color = barColor
            textSize = dp2px(getContext(), 11f).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
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
            textSize = dp2px(getContext(), 10f).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawBackground(canvas)
        drawBar(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val emptyWidth =
            if (isEmptyPastData) ((((12 + 0.5) * barSideMargin) + (12 * barWidth))).toInt()
            else 0

        rect.set(0, 0, measureWidth(), bgHeight)
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)

        if (isEmptyPastData) {
            rect.set(0, 0, emptyWidth, bgHeight)
            canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, emptyBgPaint)

            // remove right-top, right-bottom radius
            rect.set((emptyWidth - bgRadius).toInt(), 0, emptyWidth, bgHeight)
            canvas.drawRect(rect, emptyBgPaint)

            // draw empty text
            val strings = emptyPastDataText.split("\n")
            val count = ceil(strings.size / 2f) - 1
            val baseY = if (strings.size % 2 == 0) {
                bgHeight / 2 - count * emptyTextLineHeight - emptyTextLineHeight / 2 // odd
            } else {
                bgHeight / 2 - count * emptyTextLineHeight + emptyTextLineHeight / 2 // even
            }

            for ((index, text) in strings.withIndex()) {
                canvas.drawText(
                    text,
                    (emptyWidth / 2).toFloat(),
                    (baseY + emptyTextLineHeight * index),
                    emptyTextPaint
                )
            }
        }
    }

    private fun drawBar(canvas: Canvas) {
        val maxValue = calcMaxBarChartDataValue()
        var currentYear: Int? = null
        for ((index, data) in dataSet.withIndex()) {
            val left = ((index + 1) * barSideMargin) + (index * barWidth)
            val centerX = (left + barWidth / 2).toFloat()

            // draw BottomMonthText
            canvas.drawText(
                data.month.toString(),
                centerX,
                (bgHeight + bottomMonthTextYMargin).toFloat(),
                bottomMonthTextPaint
            )

            // draw BottomYearText
            if (currentYear == null || currentYear != data.year) {
                canvas.drawText(
                    data.year.toString(),
                    centerX,
                    (bgHeight + bottomYearTextYMargin).toFloat(),
                    bottomYearTextPaint
                )

                // draw divider
                if (currentYear != null) {
                    val dividerX = left - barWidth / 2f
                    canvas.drawLine(dividerX, 0f, dividerX, bgHeight.toFloat(), borderPaint)
                }

                currentYear = data.year
            }

            if (data.value == null) continue

            // draw Bar
            val top = (barTopMargin + (1 - data.value / maxValue) * (bgHeight - barTopMargin))
            rect.set(left, top.toInt(), left + barWidth, bgHeight)
            canvas.drawRoundRect(RectF(rect), barRadius, barRadius, barPaint)

            // draw BarText
            canvas.drawText(
                valueFormat.format(data.value),
                centerX,
                (top - barTextYMargin),
                barTextPaint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(), measureHeight())
    }

    private fun measureWidth(): Int {
        val totalBarWidth = barWidth * dataSet.size
        val totalBarSideMargin = barSideMargin * (dataSet.size + 1)
        return max(bgMinimumWidth, totalBarWidth + totalBarSideMargin)
    }

    private fun measureHeight(): Int {
        return bgHeight + bottomTextHeight
    }

    private fun calcMaxBarChartDataValue(): Float {
        if (dataSet.isEmpty()) return 0f

        var maxValue = 0f
        for (data in dataSet) {
            if (data.value == null) continue
            maxValue = max(maxValue, data.value)
        }
        return maxValue
    }

    fun invalidate(horizontalScrollView: HorizontalScrollView) {
        invalidate()
        horizontalScrollView.post { horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }
    }
}