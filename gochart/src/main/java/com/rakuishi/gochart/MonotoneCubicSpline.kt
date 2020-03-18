package com.rakuishi.gochart

import android.graphics.Path
import java.lang.Float.MAX_VALUE
import kotlin.math.abs

class MonotoneCubicSpline {

    companion object {

        // https://github.com/chartist-js/chartist/blob/master/src/interpolation/monotone-cubic.js
        fun computeControlPoints(xs: FloatArray, ys: FloatArray): Path {
            if (xs.size != xs.size) {
                throw IllegalStateException("xs.size and ys.size must be same.")
            }
            if (xs.isEmpty()) {
                throw IllegalStateException("xs.size is empty.")
            }

            val hasDummyXY: Boolean
            val x: FloatArray
            val y: FloatArray
            if (xs.size == 2) {
                // cubic needs 3 points to draw. If xs.size is 2, add dummyXY
                hasDummyXY = true
                x = FloatArray(3)
                y = FloatArray(3)
                x[0] = xs[0]
                x[1] = xs[1]
                x[2] = xs[1] * 2
                y[0] = ys[0]
                y[1] = ys[1]
                y[2] = ys[1] * 2
            } else {
                hasDummyXY = false
                x = xs
                y = ys
            }

            val path = Path()
            path.moveTo(x[0], y[0])
            if (x.size == 2) {
                path.lineTo(x[1], y[1])
                return path
            }

            val n = x.size
            val ms = FloatArray(n)
            val ds = FloatArray(n)
            val dys = FloatArray(n)
            val dxs = FloatArray(n)

            // Calculate deltas and derivative
            for (i in 0..(n - 2)) {
                dys[i] = y[i + 1] - y[i]
                dxs[i] = x[i + 1] - x[i]
                ds[i] = dys[i] / dxs[i]
            }

            // Determine desired slope (m) at each point using Fritsch-Carlson method
            // See: http://math.stackexchange.com/questions/45218/implementation-of-monotone-cubic-interpolation
            ms[0] = ds[0]
            ms[n - 1] = ds[n - 2]

            for (i in 1..(n - 2)) {
                if (ds[i] == 0f || ds[i - 1] == 0f || (ds[i - 1] > 0) != (ds[i] > 0)) {
                    ms[i] = 0f
                } else {
                    ms[i] = 3 * (dxs[i - 1] + dxs[i]) / (
                            (2 * dxs[i] + dxs[i - 1]) / ds[i - 1] +
                                    (dxs[i] + 2 * dxs[i - 1]) / ds[i])

                    val isFinite = abs(ms[i]) <= MAX_VALUE
                    if (!isFinite) {
                        ms[i] = 0f
                    }
                }
            }

            // Now build a path from the slopes
            for (i in 0..(n - 2)) {
                val x1: Float = x[i] + dxs[i] / 3
                val y1: Float = y[i] + ms[i] * dxs[i] / 3
                val x2: Float = x[i + 1] - dxs[i] / 3
                val y2: Float = y[i + 1] - ms[i + 1] * dxs[i] / 3
                path.cubicTo(x1, y1, x2, y2, x[i + 1], y[i + 1])
                if (hasDummyXY) break
            }

            return path
        }
    }
}