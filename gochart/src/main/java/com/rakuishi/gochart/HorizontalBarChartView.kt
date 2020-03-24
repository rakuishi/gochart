package com.rakuishi.gochart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import com.rakuishi.gochart.Util.Companion.dp2px
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class HorizontalBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    class ChartData(val year: Int, val month: Int, val value: Float?)
    class Bar(val rectF: RectF, val value: Float)

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
    private val popupTriangleWidth: Int = dp2px(context, 7f)
    private val popupTriangleHeight: Int = dp2px(context, 5f)
    private val popupPadding: Int = dp2px(context, 4f)
    private val popupUnitMargin: Int = dp2px(context, 2f)
    private val popupTextSize: Int = dp2px(context, 11f)
    private val popupUnitTextSize: Int = dp2px(context, 7f)
    private val popupRadius: Float = dp2px(context, 2f).toFloat()

    private val rect: Rect = Rect()
    private val bgColor: Int = Color.parseColor("#ECECEC")
    private val borderColor: Int = Color.parseColor("#D2D2D2")
    private val emptyBgColor: Int = Color.parseColor("#D2D2D2")
    private var textColor: Int = Color.parseColor("#3B3B3B")
    private val popupBgColor: Int = Color.parseColor("#000000")
    private val popupTextColor: Int = Color.parseColor("#FFFFFF")
    private val bgPaint: Paint = Paint()
    private val borderPaint: Paint = Paint()
    private val emptyBgPaint: Paint = Paint()
    private val emptyTextPaint: Paint = Paint()
    private val barPaint: Paint = Paint()
    private val barTextPaint: Paint = Paint()
    private val bottomMonthTextPaint: Paint = Paint()
    private val bottomYearTextPaint: Paint = Paint()
    private val popupBgPaint: Paint = Paint()
    private val popupTextPaint: Paint = Paint()
    private val popupUnitTextPaint: Paint = Paint()

    // Save bar's RectF for handling touch events
    private val drawingBars = mutableListOf<Bar>()
    private var selectedBar: Bar? = null
    private val touchableRange: Int = dp2px(context, 6f)

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
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timerTask?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        drawBackground(canvas)
        drawEmptyBackgroundIfNeeded(canvas)
        drawBar(canvas)
        drawSelectedBarIfNeeded(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        rect.set(0, 0, measureWidth(), bgHeight)
        canvas.drawRoundRect(RectF(rect), bgRadius, bgRadius, bgPaint)
    }

    private fun drawEmptyBackgroundIfNeeded(canvas: Canvas) {
        if (!isEmptyPastData) return

        val emptyWidth =
            if (isEmptyPastData) ((((12 + 0.5) * barSideMargin) + (12 * barWidth))).toInt()
            else 0

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

    private fun drawBar(canvas: Canvas) {
        drawingBars.clear()
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
                    val dividerX = left - barWidth / 2f + borderWidth
                    canvas.drawLine(dividerX, 0f, dividerX, bgHeight.toFloat(), borderPaint)
                }

                currentYear = data.year
            }

            if (data.value == null) continue

            // draw Bar
            val top = (barTopMargin + (1 - data.value / maxValue) * (bgHeight - barTopMargin))
            val right = left + barWidth
            rect.set(left, top.toInt(), right, bgHeight)

            val rectF = RectF(rect)
            drawingBars.add(Bar(rectF, data.value))
            canvas.drawRoundRect(rectF, barRadius, barRadius, barPaint)

            // draw BarText
            canvas.drawText(
                valueFormat.format(data.value),
                centerX,
                (top - barTextYMargin),
                barTextPaint
            )
        }
    }

    private fun drawSelectedBarIfNeeded(canvas: Canvas) {
        selectedBar?.let {
            val popupTextWidth = measurePopupTextWidth(valueFormat.format(it.value))
            val popupWidth = if (unitText.isNotEmpty()) {
                popupTextWidth + popupUnitMargin + measurePopupUnitTextWidth(unitText) + popupPadding * 2
            } else {
                popupTextWidth + popupPadding * 2
            }
            val popupHeight = popupTextSize + popupPadding * 2
            val centerX = it.rectF.left + (it.rectF.right - it.rectF.left) / 2
            val popupBottom = it.rectF.top - popupTriangleHeight
            val popupTop = popupBottom - popupHeight
            var popupRight = centerX + popupWidth / 2
            if (popupRight > measuredWidth) {
                popupRight = measuredWidth.toFloat()
            }
            var popupLeft = popupRight - popupWidth
            if (popupLeft < 0) {
                popupLeft = 0f
                popupRight = popupWidth
            }

            val rectF = RectF(popupLeft, popupTop, popupRight, popupBottom)
            canvas.drawRoundRect(rectF, popupRadius, popupRadius, popupBgPaint)

            // draw bottom triangle
            val path = Path()
            val triangleHalfWidth = popupTriangleWidth / 2f
            path.moveTo(centerX - triangleHalfWidth, popupBottom) // top left
            path.lineTo(centerX + triangleHalfWidth, popupBottom) // top right
            path.lineTo(centerX, it.rectF.top) // bottom center
            path.lineTo(centerX - triangleHalfWidth, popupBottom) // top left
            path.close()
            canvas.drawPath(path, popupBgPaint)

            canvas.drawText(
                valueFormat.format(it.value),
                popupLeft + popupPadding,
                popupBottom - popupTextSize / 2, // tweaked baseline
                popupTextPaint
            )

            if (unitText.isNotEmpty()) {
                canvas.drawText(
                    unitText,
                    popupRight - popupPadding,
                    popupBottom - popupTextSize / 2, // tweaked baseline
                    popupUnitTextPaint
                )
            }
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

    fun scrollToRight() {
        if (parent is HorizontalScrollView) {
            (parent as HorizontalScrollView).post {
                (parent as HorizontalScrollView).fullScroll(
                    HorizontalScrollView.FOCUS_RIGHT
                )
            }
        }
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

                        findSelectedBar(it.x.toInt(), it.y.toInt())
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
                    findSelectedBar(event.x.toInt(), event.y.toInt())
                    postInvalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                actionDownEvent = null
                timerTask?.cancel()
                disallowIntercept = false
                parent?.requestDisallowInterceptTouchEvent(false)

                selectedBar = null
                postInvalidate()
            }
        }

        return true
    }

    private fun findSelectedBar(x: Int, y: Int) {
        selectedBar = null
        val r = Region()
        for (bar in drawingBars) {
            r.set(
                bar.rectF.left.toInt() - touchableRange,
                0,
                bar.rectF.right.toInt() + touchableRange,
                bgHeight
            )
            if (r.contains(x, y)) {
                selectedBar = bar
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