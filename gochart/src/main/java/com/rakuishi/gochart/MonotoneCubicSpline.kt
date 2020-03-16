package com.rakuishi.gochart

import android.graphics.Path
import java.lang.Float.MAX_VALUE
import kotlin.math.abs

class MonotoneCubicSpline {

    companion object {

        // https://github.com/chartist-js/chartist/blob/master/src/interpolation/monotone-cubic.js
        fun computeControlPoints(xs: FloatArray, ys: FloatArray): Path {
            if (xs.size != ys.size) {
                throw IllegalStateException("xs.size and ys.size must be same.")
            }
            if (xs.isEmpty()) {
                throw IllegalStateException("xs.size is empty.")
            }

            val path = Path()
            path.moveTo(xs[0], ys[0])
            if (xs.size == 2) {
                path.lineTo(xs[1], ys[1])
                return path
            }

            val n = xs.size
            val ms = FloatArray(n)
            val ds = FloatArray(n)
            val dys = FloatArray(n)
            val dxs = FloatArray(n)

            // Calculate deltas and derivative
            for (i in 0..(n - 2)) {
                dys[i] = ys[i + 1] - ys[i]
                dxs[i] = xs[i + 1] - xs[i]
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
                val x1: Float = xs[i] + dxs[i] / 3
                val y1: Float = ys[i] + ms[i] * dxs[i] / 3
                val x2: Float = xs[i + 1] - dxs[i] / 3
                val y2: Float = ys[i + 1] - ms[i + 1] * dxs[i] / 3
                path.cubicTo(x1, y1, x2, y2, xs[i + 1], ys[i + 1])
            }

            return path
        }
    }
}