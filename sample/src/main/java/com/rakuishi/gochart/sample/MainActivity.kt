package com.rakuishi.gochart.sample

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.rakuishi.gochart.AnnualLineChartView
import com.rakuishi.gochart.CumulativeLineChartView
import com.rakuishi.gochart.HorizontalBarChartView
import com.rakuishi.gochart.VerticalBarChartView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        renderHorizontalBarChartView()
        renderVerticalBarChartView()
        renderAnnualLineChartView()
        renderCumulativeLineChartView()
    }

    private fun renderHorizontalBarChartView() {
        val dataSet: ArrayList<HorizontalBarChartView.ChartData> = arrayListOf()
        for (year in 2018..2019) {
            for (month in 1..12) {
                dataSet.add(HorizontalBarChartView.ChartData(year, month, month.toFloat()))
            }
        }
        horizontalBarChartView.isEmptyPastData = true
        horizontalBarChartView.dataSet = dataSet
        horizontalBarChartView.bgMinimumWidth = getDeviceSize(this).x - dp2px(this, 32f)
        horizontalBarChartView.barColor = Color.parseColor("#2898EE")
        horizontalBarChartView.invalidate(horizontalScrollView)
    }

    private fun renderVerticalBarChartView() {
        val dataSet: ArrayList<VerticalBarChartView.ChartData> = arrayListOf()
        dataSet.add(VerticalBarChartView.ChartData("富士山", 34f))
        dataSet.add(VerticalBarChartView.ChartData("北岳", 28f))
        dataSet.add(VerticalBarChartView.ChartData("槍ヶ岳", 21f))
        dataSet.add(VerticalBarChartView.ChartData("木曽駒ヶ岳", 17f))
        dataSet.add(VerticalBarChartView.ChartData("剱岳", 11f))
        dataSet.add(VerticalBarChartView.ChartData("阿蘇山", 8f))
        dataSet.add(VerticalBarChartView.ChartData("マカルー", 5f))
        dataSet.add(VerticalBarChartView.ChartData("カンチェンジュンガ", 3f))
        verticalBarChartView.dataSet = dataSet
        verticalBarChartView.valueFormat = "%.0f"
        verticalBarChartView.barColor = Color.parseColor("#EF3E65")
        verticalBarChartView.invalidate()
    }

    private fun renderAnnualLineChartView() {
        val dataSet: ArrayList<AnnualLineChartView.ChartData> = arrayListOf()
        for (year in 2018..2020) {
            var sum = 0
            for (month in 1..12) {
                sum += (1..10).random()
                dataSet.add(AnnualLineChartView.ChartData(year, month, sum.toFloat()))
            }
        }
        annualLineChartView.dataSet = dataSet
        annualLineChartView.bgMinimumWidth = getDeviceSize(this).x - dp2px(this, 32f)
        annualLineChartView.lineColor = Color.parseColor("#F0A037")
        annualLineChartView.invalidate()
    }

    private fun renderCumulativeLineChartView() {
        val dataSet: ArrayList<CumulativeLineChartView.ChartData> = arrayListOf()
        var sum = 0f
        for (year in 2015..2020) {
            sum += (1..10).random()
            dataSet.add(CumulativeLineChartView.ChartData(year, sum))
        }
        cumulativeLineChartView.dataSet = dataSet
        cumulativeLineChartView.bgMinimumWidth = getDeviceSize(this).x - dp2px(this, 32f)
        cumulativeLineChartView.lineColor = Color.parseColor("#45BA64")
        cumulativeLineChartView.invalidate()
    }

    private fun getDeviceSize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }

    private fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
