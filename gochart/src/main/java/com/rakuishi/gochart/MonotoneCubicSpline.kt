package com.rakuishi.gochart

class MonotoneCubicSpline {

    companion object {

        // https://www.particleincell.com/wp-content/uploads/2012/06/bezier-spline.js
        @Suppress("ReplaceRangeToWithUntil")
        fun computeControlPoints(K: FloatArray): Pair<FloatArray, FloatArray> {
            val n = K.size - 1
            val p1 = FloatArray(n)
            val p2 = FloatArray(n)

            // rhs vector
            val a = FloatArray(n)
            val b = FloatArray(n)
            val c = FloatArray(n)
            val r = FloatArray(n)

            // left most segment
            a[0] = 0f
            b[0] = 2f
            c[0] = 1f
            r[0] = K[0] + 2 * K[1]

            // internal segments
            for (i in 1..(n - 2)) {
                a[i] = 1f
                b[i] = 4f
                c[i] = 1f
                r[i] = 4 * K[i] + 2 * K[i + 1]
            }

            // right segment
            a[n - 1] = 2f
            b[n - 1] = 7f
            c[n - 1] = 0f
            r[n - 1] = 8 * K[n - 1] + K[n]

            // solves Ax=b with the Thomas algorithm (from Wikipedia)
            for (i in 1..(n - 1)) {
                val m = a[i] / b[i - 1]
                b[i] = b[i] - m * c[i - 1]
                r[i] = r[i] - m * r[i - 1]
            }

            p1[n - 1] = r[n - 1] / b[n - 1]
            for (i in (n - 2) downTo 0) {
                p1[i] = (r[i] - c[i] * p1[i + 1]) / b[i]
            }

            // we have p1, now compute p2
            for (i in 0..(n - 2)) {
                p2[i] = 2 * K[i + 1] - p1[i + 1]
            }

            p2[n - 1] = 0.5f * (K[n] + p1[n - 1])

            return Pair(p1, p2)
        }
    }
}